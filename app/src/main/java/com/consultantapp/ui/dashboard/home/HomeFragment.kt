package com.consultantapp.ui.dashboard.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.responses.Banner
import com.consultantapp.data.models.responses.Categories
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.data.network.ApiKeys.PER_PAGE
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.HOME_CATEGORIES
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentHomeBinding
import com.consultantapp.ui.adapter.CommonFragmentPagerAdapter
import com.consultantapp.ui.classes.ClassesViewModel
import com.consultantapp.ui.dashboard.CategoriesAdapter
import com.consultantapp.ui.dashboard.doctor.listing.DoctorListActivity
import com.consultantapp.ui.dashboard.home.banner.BannerFragment
import com.consultantapp.ui.dashboard.home.items.LatestBlogAdapter
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment.Companion.CATEGORY_PARENT_ID
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment.Companion.CLASSES_PAGE
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.ui.loginSignUp.welcome.WelcomeFragment
import com.consultantapp.utils.*
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class HomeFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentHomeBinding

    private var rootView: View? = null

    private lateinit var viewModel: ClassesViewModel

    private lateinit var viewModelBanner: BannerViewModel

    private lateinit var viewModelHome: HomeViewModel

    private var items = ArrayList<Categories>()

    private lateinit var adapter: CategoriesAdapter


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
            hitApi()
        }
        return rootView
    }

    private fun initialise() {

        viewModel = ViewModelProvider(this, viewModelFactory)[ClassesViewModel::class.java]
        viewModelBanner = ViewModelProvider(this, viewModelFactory)[BannerViewModel::class.java]
        viewModelHome = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]
        binding.clLoader.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorWhite))

        if (arguments?.getBoolean(CLASSES_PAGE) == true)
            binding.tvPopular.text = getString(R.string.classes)
        else
            binding.tvPopular.text = getString(R.string.meet)

        /*Set banner height*/
        val widthScreen = resources.displayMetrics.widthPixels - pxFromDp(requireContext(), 32f)
        val heightOfImage = (widthScreen * 0.6).toInt()

        binding.viewPagerBanner.layoutParams.height = heightOfImage
    }

    private fun setAdapter() {
        adapter = CategoriesAdapter(this, items)
        binding.rvCategory.adapter = adapter
    }

    private fun listeners() {
        binding.swipeRefresh.setOnRefreshListener {
            hitApi()
        }

        binding.tvWallet.setOnClickListener {
            if (userRepository.isUserLoggedIn()) {
                startActivity(Intent(requireContext(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.WALLET))
            }

          /*  startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.SYMPTOMS)
                    .putExtra(EXTRA_REQUEST_ID, arguments?.getString(EXTRA_REQUEST_ID)))*/
        }


        binding.tvMoreArticles.setOnClickListener {
            startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, BlogType.QUESTION))
        }

        binding.tvMyQuestion.setOnClickListener {
            if (userRepository.isUserLoggedIn()) {
                startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.MY_QUESTION))
            } else {
                val fragment = WelcomeFragment()
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
            }
        }

        binding.tvMoreCategories.setOnClickListener {
            startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.CATEGORIES)
                    .putExtra(CLASSES_PAGE, arguments?.getBoolean(CLASSES_PAGE)))
        }
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            /*Home*/
            viewModelHome.home()

            val hashMap = HashMap<String, String>()
            hashMap[PER_PAGE] = HOME_CATEGORIES.toString()

            viewModel.categories(hashMap)

            /*Banner api*/
            viewModelBanner.banners()
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModelHome.home.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.tvMyQuestion.visible()

                    val itemsBlogs = ArrayList<Feed>()
                    itemsBlogs.addAll(it.data?.questions ?: emptyList())
                    val adapterBlogs = LatestBlogAdapter(this, itemsBlogs)
                    binding.rvArticle.adapter = adapterBlogs

                    binding.tvArticles.hideShowView(itemsBlogs.isNotEmpty())
                    binding.tvMoreArticles.hideShowView(itemsBlogs.size >= 2)
                    binding.rvArticle.hideShowView(itemsBlogs.isNotEmpty())

                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })

        viewModel.categories.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false

                    items.clear()
                    items.addAll(it.data?.classes_category ?: emptyList())
                    adapter.notifyDataSetChanged()

                    binding.tvNoData.hideShowView(items.isEmpty())
                    binding.tvMoreCategories.hideShowView(items.size == HOME_CATEGORIES)
                }
                Status.ERROR -> {
                    adapter.setAllItemsLoaded(true)
                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false

                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    if (!binding.swipeRefresh.isRefreshing)
                        binding.clLoader.visible()
                }
            }
        })

        viewModelBanner.banners.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    val itemsBanner = ArrayList<Banner>()
                    itemsBanner.addAll(it.data?.banners ?: emptyList())

                    val adapter = CommonFragmentPagerAdapter(childFragmentManager)
                    itemsBanner.forEach {
                        adapter.addTab("", BannerFragment(this, it))
                    }
                    binding.viewPagerBanner.adapter = adapter
                    binding.pageIndicatorView.setViewPager(binding.viewPagerBanner)

                    if (itemsBanner.isNotEmpty())
                        slideItem(binding.viewPagerBanner, requireContext())

                    binding.viewPagerBanner.hideShowView(itemsBanner.isNotEmpty())
                    binding.pageIndicatorView.hideShowView(itemsBanner.size > 1)

                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })
    }


    fun clickItem(item: Categories?) {
        if (item?.is_subcategory == true) {
            startActivity(
                    Intent(requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.SUB_CATEGORY)
                            .putExtra(CLASSES_PAGE, arguments?.getBoolean(CLASSES_PAGE))
                            .putExtra(CATEGORY_PARENT_ID, item)
            )
        } else if (arguments?.getBoolean(CLASSES_PAGE) == true) {
            if (userRepository.isUserLoggedIn()) {
                startActivity(Intent(requireContext(), DrawerActivity::class.java)
                                .putExtra(PAGE_TO_OPEN, DrawerActivity.CLASSES)
                                .putExtra(CATEGORY_PARENT_ID, item))
            } else {
                val fragment = WelcomeFragment()
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
            }
        } else {
            startActivity(
                    Intent(requireContext(), DoctorListActivity::class.java)
                            .putExtra(CATEGORY_PARENT_ID, item)
            )
        }
    }
}
