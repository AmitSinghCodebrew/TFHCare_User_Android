package com.consultantapp.ui.dashboard.home.questions

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.BottomPriceQuestionBinding
import com.consultantapp.di.DaggerBottomSheetDialogFragment
import com.consultantapp.utils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject


class BottomPriceFragment(private val fragment: AskQuestionFragment) :
        DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: BottomPriceQuestionBinding

    private var items = ArrayList<Feed>()

    private lateinit var adapter: AskQuestionPriceAdapter

    private lateinit var viewModel: QuestionViewModel


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_price_question, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        listeners()
        setAdapter()
        hitApi()
        bindObservers()
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[QuestionViewModel::class.java]
    }

    private fun setAdapter() {
        adapter = AskQuestionPriceAdapter(this, items)
        binding.rvList.adapter = adapter
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            viewModel.supportPackages(hashMap)
        }
    }

    private fun listeners() {
    }

    fun clickItem(pos: Int) {
        fragment.askQuestion(items[pos].id?:"")
        dialog?.dismiss()
    }


    private fun bindObservers() {
        viewModel.supportPackages.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()

                    items.clear()
                    items.addAll(it.data?.support_packages ?: emptyList())

                    adapter.notifyDataSetChanged()
                    binding.clNoData.hideShowView(items.isEmpty())
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
    }
}