package com.consultantapp.ui.drawermenu.wallet

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Wallet
import com.consultantapp.data.network.ApiKeys.AFTER
import com.consultantapp.data.network.ApiKeys.PER_PAGE
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PER_PAGE_LOAD
import com.consultantapp.data.network.PushType
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.databinding.FragmentWalletBinding
import com.consultantapp.ui.drawermenu.addmoney.AddMoneyActivity
import com.consultantapp.utils.*
import com.consultantapp.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.item_no_data.view.*
import javax.inject.Inject

class WalletFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentWalletBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: WalletViewModel

    private var items = ArrayList<Wallet>()

    private lateinit var adapter: WalletAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private var isReceiverRegistered = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_wallet, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setAdapter()
            bindObservers()

            if (isConnectedToInternet(requireContext(), false))
                viewModel.wallet(HashMap())
            hitApi(true)
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[WalletViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.clLoader.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.colorWhite
            )
        )


        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_wallet_empty)
        binding.clNoData.tvNoData.text = getString(R.string.no_transaction)
        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_transaction_desc)

    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvAddMoney.setOnClickListener {

            /*val jitsiClass = JitsiClass()
            jitsiClass.id = "123"
            jitsiClass.call_id = "123"
            jitsiClass.callType = CallFrom.VIDEO_CALL
            jitsiClass.name = ""

            val intent = Intent(requireContext(), JitsiActivity::class.java)
            intent.putExtra(EXTRA_CALL_NAME, jitsiClass)
            startActivity(intent)*/

            disableButton(binding.tvAddMoney)
            startActivityForResult(
                Intent(requireContext(), AddMoneyActivity::class.java),
                AppRequestCode.ADD_MONEY
            )
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.wallet(HashMap())
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


    private fun setAdapter() {
        adapter = WalletAdapter(items)
        binding.rvListing.adapter = adapter
        binding.rvListing.itemAnimator = null
    }

    fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            val hashMap = HashMap<String, String>()
            if (!isFirstPage && items.isNotEmpty())
                hashMap[AFTER] = items[items.size - 1].id ?: ""

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()
            hashMap["transaction_type"] = "all"
            viewModel.walletHistory(hashMap)
        } else
            binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.wallet.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.tvAmount.text = getCurrency(it.data?.balance)

                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {

                }
            }
        })

        viewModel.walletHistory.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.clLoader.setBackgroundColor(0)
                    binding.clLoader.gone()

                    isLoadingMoreItems = false

                    val tempList = it.data?.payments ?: emptyList()
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
                    binding.swipeRefreshLayout.isRefreshing = false
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    if (!binding.swipeRefreshLayout.isRefreshing && !isLoadingMoreItems)
                        binding.clLoader.visible()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == AppRequestCode.ADD_MONEY) {
                requireActivity().runOnUiThread {
                    viewModel.wallet(HashMap())
                    hitApi(true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.AMOUNT_RECEIVED)
            intentFilter.addAction(PushType.BALANCE_ADDED)
            intentFilter.addAction(PushType.BALANCE_FAILED)
            intentFilter.addAction(PushType.BOOKING_RESERVED)
            intentFilter.addAction(PushType.ASKED_QUESTION)
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
                PushType.AMOUNT_RECEIVED, PushType.BALANCE_ADDED, PushType.BALANCE_FAILED,
                PushType.BOOKING_RESERVED, PushType.ASKED_QUESTION -> {
                    viewModel.wallet(HashMap())
                    hitApi(true)
                }
            }
        }
    }
}