package com.consultantapp.ui.dashboard.doctor.confirm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.requests.SecondOpinion
import com.consultantapp.data.models.responses.UserData
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentConfirmBookingBinding
import com.consultantapp.ui.dashboard.DoctorViewModel
import com.consultantapp.ui.dashboard.doctor.detail.DoctorDetailActivity
import com.consultantapp.ui.dashboard.doctor.schedule.ScheduleFragment
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.ui.drawermenu.DrawerActivity.Companion.WALLET
import com.consultantapp.utils.*
import com.consultantapp.utils.DateUtils.dateFormatFromMillis
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ConfirmBookingFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentConfirmBookingBinding

    private var rootView: View? = null

    private lateinit var viewModel: DoctorViewModel

    private lateinit var progressDialog: ProgressDialog

    private var doctorData: UserData? = null

    private var scheduleType = ""

    private var couponStatus = 0

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                    inflater,
                    R.layout.fragment_confirm_booking, container, false
            )
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
            hitApi(true)

        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        binding.clLoader.setBackgroundResource(R.color.colorWhite)
        doctorData = requireActivity().intent.getSerializableExtra(USER_DATA) as UserData
        scheduleType = requireActivity().intent.getStringExtra(PAGE_TO_OPEN) ?: ""

        if (scheduleType == RequestType.INSTANT) {
            binding.tvEditAppointment.gone()
            binding.tvConfirm.text = getString(R.string.confirm_booking)
        }

        if (requireActivity().intent.hasExtra(DoctorDetailActivity.EXTRA_SECOND_OPINION)) {
            binding.tvConfirm.text = getString(R.string.get_second_opinion)
        }

        binding.tvName.text = getDoctorName(doctorData)
        binding.tvDesc.text = doctorData?.categoryData?.name
                ?: binding.root.context.getString(R.string.na)
        loadImage(binding.ivPic, doctorData?.profile_image,
                R.drawable.ic_profile_placeholder)

        if (doctorData?.phone != null)
            binding.etPhone.setText(
                    getString(
                            R.string.phone_s_s,
                            doctorData?.country_code,
                            doctorData?.phone
                    )
            )

        if (doctorData?.email != null)
            binding.etEmail.setText(doctorData?.email)

        val date =
                dateFormatFromMillis(DateFormat.DAY_DATE_FORMAT, arguments?.getLong(DATE_SELECTED))
        binding.etAppointment.setText(
                getString(
                        R.string.date_time,
                        date, arguments?.getString(TIME_SELECTED)
                )
        )
    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvEditAppointment.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.tvConfirm.setOnClickListener {
            hitApi(false)
            binding.tvConfirm.hideKeyboard()
        }

        binding.tvApply.setOnClickListener {
            binding.tvApply.hideKeyboard()
            if (binding.etCoupon.text.toString().length < 5) {
                binding.etCoupon.showSnackBar(getString(R.string.add_valid_coupon))
            } else {
                couponStatus = 1
                hitApi(true)
            }
        }
    }

    private fun hitApi(confirm: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, Any>()

            /*Category id auto pic backend*/
            hashMap["consultant_id"] = doctorData?.id ?: ""
            hashMap["service_id"] =
                    requireActivity().intent.getStringExtra(ScheduleFragment.SERVICE_ID)
                            ?: ""

            hashMap["schedule_type"] = scheduleType

            if (requireActivity().intent.hasExtra(EXTRA_REQUEST_ID))
                hashMap["request_id"] = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID)
                        ?: ""

            if (scheduleType == RequestType.SCHEDULE) {
                val date =
                        dateFormatFromMillis(DateFormat.DATE_FORMAT, arguments?.getLong(DATE_SELECTED))
                hashMap["date"] = date
                hashMap["time"] = DateUtils.dateFormatChange(
                        DateFormat.TIME_FORMAT,
                        DateFormat.TIME_FORMAT_24, arguments?.getString(TIME_SELECTED) ?: ""
                )
            }

            if (requireActivity().intent.hasExtra(DoctorDetailActivity.EXTRA_SECOND_OPINION)) {
                val secondOpinion =
                        requireActivity().intent.getSerializableExtra(DoctorDetailActivity.EXTRA_SECOND_OPINION) as SecondOpinion
                if (secondOpinion.title != null) {
                    hashMap["title"] = secondOpinion.title ?: ""

                    var images = ""
                    secondOpinion.image?.forEach {
                        images += "$it,"
                    }

                    hashMap["images"] = images.removeSuffix(",")
                }
                hashMap["second_oponion"] = true
            }

            if (confirm) {
                if (couponStatus == 1)
                    hashMap["coupon_code"] = binding.etCoupon.text.toString()
                viewModel.confirmRequest(hashMap)
            } else {
                if (couponStatus == 2)
                    hashMap["coupon_code"] = binding.etCoupon.text.toString()
                viewModel.createRequest(hashMap)
            }
        }
    }


    private fun bindObservers() {
        viewModel.createRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    /*If amount not sufficient then add money*/
                    if (it.data?.amountNotSufficient == true) {
                        AlertDialog.Builder(requireContext())
                                .setCancelable(false)
                                .setTitle(getString(R.string.added_to_wallet))
                                .setMessage(getString(R.string.money_insufficient))
                                .setPositiveButton(getString(R.string.ok)) { dialog, which ->

                                }
                                .setNegativeButton(getString(R.string.add_money)) { dialog, which ->
                                    startActivity(
                                            Intent(requireContext(), DrawerActivity::class.java)
                                                    .putExtra(PAGE_TO_OPEN, WALLET)
                                    )
                                }.show()

                    } else {
                        /* requireActivity().longToast(getString(R.string.request_sent))
                         requireActivity().setResult(Activity.RESULT_OK)
                         requireActivity().finish()*/

                        requireActivity().setResult(Activity.RESULT_OK)
                        val fragment = SuccessFragment(this)
                        val bundle = Bundle()
                        bundle.putString(EXTRA_REQUEST_ID, it.data?.request?.id)
                        fragment.arguments = bundle
                        fragment.show(requireActivity().supportFragmentManager, fragment.tag)
                    }

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

        viewModel.confirmRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.tvConfirm.visible()
                    binding.clLoader.setBackgroundResource(0)

                    val data = it.data

                    if (couponStatus == 1)
                        couponStatus = 2

                    binding.tvSubTotalV.text = getCurrency(data?.total)
                    binding.tvPromoAppliedV.text = getCurrency(data?.discount)
                    binding.tvTotalV.text = getCurrency(data?.grand_total)

                    if (scheduleType == RequestType.INSTANT) {
                        val date = DateUtils.dateFormatChange(
                                DateFormat.DATE_FORMAT, DateFormat.DAY_DATE_FORMAT, data?.book_slot_date
                                ?: "")
                        binding.etAppointment.setText(
                                getString(
                                        R.string.date_time,
                                        date, data?.book_slot_time
                                ))
                    }

                    if (data?.is_paid == false) {
                        binding.tvFreeBookingNote.visible()
                        startTimer((data.left_minute ?: 0) * 60 * 1000)
                    } else
                        binding.tvFreeBookingNote.gone()
                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    couponStatus = 1
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.visible()

                    if (binding.etCoupon.text.toString().isEmpty())
                        binding.tvConfirm.gone()
                }
            }
        })
    }

    private fun startTimer(currentTimer: Long) {
        val TOTAL_TIME = 60000L

        val totalTimerToRun = currentTimer
        countDownTimer = object : CountDownTimer(currentTimer, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val newTimer = millisUntilFinished / 1000
                binding.tvFreeBookingNote.text =
                        getString(R.string.free_booking_time, convertMiliSecondsToMinute(newTimer))
            }

            override fun onFinish() {}
        }
        countDownTimer?.start()
    }

    private fun convertMiliSecondsToMinute(seconds: Long): String {
        val days = seconds / (3600 * 24)
        return when {
            days > 1 -> getString(R.string.s_days, days.toString())
            days > 0 -> getString(R.string.s_day, days.toString())
            else -> {
                val s = seconds % 60
                val m = seconds / 60 % 60
                val h = seconds / 3600
                String.format("%s : %02d", h.toString(), m)
            }
        }
    }

    companion object {
        const val DATE_SELECTED = "DATE_SELECTED"
        const val TIME_SELECTED = "TIME_SELECTED"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
