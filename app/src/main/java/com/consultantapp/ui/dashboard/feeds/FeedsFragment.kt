package com.consultantapp.ui.dashboard.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
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
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class FeedsFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityListingToolbarBinding

    private var rootView: View? = null

    private lateinit var viewModel: FeedViewModel

    private var items = ArrayList<Feed>()

    private lateinit var adapter: FeedsAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private var typeOfBlog = BlogType.QUESTION


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.activity_listing_toolbar, container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
            hitApi(true)
        }
        return rootView
    }

    private fun initialise() {
        when (requireActivity().intent.getStringExtra(PAGE_TO_OPEN)) {
            BlogType.QUESTION -> {
                typeOfBlog = BlogType.QUESTION
                binding.tvTitle.text = getString(R.string.free_questions)
            }
            BlogType.ARTICLE -> {
                typeOfBlog = BlogType.ARTICLE

                binding.tvTitle.text = getString(R.string.latest_articles)
            }
            else -> {
                typeOfBlog = BlogType.ARTICLE
                binding.tvTitle.text = getString(R.string.latest_articles)
            }
        }

        viewModel = ViewModelProvider(this, viewModelFactory)[FeedViewModel::class.java]
    }

    private fun setAdapter() {
        adapter = FeedsAdapter(this, items)
        binding.rvListing.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvListing.adapter = adapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi(true)
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
            hashMap["type"] = typeOfBlog

            viewModel.getFeeds(hashMap)
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.getFeeds.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false

                    isLoadingMoreItems = false

                    val tempList = it.data?.feeds ?: emptyList()
                    if (isFirstPage) {
                        isFirstPage = false
                        items.clear()
                    }

                    items.addAll(tempList)
                    adapter.notifyDataSetChanged()

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
                    if (!isLoadingMoreItems && !binding.swipeRefresh.isRefreshing)
                        binding.clLoader.visible()
                }
            }
        })

    }
}
