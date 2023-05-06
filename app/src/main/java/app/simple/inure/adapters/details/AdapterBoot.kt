package app.simple.inure.adapters.details

import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.apk.utils.ReceiversUtils
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleLinearLayout
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.decorations.views.AppIconImageView
import app.simple.inure.glide.util.ImageLoader.loadIconFromActivityInfo
import app.simple.inure.util.AdapterUtils

class AdapterBoot(private val resolveInfoList: ArrayList<ResolveInfo>, val keyword: String)
    : RecyclerView.Adapter<AdapterBoot.Holder>() {

    private lateinit var bootCallbacks: BootCallbacks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.adapter_receivers, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.icon.loadIconFromActivityInfo(resolveInfoList[position].activityInfo)
        holder.name.text = resolveInfoList[position].activityInfo.name.substring(resolveInfoList[position].activityInfo.name.lastIndexOf(".") + 1)
        holder.packageId.text = resolveInfoList[position].activityInfo.name
        holder.status.text = holder.itemView.context.getString(
                R.string.activity_status,

                if (resolveInfoList[position].activityInfo.exported) {
                    holder.itemView.context.getString(R.string.exported)
                } else {
                    holder.itemView.context.getString(R.string.not_exported)
                },

                if (ReceiversUtils.isEnabled(holder.itemView.context, resolveInfoList[position].activityInfo.packageName, resolveInfoList[position].activityInfo.name)) {
                    holder.itemView.context.getString(R.string.enabled)
                } else {
                    holder.itemView.context.getString(R.string.disabled)
                }
        )

        // holder.status.append(receivers[position].status)
        // holder.name.setTrackingIcon(receivers[position].trackerId.isNullOrEmpty().not())

        holder.container.setOnLongClickListener {
            bootCallbacks
                .onBootLongPressed(
                        resolveInfoList[holder.absoluteAdapterPosition].activityInfo.name,
                        it,
                        ReceiversUtils.isEnabled(holder.itemView.context, resolveInfoList[position].activityInfo.packageName, resolveInfoList[holder.absoluteAdapterPosition].activityInfo.name),
                        holder.absoluteAdapterPosition)
            true
        }

        holder.container.setOnClickListener {
            bootCallbacks
                .onBootClicked(resolveInfoList[holder.absoluteAdapterPosition])
        }

        if (keyword.isNotBlank()) {
            AdapterUtils.searchHighlighter(holder.name, keyword)
            AdapterUtils.searchHighlighter(holder.packageId, keyword)
        }
    }

    override fun getItemCount(): Int {
        return resolveInfoList.size
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val icon: AppIconImageView = itemView.findViewById(R.id.adapter_receiver_icon)
        val name: TypeFaceTextView = itemView.findViewById(R.id.adapter_receiver_name)
        val packageId: TypeFaceTextView = itemView.findViewById(R.id.adapter_receiver_process)
        val status: TypeFaceTextView = itemView.findViewById(R.id.adapter_receiver_status)
        val container: DynamicRippleLinearLayout = itemView.findViewById(R.id.adapter_receiver_container)
    }

    fun setBootCallbacks(bootCallbacks: BootCallbacks) {
        this.bootCallbacks = bootCallbacks
    }

    companion object {
        interface BootCallbacks {
            fun onBootClicked(activityInfoModel: ResolveInfo)
            fun onBootLongPressed(packageId: String, icon: View, isComponentEnabled: Boolean, position: Int)
        }
    }
}