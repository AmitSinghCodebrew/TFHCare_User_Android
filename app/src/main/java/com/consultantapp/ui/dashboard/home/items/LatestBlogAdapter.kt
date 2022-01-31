package com.consultantapp.ui.dashboard.home.items

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.data.network.LoadingStatus.ITEM
import com.consultantapp.data.network.LoadingStatus.LOADING
import com.consultantapp.databinding.ItemPagingLoaderBinding
import com.consultantapp.databinding.RvItemLatestBlogBinding
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.utils.EXTRA_REQUEST_ID
import com.consultantapp.utils.PAGE_TO_OPEN
import com.consultantapp.utils.loadImage


class LatestBlogAdapter(private val fragment: Fragment, private val items: ArrayList<Feed>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.rv_item_latest_blog, parent, false
                    )
            )
        } else {
            ViewHolderLoader(
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_paging_loader, parent, false
                    )
            )
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemLatestBlogBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cvBlog.setOnClickListener {
                fragment.startActivity(Intent(fragment.requireActivity(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.BLOGS_DETAILS)
                        .putExtra(EXTRA_REQUEST_ID, items[adapterPosition]))
            }
        }

        fun bind(item: Feed) = with(binding) {
            tvName.text = item.title
            loadImage(ivImage, item.image, R.drawable.image_placeholder)
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
