package com.consultantapp.ui.drawermenu.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.requests.SaveAddress
import com.consultantapp.data.models.responses.UserData
import com.consultantapp.data.network.Config
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentProfileBinding
import com.consultantapp.ui.loginSignUp.SignUpActivity
import com.consultantapp.utils.*
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentProfileBinding

    private var rootView: View? = null

    private var userData: UserData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
            rootView = binding.root

            initialise()
            setUserProfile()
            listeners()
        }
        return rootView
    }

    private fun initialise() {
        val userAddress = prefsManager.getObject(USER_ADDRESS, SaveAddress::class.java)
        if (userAddress != null) {
            binding.tvLocation.text = userAddress.locationName
            binding.tvLocation.visible()
        } else
            binding.tvLocation.gone()
    }

    private fun setUserProfile() {
        userData = userRepository.getUser()

        binding.tvName.text = userData?.name ?: ""
        binding.tvAge.text = "${getString(R.string.age)} ${getAge(userData?.profile?.dob)}"
        binding.tvBioV.text = userData?.profile?.bio ?: getString(R.string.na)
        binding.tvEmailV.text = userData?.email ?: getString(R.string.na)
        binding.tvPhoneV.text = "${userData?.country_code ?: ""} ${userData?.phone ?: ""}"
        binding.tvDOBV.text = userData?.profile?.dob ?: getString(R.string.na)

        if (!userData?.profile?.dob.isNullOrEmpty())
            binding.tvDOBV.text = DateUtils.dateFormatChange(DateFormat.DATE_FORMAT,
                    DateFormat.MON_DAY_YEAR, userData?.profile?.dob ?: "")

        loadImage(binding.ivPic, userData?.profile_image, R.drawable.ic_profile_placeholder)


    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvEdit.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), SignUpActivity::class.java)
                    .putExtra(UPDATE_PROFILE, true), AppRequestCode.PROFILE_UPDATE)
        }

        binding.ivPic.setOnClickListener {
            val itemImages = java.util.ArrayList<String>()
            itemImages.add("${Config.imageURL}${ImageFolder.UPLOADS}${userRepository.getUser()?.profile_image}")
            viewImageFull(context as Activity, itemImages, 0)
        }

        binding.tvPhoneUpdate.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), SignUpActivity::class.java)
                    .putExtra(UPDATE_NUMBER, true), AppRequestCode.PROFILE_UPDATE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppRequestCode.PROFILE_UPDATE) {
                setUserProfile()
                requireActivity().setResult(Activity.RESULT_OK)
            }
        }
    }

}
