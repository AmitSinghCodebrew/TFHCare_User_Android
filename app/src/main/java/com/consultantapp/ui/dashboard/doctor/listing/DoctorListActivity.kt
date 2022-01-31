package com.consultantapp.ui.dashboard.doctor.listing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.*
import com.consultantapp.data.network.ApiKeys.PER_PAGE
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PER_PAGE_LOAD
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.ActivityDoctorListingBinding
import com.consultantapp.ui.adapter.CommonFragmentPagerAdapter
import com.consultantapp.ui.dashboard.DoctorViewModel
import com.consultantapp.ui.dashboard.DoctorsAdapter
import com.consultantapp.ui.dashboard.doctor.DoctorActionActivity
import com.consultantapp.ui.dashboard.doctor.detail.prefrence.PrefrenceFragment.Companion.FILTER_DATA
import com.consultantapp.ui.dashboard.home.BannerViewModel
import com.consultantapp.ui.dashboard.home.banner.BannerFragment
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment.Companion.CATEGORY_PARENT_ID
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.item_no_data.view.*
import javax.inject.Inject


class DoctorListActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var binding: ActivityDoctorListingBinding

    private var items = ArrayList<Doctor>()

    private var itemsService = ArrayList<Service>()

    private lateinit var adapter: DoctorsAdapter

    private lateinit var serviceAdapter: ServiceAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: DoctorViewModel

    private lateinit var viewModelBanner: BannerViewModel

    private var categoryData: Categories? = null

    private var serviceId = ""

    var filters = ArrayList<Filter>()

    var filtersOptionId = ""

    var searchText = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_doctor_listing)

        initialise()
        listeners()
        bindObservers()
        setAdapter()

        binding.clLoader.visible()
        hitApi(true)
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        viewModelBanner = ViewModelProvider(this, viewModelFactory)[BannerViewModel::class.java]
        progressDialog = ProgressDialog(this)
        binding.clLoader.setBackgroundResource(R.color.colorWhite)

        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_profile_empty_state)
        binding.clNoData.tvNoData.text = getString(R.string.no_vendor)

        if (intent.hasExtra(EXTRA_CONSULT_TYPE)) {
            val consult = when (intent.getStringExtra(EXTRA_CONSULT_TYPE)) {
                ConsultType.ALL -> getString(R.string.consult_a_doctor)
                else -> getString(R.string.consult_a_doctor)
            }

            binding.tvTitle.text = consult
            binding.clNoData.tvNoDataDesc.text = getString(R.string.no_vendor_desc, consult)

            binding.rvServices.gone()
            binding.ivFilter.gone()

            binding.etSearch.hint = getString(R.string.search_for_consultant,consult)
        } else if (intent.hasExtra(CATEGORY_PARENT_ID)) {
            categoryData = intent.getSerializableExtra(CATEGORY_PARENT_ID) as Categories
            binding.tvTitle.text = categoryData?.name

            binding.etSearch.hint = getString(R.string.search_for_consultant,categoryData?.name)

            binding.ivFilter.hideShowView(categoryData?.is_filters == true)
            binding.clNoData.tvNoDataDesc.text = getString(R.string.no_vendor_desc, categoryData?.name)

            if (isConnectedToInternet(this, false)) {
                val hashMap = HashMap<String, String>()
                hashMap["category_id"] = categoryData?.id ?: ""

                viewModel.services(hashMap)
                viewModelBanner.coupons(hashMap)
            }
        }
    }

    private fun setAdapter() {
        adapter = DoctorsAdapter(this, items)
        binding.rvListing.adapter = adapter
        binding.rvListing.itemAnimator = null

        serviceAdapter = ServiceAdapter(this, itemsService)
        binding.rvServices.adapter = serviceAdapter

    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount - 1
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMoreItems && !isLastPage && lastVisibleItemPosition >= totalItemCount) {
                    hitApi(false)
                }
            }
        })

        binding.ivFilter.setOnClickListener {
            startActivityForResult(Intent(this, DoctorActionActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DoctorActionActivity.FILTER)
                    .putExtra(CATEGORY_PARENT_ID, categoryData)
                    .putExtra(FILTER_DATA, filters), AppRequestCode.ADD_FILTER)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (searchText != binding.etSearch.text.toString().trim()) {
                    searchText = binding.etSearch.text.toString().trim()
                    binding.clLoaderSearch.visible()
                    hitApi(true)
                }
            }
        })

        binding.etSearch.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.etSearch.hideKeyboard()
                return@OnEditorActionListener true
            }
            false
        })
    }


    fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(this, true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
                items.clear()
            }

            val hashMap = HashMap<String, String>()

           /* if (!isFirstPage && items.isNotEmpty()) {
                hashMap[AFTER] = items[items.size - 1].id ?: ""
            }*/
            hashMap["page"] = if (isFirstPage) "1" else ((items.size / PER_PAGE_LOAD) + 1).toString()

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()

            if (serviceId.isNotEmpty())
                hashMap["service_id"] = serviceId

            if (filtersOptionId.isNotEmpty())
                hashMap["filter_option_ids"] = filtersOptionId.removeSuffix(",")

            if (intent.hasExtra(CATEGORY_PARENT_ID))
                hashMap["category_id"] = categoryData?.id ?: ""

            if (searchText.isNotEmpty())
                hashMap["search"] = searchText


            /*User Address*/
           /* val userAddress = prefsManager.getObject(USER_ADDRESS, SaveAddress::class.java)
            if (userAddress != null) {
                hashMap["lat"] = (userAddress.location?.get(1) ?: "").toString()
                hashMap["long"] = (userAddress.location?.get(0) ?: "").toString()
            }
*/

            if (!isLoadingMoreItems) {
                isLoadingMoreItems = true
                viewModel.doctorList(hashMap)
            }
        }
    }

    private fun bindObservers() {
        viewModel.doctorList.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.setBackgroundResource(0)
                    binding.clLoader.gone()
                    binding.clLoaderSearch.gone()

                    isLoadingMoreItems = false

                    val tempList = it.data?.doctors ?: emptyList()
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

                    binding.clLoader.gone()
                    binding.clLoaderSearch.gone()
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    if (!isLoadingMoreItems && binding.clLoaderSearch.visibility != View.VISIBLE)
                        binding.clLoader.visible()
                }
            }
        })

        viewModel.services.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    itemsService.clear()

                    if (it.data?.services?.size ?: 0 > 1) {
                        val service = Service()
                        service.name = getString(R.string.all)
                        service.service_id = ""
                        service.isSelected = true
                        itemsService.add(service)
                    }

                    itemsService.addAll(it.data?.services ?: emptyList())
                    serviceAdapter.notifyDataSetChanged()

                    binding.rvServices.hideShowView(itemsService.size > 1)

                    serviceAdapter.setAllItemsLoaded(true)

                }
                Status.ERROR -> {
                    isLoadingMoreItems = false
                    serviceAdapter.setAllItemsLoaded(true)

                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {

                }
            }
        })

        viewModelBanner.coupons.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    val itemsBanner = ArrayList<Banner>()
                    itemsBanner.addAll(it.data?.coupons ?: emptyList())

                    val adapter = CommonFragmentPagerAdapter(this.supportFragmentManager)
                    itemsBanner.forEach {
                        adapter.addTab("", BannerFragment(null, it))
                    }
                    binding.viewPagerBanner.adapter = adapter
                    binding.pageIndicatorView.setViewPager(binding.viewPagerBanner)

                    if (itemsBanner.isNotEmpty())
                        slideItem(binding.viewPagerBanner, this)

                    binding.viewPagerBanner.hideShowView(itemsBanner.isNotEmpty())
                    binding.pageIndicatorView.hideShowView(itemsBanner.size > 1)
                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.ADD_FILTER -> {
                    filters.clear()

                    val filterSet = data?.getSerializableExtra(FILTER_DATA) as ArrayList<Filter>
                    filters.addAll(filterSet)

                    filtersOptionId = ""
                    filters.forEach {
                        it.options?.forEach {
                            if (it.isSelected)
                                filtersOptionId += it.id + ","
                        }
                    }

                    binding.clLoader.visible()
                    hitApi(true)

                }
                AppRequestCode.NEW_APPOINTMENT -> {
                    finish()
                }
            }
        }
    }

    fun onServiceSelected(item: Service) {
        binding.etSearch.hideKeyboard()
        serviceId = item.service_id ?: ""
        binding.clLoader.visible()
        hitApi(true)
    }

    companion object {
        const val EXTRA_CONSULT_TYPE = "CONSULT_TYPE"
        const val AK_SECOND_OPINION = "Ask Second Opinion"

        object ConsultType {
            const val ALL = "all"
        }
    }
}
