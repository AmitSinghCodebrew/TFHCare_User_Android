package com.consultantapp.ui.dashboard.home.questions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.data.network.ApiKeys.AFTER
import com.consultantapp.data.network.ApiKeys.PER_PAGE
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PER_PAGE_LOAD
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.ActivityListingToolbarBinding
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.item_no_data.view.*
import javax.inject.Inject

class QuestionsFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityListingToolbarBinding

    private var rootView: View? = null

    private lateinit var viewModel: QuestionViewModel

    private lateinit var progressDialog: ProgressDialog

    private var items = ArrayList<Feed>()

    private lateinit var adapter: QuestionsAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                    inflater, R.layout.activity_listing_toolbar,
                    container, false
            )
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

        binding.toolbar.title = getString(R.string.my_question)
        binding.tvTitle.gone()
        binding.tvAdd.visible()
        binding.tvAdd.text = getString(R.string.ask_plus)

        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_requests_empty_state)
        binding.clNoData.tvNoData.text = getString(R.string.no_question)
        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_question_desc)

    }

    private fun setAdapter() {
        adapter = QuestionsAdapter(this, items)
        binding.rvListing.adapter = adapter
        binding.rvListing.itemAnimator = null
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi(true)
        }

        binding.tvAdd.setOnClickListener {
            replaceFragment(
                    requireActivity().supportFragmentManager,
                    AskQuestionFragment(), R.id.container
            )
        }

        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount - 1
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMoreItems && !isLastPage && lastVisibleItemPosition >= totalItemCount) {
                    isLoadingMoreItems = true
                    hitApi(false)
                }
            }
        })
    }


    private fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }
            val hashMap = HashMap<String, String>()
            if (!isFirstPage && items.isNotEmpty())
                hashMap[AFTER] = items[items.size - 1].id ?: ""

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()

            hashMap["service_type"] = CallType.ALL
            viewModel.getQuestions(hashMap)
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.getQuestions.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false

                    isLoadingMoreItems = false

                    val tempList = it.data?.questions ?: emptyList()
                    if (isFirstPage) {
                        isFirstPage = false
                        items.clear()
                        items.addAll(tempList)

                        adapter.notifyDataSetChanged()
                    } else {
                        val oldSize = items.size
                        items.addAll(tempList)

                        adapter.notifyItemRangeInserted(oldSize, items.size)
                    }
                    isLastPage = tempList.size < PER_PAGE_LOAD
                    adapter.setAllItemsLoaded(isLastPage)

                    binding.clNoData.hideShowView(items.isEmpty())
                }
                Status.ERROR -> {
                    isLoadingMoreItems = false
                    adapter.setAllItemsLoaded(true)

                    binding.swipeRefresh.isRefreshing = false
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    if (!isLoadingMoreItems && !binding.swipeRefresh.isRefreshing && items.isEmpty())
                        binding.clLoader.visible()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        hitApi(true)
    }
}