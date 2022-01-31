package com.consultantapp.ui.dashboard.home.questions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Feed
import com.consultantapp.databinding.RvItemQuestionPriceBinding
import com.consultantapp.utils.getCurrency
import com.consultantapp.utils.loadImage


class AskQuestionPriceAdapter(private val fragmentMain: BottomPriceFragment,
                              private val items: ArrayList<Feed>) : RecyclerView.Adapter<AskQuestionPriceAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.rv_item_question_price, parent, false))
    }

    override fun getItemCount(): Int = items.size


    inner class ViewHolder(val binding: RvItemQuestionPriceBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                fragmentMain.clickItem(adapterPosition)
            }
        }

        fun bind(item: Feed) = with(binding) {

            tvTitle.text = item.title
            tvDesc.text = item.description
            tvPrice.text= getCurrency(item.price)

            loadImage(binding.ivIcon, item.image_icon, 0)

            try {
                if (item.color_code.isNullOrEmpty())
                    clMain.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
                else
                    clMain.setBackgroundColor(Color.parseColor(item.color_code))
            } catch (e: Exception) {
            }

        }
    }
}
