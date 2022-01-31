package com.consultantapp.ui.classes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Categories
import com.consultantapp.data.network.ApiKeys.AFTER
import com.consultantapp.data.network.ApiKeys.PER_PAGE
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PER_PAGE_LOAD
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.ActivityListingToolbarBinding
import com.consultantapp.ui.dashboard.CategoriesAdapter
import com.consultantapp.ui.dashboard.doctor.listing.DoctorListActivity
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment.Companion.CLASSES_PAGE
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.ui.loginSignUp.welcome.WelcomeFragment
import com.consultantapp.utils.*
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class CategoriesFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityListingToolbarBinding

    private var rootView: View? = null

    private lateinit var viewModel: ClassesViewModel

    private var items = ArrayList<Categories>()

    private lateinit var adapter: CategoriesAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false


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
        binding.tvTitle.text = "${getString(R.string.meet)} ${getString(R.string.with_experts)}"
        viewModel = ViewModelProvider(this, viewModelFactory)[ClassesViewModel::class.java]
    }

    private fun setAdapter() {
        binding.rvListing.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = CategoriesAdapter(this, items)
        binding.rvListing.adapter = adapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi(true)
        }

        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as GridLayoutManager
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

            viewModel.categories(hashMap)
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.categories.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false

                    isLoadingMoreItems = false

                    val tempList = it.data?.classes_category ?: emptyList()
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


    fun clickItem(item: Categories?) {
        if (item?.is_subcategory == true) {
            startActivity(Intent(requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.SUB_CATEGORY)
                            .putExtra(CLASSES_PAGE, arguments?.getBoolean(CLASSES_PAGE))
                            .putExtra(SubCategoryFragment.CATEGORY_PARENT_ID, item))
        } else if (arguments?.getBoolean(CLASSES_PAGE) == true) {
            if (userRepository.isUserLoggedIn()) {
                startActivity(Intent(requireContext(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.CLASSES)
                        .putExtra(SubCategoryFragment.CATEGORY_PARENT_ID, item))
            } else {
                val fragment = WelcomeFragment()
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
            }
        } else {
            startActivity(
                    Intent(requireContext(), DoctorListActivity::class.java)
                            .putExtra(SubCategoryFragment.CATEGORY_PARENT_ID, item)
            )
        }
    }
}
