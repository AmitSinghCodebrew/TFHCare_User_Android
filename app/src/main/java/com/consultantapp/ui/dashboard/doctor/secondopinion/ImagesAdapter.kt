package com.consultantapp.ui.dashboard.doctor.secondopinion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.consultantapp.R
import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.databinding.RvItemImageBinding
import com.consultantapp.ui.dashboard.doctor.symptom.SymptomDetailFragment
import com.consultantapp.utils.DocType
import com.consultantapp.utils.gone
import com.consultantapp.utils.visible


class ImagesAdapter(private val fragment: Fragment, private val items: ArrayList<DocImage>) :
        RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    val MAX_ITEM = 3

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (items.isEmpty() || (position == 0 && items.size < MAX_ITEM))
            holder.bind(null)
        else if (items.size == MAX_ITEM)
            holder.bind(items[position])
        else
            holder.bind(items[position - 1])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.rv_item_image, parent, false))

    }

    override fun getItemCount(): Int = if (items.size == MAX_ITEM) items.size else items.size + 1

    inner class ViewHolder(val binding: RvItemImageBinding) :
            RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context

        init {
            binding.ivImage.setOnClickListener {
                if (items.size < MAX_ITEM && bindingAdapterPosition == 0) {
                    if (fragment is SecondOpinionFragment)
                        fragment.clickItem()
                    else if (fragment is SymptomDetailFragment)
                        fragment.clickItem()
                }
            }

            binding.ivDelete.setOnClickListener {
                if (items.size == MAX_ITEM)
                    items.removeAt(bindingAdapterPosition)
                else
                    items.removeAt(bindingAdapterPosition - 1)

                notifyDataSetChanged()
            }
        }


        fun bind(item: DocImage?) = with(binding) {
            ivImage.setBackgroundResource(R.drawable.drawable_grey_stroke)
            if (items.size < MAX_ITEM && bindingAdapterPosition == 0) {
                ivImage.setImageResource(R.drawable.ic_camera)
                ivDelete.gone()
            } else {
                if (item?.type == DocType.PDF) {
                    ivImage.setBackgroundResource(R.drawable.ic_pdf)
                    Glide.with(context).load("").into(ivImage)
                }else
                    Glide.with(context).load(item?.imageFile).into(ivImage)
                ivDelete.visible()
            }
        }
    }

}
