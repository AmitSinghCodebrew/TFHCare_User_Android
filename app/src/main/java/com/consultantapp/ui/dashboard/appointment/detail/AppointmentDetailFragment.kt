package com.consultantapp.ui.dashboard.appointment.detail

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.consultantapp.R
import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.data.models.responses.Filter
import com.consultantapp.data.models.responses.Request
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PushType
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.FragmentAppointmentDetailsBinding
import com.consultantapp.ui.adapter.CheckItemAdapter
import com.consultantapp.ui.dashboard.DoctorViewModel
import com.consultantapp.ui.dashboard.appointment.AppointmentViewModel
import com.consultantapp.ui.dashboard.doctor.DoctorActionActivity
import com.consultantapp.ui.dashboard.doctor.schedule.ScheduleFragment
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class AppointmentDetailFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentAppointmentDetailsBinding

    private var rootView: View? = null

    private lateinit var viewModel: AppointmentViewModel

    private lateinit var viewModelDoctor: DoctorViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var request: Request

    private var isReceiverRegistered = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_appointment_details,
                container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
            hitApi()
        }
        return rootView
    }


    private fun initialise() {
        progressDialog = ProgressDialog(requireActivity())
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        viewModelDoctor = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        binding.clLoader.setBackgroundResource(R.color.colorWhite)
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["request_id"] = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
            viewModel.requestDetail(hashMap)
        }
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvReSchedule.setOnClickListener {
            rescheduleAppointment()
        }

        binding.tvCancel.setOnClickListener {
            cancelAppointment()
        }

        binding.tvRate.setOnClickListener {
            if (request.rating == null) {
                startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.RATE)
                        .putExtra(USER_DATA, request.to_user)
                        .putExtra(EXTRA_REQUEST_ID, request.id), AppRequestCode.APPOINTMENT_DETAILS)
            }
        }

        binding.tvViewPrescription.setOnClickListener {
            val link = getString(R.string.pdf_link, request.id, APP_UNIQUE_ID)
            openPdf(requireActivity(), link,true)
        }

        binding.tvNotifyAvailable.setOnClickListener {
            notifyAvailable()
        }
    }

    private fun rescheduleAppointment() {
        if (request.schedule_type == RequestType.INSTANT) {
            confirmRequest()
        } else {
            val intent = Intent(requireContext(), DoctorActionActivity::class.java)
                .putExtra(PAGE_TO_OPEN, request.schedule_type)
                .putExtra(ScheduleFragment.SERVICE_ID, request.service_id)
                .putExtra(USER_DATA, request.to_user)

            if (request.status != CallAction.COMPLETED) {
                intent.putExtra(EXTRA_REQUEST_ID, request.id)
            }
            startActivityForResult(intent, AppRequestCode.NEW_APPOINTMENT)
        }
    }

    private fun confirmRequest() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, Any>()

            /*Category id auto pic backend*/
            hashMap["consultant_id"] = request.to_user?.id ?: ""
            hashMap["service_id"] = request.service_id ?: ""
            hashMap["schedule_type"] = request.schedule_type ?: ""

            if (request.status != CallAction.COMPLETED)
                hashMap["request_id"] = request.id ?: ""

            viewModelDoctor.confirmRequest(hashMap)
        }
    }

    private fun notifyAvailable() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(),
            R.string.notify,
            R.string.notify_desc,
            R.string.notify,
            R.string.cancel,
            false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    if (isConnectedToInternet(requireContext(), true)) {
                        val hashMap = HashMap<String, String>()
                        hashMap["request_id"] = request.id ?: ""
                        viewModel.notifyUser(hashMap)
                    }
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun cancelAppointment() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(),
            R.string.cancel_appointment,
            R.string.cancel_appointment_msg,
            R.string.cancel_appointment,
            R.string.cancel,
            false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    if (isConnectedToInternet(requireContext(), true)) {
                        val hashMap = HashMap<String, String>()
                        hashMap["request_id"] = request.id ?: ""
                        viewModel.cancelRequest(hashMap)
                    }
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun bindObservers() {
        viewModel.requestDetail.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.clLoader.setBackgroundResource(0)

                    request = it.data?.request_detail ?: Request()
                    setData()

                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.visible()
                }
            }
        })

        viewModel.cancelRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi()

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

        viewModel.notifyAvailable.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    requireActivity().longToast(getString(R.string.notify_message))

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

        viewModelDoctor.confirmRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    val intent = Intent(requireContext(), DoctorActionActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, request.schedule_type)
                        .putExtra(ScheduleFragment.SERVICE_ID, request.service_id)
                        .putExtra(USER_DATA, request.to_user)

                    if (request.status != CallAction.COMPLETED) {
                        intent.putExtra(EXTRA_REQUEST_ID, request.id)
                    }
                    startActivityForResult(intent, AppRequestCode.NEW_APPOINTMENT)
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })
    }

    private fun setData() {
        binding.tvCancel.hideShowView(request.canCancel)
        binding.tvReSchedule.hideShowView(request.canReschedule)
        binding.tvReSchedule.text = getString(R.string.re_schedule)
        binding.tvRate.gone()
        binding.tvViewPrescription.gone()
        binding.tvNotifyAvailable.gone()

        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.colorPrimary))

        binding.tvName.text = request.to_user?.name
        binding.tvPrice.text = getCurrency(request.price)
        binding.tvServiceTypeV.text = request.service_type
        loadImage(binding.ivPic, request.to_user?.profile_image,
            R.drawable.ic_profile_placeholder)

        binding.tvBookingDateV.text =
            DateUtils.dateTimeFormatFromUTC(DateFormat.MON_YEAR_FORMAT, request.bookingDateUTC)
        binding.tvBookingTimeV.text =
            DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.bookingDateUTC)

        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.colorPrimary))

        when (request.status) {
            CallAction.PENDING -> {
                binding.tvStatus.text = getString(R.string.new_request)
            }
            CallAction.ACCEPT -> {
                binding.tvStatus.text = getString(R.string.accepted)
                binding.tvReSchedule.gone()
                binding.tvCancel.gone()

                binding.tvNotifyAvailable.hideShowView(request.canNotify)
            }
            CallAction.INPROGRESS, CallAction.BUSY -> {
                binding.tvStatus.text = getString(R.string.inprogess)
                binding.tvReSchedule.gone()
                binding.tvCancel.gone()

                binding.tvNotifyAvailable.hideShowView(request.canNotify)
            }
            CallAction.COMPLETED -> {
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.textColorGreen))
                binding.tvStatus.text = getString(R.string.completed)
                binding.tvReSchedule.text = getString(R.string.book_again)
                binding.tvReSchedule.visible()
                binding.tvCancel.gone()

                binding.tvRate.hideShowView(request.rating.isNullOrEmpty())
                binding.tvViewPrescription.hideShowView(request.is_prescription == true)
            }
            CallAction.FAILED -> {
                binding.tvStatus.text = getString(R.string.no_show)
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.colorCancel))
            }
            CallAction.CANCELED -> {
                binding.tvStatus.text = getString(R.string.canceled)
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.colorCancel))
                binding.tvReSchedule.gone()
                binding.tvCancel.gone()
            }
            else -> {
                binding.tvStatus.text = getString(R.string.new_request)
            }
        }


        /*Second Opinion*/
        binding.tvMedicalRecord.text = request.second_oponion?.title ?: ""
        binding.tvMedicalRecord.hideShowView(binding.tvMedicalRecord.text.isNotEmpty())

        val array = request.second_oponion?.images?.split(",") ?: emptyList()
        val adapter = ImagesUploadedAdapter(this, ArrayList(array))
        binding.rvMedicalRecord.adapter = adapter

        binding.tvMedicalRecordT.hideShowView(
            binding.tvMedicalRecord.text.isNotEmpty() && ArrayList(
                array
            ).isNotEmpty()
        )

        /*Symptom*/
        binding.tvSymptomDec.text = request.symptom_details
        binding.tvSymptomDec.hideShowView(binding.tvSymptomDec.text.isNotEmpty())

        binding.rvSymptomListing.layoutManager = GridLayoutManager(requireContext(), 3)

        val symptomImages = ArrayList<DocImage>()
        symptomImages.addAll(request.symptom_images ?: emptyList())
        val adapterSymptomImage = ImagesDocumentAdapter(this, symptomImages)
        binding.rvSymptomDoc.adapter = adapterSymptomImage
        binding.rvSymptomDoc.hideShowView(symptomImages.isNotEmpty())

        val items = ArrayList<Filter>()
        items.addAll(request.symptoms ?: emptyList())
        val adapterSymptom = CheckItemAdapter(this, true, items)
        binding.rvSymptomListing.adapter = adapterSymptom

        binding.tvSymptom.hideShowView(binding.tvSymptomDec.text.isNotEmpty() || items.isNotEmpty())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppRequestCode.APPOINTMENT_DETAILS) {
                requireActivity().setResult(Activity.RESULT_OK)
                hitApi()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            intentFilter.addAction(PushType.REQUEST_ACCEPTED)
            intentFilter.addAction(PushType.CANCELED_REQUEST)
            intentFilter.addAction(PushType.REQUEST_FAILED)
            intentFilter.addAction(PushType.CHAT_STARTED)
            intentFilter.addAction(PushType.UPCOMING_APPOINTMENT)
            intentFilter.addAction(PushType.PRESCRIPTION_ADDED)
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(refreshRequests, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshRequests)
            isReceiverRegistered = false
        }
    }

    private val refreshRequests = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.REQUEST_COMPLETED, PushType.COMPLETED, PushType.REQUEST_ACCEPTED,
                PushType.CANCELED_REQUEST, PushType.REQUEST_FAILED, PushType.CHAT_STARTED,
                PushType.UPCOMING_APPOINTMENT,PushType.PRESCRIPTION_ADDED-> {
                    if (request.id == intent.getStringExtra(EXTRA_REQUEST_ID))
                        hitApi()
                }
            }
        }
    }
}