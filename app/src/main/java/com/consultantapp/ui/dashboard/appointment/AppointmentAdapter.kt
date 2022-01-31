package com.consultantapp.ui.dashboard.appointment

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantapp.R
import com.consultantapp.data.models.responses.Request
import com.consultantapp.data.network.LoadingStatus.ITEM
import com.consultantapp.data.network.LoadingStatus.LOADING
import com.consultantapp.databinding.ItemPagingLoaderBinding
import com.consultantapp.databinding.RvItemAppointmentBinding
import com.consultantapp.ui.drawermenu.DrawerActivity
import com.consultantapp.utils.*


class AppointmentAdapter(private val fragment: AppointmentFragment, private val items: ArrayList<Request>) :
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
                            R.layout.rv_item_appointment, parent, false
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

    inner class ViewHolder(val binding: RvItemAppointmentBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvReSchedule.setOnClickListener {
                fragment.rescheduleAppointment(items[adapterPosition])
            }

            binding.tvCancel.setOnClickListener {
                fragment.cancelAppointment(items[adapterPosition])
            }

            binding.tvViewPrescription.setOnClickListener {
                val link = fragment.getString(R.string.pdf_link, items[adapterPosition].id, APP_UNIQUE_ID)
                openPdf(fragment.requireActivity(), link,true)
            }

            binding.root.setOnClickListener {
                fragment.startActivityForResult(Intent(fragment.requireContext(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT_DETAIL)
                        .putExtra(EXTRA_REQUEST_ID, items[adapterPosition].id), AppRequestCode.APPOINTMENT_DETAILS)
            }
        }

        fun bind(item: Request) = with(binding) {
            val context = binding.root.context

            tvCancel.hideShowView(item.canCancel)
            tvReSchedule.hideShowView(item.canReschedule)
            tvReSchedule.text = context.getString(R.string.re_schedule)
            tvViewPrescription.gone()

            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))

            tvName.text = getDoctorName(item.to_user)
            loadImage(binding.ivPic, item.to_user?.profile_image,
                    R.drawable.ic_profile_placeholder)

            tvDateTime.text = "${DateUtils.dateTimeFormatFromUTC(DateFormat.MON_YEAR_FORMAT, item.bookingDateUTC)} · " +
                    "${DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, item.bookingDateUTC)}"

            tvRequestType.text = item.service_type
            tvPrice.text = getCurrency(item.price)


            when (item.status) {
                CallAction.ACCEPT -> {
                    tvStatus.text = context.getString(R.string.accepted)
                    tvReSchedule.gone()
                    tvCancel.gone()
                }
                CallAction.PENDING -> {
                    tvStatus.text = context.getString(R.string.new_request)
                }
                CallAction.COMPLETED -> {
                    tvStatus.text = context.getString(R.string.completed)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.textColorGreen))
                    tvReSchedule.text = context.getString(R.string.book_again)
                    tvReSchedule.visible()
                    tvCancel.gone()

                    tvViewPrescription.hideShowView(item.is_prescription == true)
                }
                CallAction.INPROGRESS, CallAction.BUSY -> {
                    tvStatus.text = context.getString(R.string.inprogess)
                    tvReSchedule.gone()
                    tvCancel.gone()
                }
                CallAction.FAILED -> {
                    tvStatus.text = context.getString(R.string.no_show)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorCancel))
                }
                CallAction.CANCELED -> {
                    tvStatus.text = context.getString(R.string.canceled)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorCancel))
                    tvReSchedule.gone()
                    tvCancel.gone()
                }
                else -> {
                    tvStatus.text = context.getString(R.string.new_request)
                }
            }

            /*Hide All Button*/
            tvCancel.gone()
            tvReSchedule.gone()
            tvViewPrescription.gone()
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
