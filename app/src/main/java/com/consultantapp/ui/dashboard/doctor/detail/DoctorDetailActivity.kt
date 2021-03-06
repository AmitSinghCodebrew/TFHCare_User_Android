package com.consultantapp.ui.dashboard.doctor.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.requests.SecondOpinion
import com.consultantapp.data.models.responses.Review
import com.consultantapp.data.models.responses.Service
import com.consultantapp.data.models.responses.UserData
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PER_PAGE_LOAD
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.ActivityDoctorDetailBinding
import com.consultantapp.ui.dashboard.DoctorViewModel
import com.consultantapp.ui.dashboard.doctor.DoctorActionActivity
import com.consultantapp.ui.dashboard.doctor.listing.DoctorListActivity.Companion.AK_SECOND_OPINION
import com.consultantapp.ui.dashboard.doctor.schedule.ScheduleFragment.Companion.SERVICE_ID
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.ui.drawermenu.DrawerActivity.Companion.WALLET
import com.consultantapp.ui.loginSignUp.welcome.WelcomeFragment
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class DoctorDetailActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var binding: ActivityDoctorDetailBinding

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: DoctorViewModel

    private lateinit var adapter: RatingAdapter

    private var items = ArrayList<Review>()

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private var doctorId = ""

    private var doctorData: UserData? = null

    private var serviceSelected: Service? = null

    private var secondOpinion: SecondOpinion? = null

    private var scheduleType = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_doctor_detail)

        initialise()
        listeners()
        setAdapter()
        bindObservers()
        hiApiDoctorDetail()
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        progressDialog = ProgressDialog(this)

        doctorId = intent.getStringExtra(DOCTOR_ID) ?: ""

        binding.clLoader.setBackgroundColor(ContextCompat.getColor(this, R.color.colorWhite))
    }

    private fun hiApiDoctorDetail() {
        if (isConnectedToInternet(this, true)) {
            val hashMap = HashMap<String, String>()
            hashMap["doctor_id"] = doctorId
            viewModel.doctorDetails(hashMap)

            viewModel.reviewList(hashMap)

        }
    }


    fun hiApiDoctorRequest(schedule: Boolean) {
        scheduleType = schedule

        when {
            secondOpinion == null && intent.hasExtra(PAGE_TO_OPEN) -> {
                startActivityForResult(Intent(this, DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, AK_SECOND_OPINION), AppRequestCode.SECOND_OPINION)
            }
            scheduleType -> {
                val intent = Intent(this, DoctorActionActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, RequestType.SCHEDULE)
                        .putExtra(SERVICE_ID, serviceSelected?.service_id)
                        .putExtra(USER_DATA, doctorData)

                if (secondOpinion != null)
                    intent.putExtra(EXTRA_SECOND_OPINION, secondOpinion)

                startActivityForResult(intent, AppRequestCode.NEW_APPOINTMENT)

            }
            isConnectedToInternet(this, true) -> {
                val hashMap = HashMap<String, Any>()

                hashMap["consultant_id"] = doctorId
                hashMap["service_id"] = serviceSelected?.service_id ?: ""
                hashMap["schedule_type"] = RequestType.INSTANT

                viewModel.confirmRequest(hashMap)
            }

        }
    }

    private fun bindObservers() {
        viewModel.doctorDetails.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.ivMark.visible()

                    doctorData = it.data?.dcotor_detail
                    setDoctorData()

                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    binding.ivMark.gone()
                    binding.clLoader.visible()
                }
            }
        })

        viewModel.reviewList.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    isLoadingMoreItems = false

                    val tempList = it.data?.review_list ?: emptyList()
                    if (isFirstPage) {
                        isFirstPage = false
                        items.clear()
                    }

                    items.addAll(tempList)
                    adapter.notifyDataSetChanged()

                    isLastPage = tempList.size < PER_PAGE_LOAD
                    adapter.setAllItemsLoaded(isLastPage)

                    binding.tvNoData.hideShowView(items.isEmpty())
                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {

                }
            }
        })

        viewModel.confirmRequest.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    /*If amount not sufficient then add money*/
                    if (it.data?.amountNotSufficient == true) {
                        AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setTitle(getString(R.string.added_to_wallet))
                                .setMessage(getString(R.string.money_insufficient))
                                .setPositiveButton(getString(R.string.ok)) { dialog, which ->

                                }
                                .setNegativeButton(getString(R.string.add_money)) { dialog, which ->
                                    startActivity(
                                            Intent(this, DrawerActivity::class.java)
                                                    .putExtra(PAGE_TO_OPEN, WALLET)
                                    )
                                }.show()

                    } else {
                        val intent = Intent(this, DoctorActionActivity::class.java)
                                .putExtra(PAGE_TO_OPEN, RequestType.INSTANT)
                                .putExtra(SERVICE_ID, serviceSelected?.service_id)
                                .putExtra(USER_DATA, doctorData)

                        if (secondOpinion != null)
                            intent.putExtra(EXTRA_SECOND_OPINION, secondOpinion)

                        startActivityForResult(intent, AppRequestCode.NEW_APPOINTMENT)
                    }

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

    }

    private fun setDoctorData() {
        binding.tvName.text = getDoctorName(doctorData)
        loadImage(
                binding.ivPic, doctorData?.profile_image,
                R.drawable.image_placeholder
        )
        binding.tvAboutV.text = doctorData?.profile?.bio ?: getString(R.string.na)

        binding.tvDesc.text = doctorData?.categoryData?.name ?: getString(R.string.na)

        binding.tvRating.text = getString(
                R.string.s_s_reviews,
                getUserRating(doctorData?.totalRating),
                doctorData?.reviewCount
        )

        if (doctorData?.consultationCount.isNullOrEmpty() || doctorData?.consultationCount == "0") {
            binding.tvPatient.gone()
            binding.tvPatientV.gone()
        } else
            binding.tvPatientV.text = doctorData?.consultationCount ?: getString(R.string.na)

        if (doctorData?.profile?.working_since == null) {
            binding.tvExperience.gone()
            binding.tvExperienceV.gone()
        } else
            binding.tvExperienceV.text =
                    "${getAge(doctorData?.profile?.working_since)} ${getString(R.string.years)}"

        binding.tvReviewsV.text = doctorData?.reviewCount ?: getString(R.string.na)
        binding.tvReviewCount.text = getUserRating(doctorData?.totalRating)

        val serviceList = ArrayList<Service>()
        serviceList.addAll(doctorData?.services ?: emptyList())
        val adapter = ServicesAdapter(this, serviceList)
        binding.rvServices.adapter = adapter


        var additions = ""
        doctorData?.custom_fields?.forEach {
            if (it.field_name == CustomFields.QUALIFICATION) {
                additions += getString(R.string.qualifications_s, it.field_value)
                return@forEach
            }
        }

        doctorData?.master_preferences?.forEach {
            additions += "${it.preference_name} : "

            it.options?.forEach {
                if (it.isSelected)
                    additions += "${it.option_name}, "
            }

            additions = additions.removeSuffix(", ")
            additions += "\n"
        }

        binding.tvAdditionals.text = additions
    }


    private fun setAdapter() {
        adapter = RatingAdapter(items)
        binding.rvReview.adapter = adapter
    }

    private fun listeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.ivMark.setOnClickListener {
            if (isConnectedToInternet(this, true)) {
                shareDeepLink(DeepLink.USER_PROFILE, this, doctorData)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.SECOND_OPINION -> {
                    secondOpinion = SecondOpinion()
                    secondOpinion = data?.getSerializableExtra(EXTRA_SECOND_OPINION) as SecondOpinion
                    hiApiDoctorRequest(scheduleType)
                }
                AppRequestCode.NEW_APPOINTMENT -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    fun serviceClick(item: Service) {
        serviceSelected = item
        if (userRepository.isUserLoggedIn()) {
            bottomOption()
        } else {
            val fragment = WelcomeFragment()
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    private fun bottomOption() {
        if (serviceSelected?.need_availability == "1") {
            val fragment = BottomRequestFragment(this)
            fragment.show(supportFragmentManager, fragment.tag)
        } else {
            hiApiDoctorRequest(false)
        }
    }

    private fun showCreateRequestDialog() {
        AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getString(R.string.create_request))
                .setMessage(getString(R.string.create_request_message))
                .setPositiveButton(getString(R.string.create_request)) { dialog, which ->
                    hiApiDoctorRequest(false)
                }.setNegativeButton(getString(R.string.cancel)) { dialog, which ->

                }.show()
    }


    companion object {
        const val DOCTOR_ID = "DOCTOR_ID"
        const val EXTRA_SECOND_OPINION = "SECOND_OPINION"
    }
}
