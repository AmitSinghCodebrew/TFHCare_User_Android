package com.consultantapp.ui.walkthrough

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.consultantapp.R
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentWalkthroughBinding
import com.consultantapp.ui.adapter.CommonFragmentPagerAdapter
import com.consultantapp.utils.POSITION
import com.consultantapp.utils.PrefsManager
import com.consultantapp.utils.hideShowView
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class WalkThroughFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentWalkthroughBinding

    private var rootView: View? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_walkthrough, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setBanners()
        }
        return rootView
    }

    private fun initialise() {
        requireActivity().setResult(Activity.RESULT_OK)
    }

    private fun setBanners() {
        val adapter = CommonFragmentPagerAdapter(requireActivity().supportFragmentManager)
        for (i in 0..3) {
            val fragment = WalkthroughDetailFragment()
            val bundle = Bundle()
            bundle.putInt(POSITION, i)
            fragment.arguments = bundle
            adapter.addTab("", fragment)
        }

        binding.viewPager.adapter = adapter

        binding.pageIndicatorView.setViewPager(binding.viewPager)
    }

    private fun listeners() {

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                val show = position == binding.viewPager.adapter?.count?.minus(1)

                binding.tvSkip.hideShowView(!show)
                binding.tvGetStarted.hideShowView(show)
            }

        })

        binding.tvGetStarted.setOnClickListener {
            doneWalkThrough()
        }

        binding.tvSkip.setOnClickListener {
            doneWalkThrough()
        }
    }

    private fun doneWalkThrough() {
        prefsManager.save(WALK_THROUGH_SCREEN, true)
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    companion object {
        const val WALK_THROUGH_SCREEN = "WALK_THROUGH_SCREEN"
    }
}
