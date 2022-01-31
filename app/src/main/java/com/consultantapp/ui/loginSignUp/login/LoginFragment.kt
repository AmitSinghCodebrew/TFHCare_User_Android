package com.consultantapp.ui.loginSignUp.login

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.FragmentLoginBinding
import com.consultantapp.ui.LoginViewModel
import com.consultantapp.ui.loginSignUp.loginemail.LoginEmailFragment
import com.consultantapp.ui.loginSignUp.verifyotp.VerifyOTPFragment
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class LoginFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentLoginBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: LoginViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.setText(setAcceptTerms(requireActivity()), TextView.BufferType.SPANNABLE)

        binding.etMobileNumber.requestFocus()
        binding.etMobileNumber.showKeyboard()

        if (arguments?.containsKey(UPDATE_NUMBER) == true) {
            binding.tvTitle.text = getString(R.string.update)

            binding.tvLoginScreen.gone()
            binding.tvResentTitle.gone()
            binding.tvTerms.gone()
        }
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            binding.etMobileNumber.hideKeyboard()
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvLoginScreen.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            replaceFragment(requireActivity().supportFragmentManager, LoginEmailFragment(), R.id.container)
        }

        binding.ivNext.setOnClickListener {
            if (binding.etMobileNumber.text.toString().isEmpty() || binding.etMobileNumber.text.toString().length < 6) {
                binding.etMobileNumber.showSnackBar(getString(R.string.enter_phone_number))
            } else if (isConnectedToInternet(requireContext(), true)) {

                val hashMap = HashMap<String, Any>()
                hashMap["country_code"] = binding.ccpCountryCode.selectedCountryCodeWithPlus
                hashMap["phone"] = binding.etMobileNumber.text.toString()

                viewModel.sendSms(hashMap)
            }
        }
    }

    private fun bindObservers() {
        viewModel.sendSMS.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    val fragment = VerifyOTPFragment()
                    val bundle = Bundle()
                    bundle.putString(COUNTRY_CODE, binding.ccpCountryCode.selectedCountryCodeWithPlus)
                    bundle.putString(PHONE_NUMBER, binding.etMobileNumber.text.toString())
                    if (arguments?.containsKey(UPDATE_NUMBER) == true)
                        bundle.putBoolean(UPDATE_NUMBER, true)
                    fragment.arguments = bundle

                    replaceFragment(requireActivity().supportFragmentManager,
                            fragment, R.id.container)

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

    override fun onResume() {
        super.onResume()
        binding.etMobileNumber.post {
            binding.etMobileNumber.requestFocus()
            binding.etMobileNumber.showKeyboard()
        }
    }
}
