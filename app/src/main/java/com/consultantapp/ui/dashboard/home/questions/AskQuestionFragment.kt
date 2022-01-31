package com.consultantapp.ui.dashboard.home.questions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentAskQuestionBinding
import com.consultantapp.ui.dashboard.doctor.confirm.SuccessFragment
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class AskQuestionFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentAskQuestionBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: QuestionViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_ask_question, container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
        }
        return rootView
    }


    private fun initialise() {
        editTextScroll(binding.etDesc)

        viewModel = ViewModelProvider(this, viewModelFactory)[QuestionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvSubmit.setOnClickListener {
            checkValidation()
        }
    }


    private fun checkValidation() {
        binding.etTitle.hideKeyboard()
        when {
            binding.etTitle.text.toString().trim().isEmpty() -> {
                binding.etTitle.showSnackBar(getString(R.string.question))
            }
            binding.etDesc.text.toString().trim().isEmpty() -> {
                binding.etDesc.showSnackBar(getString(R.string.description))
            }
            isConnectedToInternet(requireContext(), true) -> {
                val fragment = BottomPriceFragment(this)
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
            }
        }
    }

    fun askQuestion(package_id: String) {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["title"] = binding.etTitle.text.toString().trim()
            hashMap["description"] = binding.etDesc.text.toString().trim()
            hashMap["package_id"] = package_id
            viewModel.askQuestion(hashMap)
        }
    }

    private fun bindObservers() {
        viewModel.askQuestion.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

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
                                        .putExtra(PAGE_TO_OPEN, DrawerActivity.WALLET)
                                )
                            }.show()

                    } else {
                        val fragment = SuccessFragment(this)
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
    }

    fun successDone(){
        requireActivity().setResult(Activity.RESULT_OK)

        if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
            requireActivity().supportFragmentManager.popBackStack()
        else
            requireActivity().finish()
    }

}
