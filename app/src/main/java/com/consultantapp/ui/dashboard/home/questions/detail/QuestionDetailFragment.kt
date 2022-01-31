package com.consultantapp.ui.dashboard.home.questions.detail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantapp.R
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PushType
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.FragmentQuestionDetailBinding
import com.consultantapp.ui.dashboard.home.questions.QuestionViewModel
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class QuestionDetailFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentQuestionDetailBinding

    private var rootView: View? = null

    private var questionId: String? = null

    private var details: Feed? = null

    private var items = ArrayList<Feed>()

    private lateinit var adapter: AnswersAdapter

    private lateinit var viewModel: QuestionViewModel

    private lateinit var progressDialog: ProgressDialog

    private var isReceiverRegistered = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_question_detail,
                    container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[QuestionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.clLoader.setBackgroundResource(R.color.colorWhite)

        questionId = arguments?.getString(EXTRA_REQUEST_ID)
        hitApi()
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["question_id"] = questionId ?:""

            viewModel.getQuestionsDetails(hashMap)
        }
    }

    private fun setAdapter() {
        adapter = AnswersAdapter(this, items)
        binding.rvAnswer.adapter = adapter
    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }
    }

    private fun setData() {
        binding.tvTitle.text = details?.title
        binding.tvDec.text = details?.description

        if (details?.amount == null || details?.amount == "0")
            binding.tvAmount.gone()
        else {
            binding.tvAmount.visible()
            binding.tvAmount.text = getCurrency(details?.amount)
        }


        items.clear()
        items.addAll(details?.answers ?: emptyList())
        adapter.notifyDataSetChanged()

        binding.tvAnswer.hideShowView(items.isNotEmpty())
    }

    private fun bindObservers() {
        viewModel.getQuestionsDetails.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.clLoader.setBackgroundResource(0)

                    details = it.data?.question
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
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.QUESTION_ANSWERED)
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
                PushType.QUESTION_ANSWERED -> {
                    hitApi()
                }
            }
        }
    }
}
