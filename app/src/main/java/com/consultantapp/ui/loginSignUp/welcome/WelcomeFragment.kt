package com.consultantapp.ui.loginSignUp.welcome

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.network.ApiKeys
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.ProviderType
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentWelcomeBinding
import com.consultantapp.di.DaggerBottomSheetDialogFragment
import com.consultantapp.ui.LoginViewModel
import com.consultantapp.ui.loginSignUp.SignUpActivity
import com.consultantapp.ui.loginSignUp.SignUpActivity.Companion.EXTRA_LOGIN
import com.consultantapp.ui.loginSignUp.SignUpActivity.Companion.EXTRA_SIGNUP_EMAIL
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject


class WelcomeFragment : DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentWelcomeBinding

    private var rootView: View? = null

    private var callbackManager: CallbackManager? = CallbackManager.Factory.create()

    lateinit var mGoogleSignInClient: GoogleSignInClient

    lateinit var gso: GoogleSignInOptions

    private val RC_SIGN_IN = 111

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: LoginViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_welcome, container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        binding.tvTitle.text = formatString()

        //initial google sign in
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.setText(setAcceptTerms(requireActivity()), TextView.BufferType.SPANNABLE)

    }

    private fun formatString(): SpannableString {
        val createAccount = getString(R.string.create_a_account, getString(R.string.app_name))
        val appName = getString(R.string.app_name)
        val stringFinal = SpannableString.valueOf(createAccount)
        stringFinal.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorPrimary)),
                createAccount.indexOf(appName),
                (createAccount.indexOf(appName) + appName.length), 0)

        return stringFinal
    }

    private fun listeners() {
        binding.ivCross.setOnClickListener {
            dialog?.dismiss()
        }

        binding.tvMobile.setOnClickListener {
            gotoLogin()
        }

        binding.tvLogin.setOnClickListener {
            gotoLogin()
        }

        binding.tvEmail.setOnClickListener {
            startActivity(Intent(activity, SignUpActivity::class.java)
                    .putExtra(EXTRA_SIGNUP_EMAIL, false))

            dialog?.dismiss()
        }

        binding.tvFacebook.setOnClickListener {
            loginFacebook()
        }

        binding.tvGoogle.setOnClickListener {
            loginGoogle()
        }
    }

    private fun gotoLogin() {
        startActivity(Intent(activity, SignUpActivity::class.java)
                .putExtra(EXTRA_LOGIN, true))

        dialog?.dismiss()
    }

    private fun loginGoogle() {
        mGoogleSignInClient.signOut()
        mGoogleSignInClient.revokeAccess()
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    /*Login with Facebook*/
    private fun loginFacebook() {
        LoginManager.getInstance().logOut()

        LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("public_profile", "email"))

        LoginManager.getInstance().registerCallback(callbackManager, object :
                FacebookCallback<LoginResult> {

            override fun onSuccess(loginResult: LoginResult) {
                if (isConnectedToInternet(requireActivity(), true)) {

                    val hashMap = HashMap<String, Any>()
                    hashMap[ApiKeys.PROVIDER_TYPE] = ProviderType.facebook
                    hashMap[ApiKeys.PROVIDER_VERIFICATION] = loginResult.accessToken.token
                    hashMap[ApiKeys.USER_TYPE] = APP_TYPE

                    viewModel.login(hashMap)
                }
            }

            override fun onCancel() {
                Log.e("FBLOGIN_FAILD", "Cancel")
            }

            override fun onError(error: FacebookException) {
                Log.e("FBLOGIN_FAILD", "ERROR", error)
            }
        })
    }


    private fun bindObservers() {
        viewModel.login.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    prefsManager.save(USER_DATA, it.data)

                    if (!userRepository.isUserLoggedIn()) {
                        gotoLogin()
                    } else {
                        requireActivity().setResult(Activity.RESULT_OK)
                        dialog?.dismiss()
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                if (isConnectedToInternet(requireActivity(), true)) {

                    val hashMap = HashMap<String, Any>()
                    hashMap[ApiKeys.PROVIDER_TYPE] = ProviderType.google
                    hashMap[ApiKeys.PROVIDER_VERIFICATION] = account?.idToken ?: ""
                    hashMap[ApiKeys.USER_TYPE] = APP_TYPE

                    viewModel.login(hashMap)
                }

            } catch (e: ApiException) {
                Log.e("Google_FAILD", "ERROR", e)
            }
        }
    }


}
