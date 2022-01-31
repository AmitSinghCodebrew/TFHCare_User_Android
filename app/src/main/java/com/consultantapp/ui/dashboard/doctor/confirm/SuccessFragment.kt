package com.consultantapp.ui.dashboard.doctor.confirm

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantapp.R
import com.consultantapp.data.network.PushType
import com.consultantapp.databinding.ItemRequestSuccessBinding
import com.consultantapp.ui.dashboard.home.questions.AskQuestionFragment
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.ui.loginSignUp.changepassword.ChangePasswordFragment
import com.consultantapp.utils.EXTRA_REQUEST_ID
import com.consultantapp.utils.PAGE_TO_OPEN
import dagger.android.support.DaggerDialogFragment
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule


class SuccessFragment(private val fragmentMain: Fragment) : DaggerDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ItemRequestSuccessBinding


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.item_request_success, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
    }

    private fun initialise() {
        when (fragmentMain) {
            is ConfirmBookingFragment -> {
                binding.tvTitle.text = getString(R.string.request_sent)
            }
            is AskQuestionFragment -> {
                fragmentMain.successDone()
                binding.tvTitle.text = getString(R.string.thanks_desc)
            }
            is ChangePasswordFragment -> {
                binding.tvTitle.text = getString(R.string.password_changed_successfully)
            }
        }

        Timer().schedule(3000) {
            when (fragmentMain) {
                is ConfirmBookingFragment -> {
                    val intent = Intent()
                    intent.action = PushType.REQUEST_ACCEPTED
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

                    startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.SYMPTOMS)
                            .putExtra(EXTRA_REQUEST_ID, arguments?.getString(EXTRA_REQUEST_ID)))

                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()
                    dialog?.dismiss()
                }
                is AskQuestionFragment -> {
                    dialog?.dismiss()
                }
                is ChangePasswordFragment -> {
                    dialog?.dismiss()
                }
            }
        }
    }

}
