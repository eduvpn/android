/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import nl.eduvpn.app.R
import nl.eduvpn.app.adapter.animator.ServerItemAnimator
import nl.eduvpn.app.adapter.viewholder.ServerChildViewHolder
import nl.eduvpn.app.adapter.viewholder.ServerViewHolder
import nl.eduvpn.app.databinding.ListItemServerBinding
import nl.eduvpn.app.entity.DiscoveredInstance
import nl.eduvpn.app.utils.FormattingUtils

/**
 * Adapter for the providers list.
 * Created by Daniel Zolnai on 2016-10-07.
 */
class ServerAdapter(context: Context) : ListAdapter<DiscoveredInstance, ServerViewHolder>(object : DiffUtil.ItemCallback<DiscoveredInstance>() {
    override fun areItemsTheSame(oldItem: DiscoveredInstance, newItem: DiscoveredInstance): Boolean {
        return oldItem.instance.sanitizedBaseURI == newItem.instance.sanitizedBaseURI
    }

    override fun areContentsTheSame(oldItem: DiscoveredInstance, newItem: DiscoveredInstance): Boolean {
        return oldItem == newItem
    }
}) {

    companion object {
        private const val ITEM_TYPE_PARENT = 0
        private const val ITEM_TYPE_CHILD = 1

        private const val ANIM_DURATION = 300L
        private const val ROTATION_COLLAPSED = 0f
        private const val ROTATION_EXPANDED = 90f

    }

    private val layoutInflater = LayoutInflater.from(context)

    private var expandedDetailPosition = RecyclerView.NO_POSITION
    private var expandedItemCount = 0
    private var detailIndexMapping = mutableMapOf<Int, Int>() // format: key is the visible index to the user, value is the index in the API object


    private var expandedItem: DiscoveredInstance? = null

    override fun getItemId(position: Int): Long {
        val itemId = if (isDetailExpanded() && position > expandedDetailPosition && position <= expandedDetailPosition + expandedItemCount) {
            // Detail
            val expandedParentHashcode = getItem(expandedDetailPosition).hashCode()
            val visibleIndex = position - expandedDetailPosition - 1
            val apiObjectIndex = detailIndexMapping[visibleIndex]!!
            val detailItem = expandedItem?.instance?.peerList?.getOrNull(apiObjectIndex)!!
            -expandedParentHashcode - detailItem.hashCode() // Detail item is negative so it surely doesn't collide with regular order item ID
        } else if (isDetailExpanded() && position > expandedDetailPosition) {
            getItem(position - expandedItemCount).hashCode()
        } else {
            getItem(position).hashCode()
        }
        return itemId.toLong()
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        if (isDetailExpanded()) {
            return count + expandedItemCount
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return if (isDetailExpanded() && position > expandedDetailPosition && position <= expandedDetailPosition + expandedItemCount) {
            ITEM_TYPE_CHILD
        } else {
            ITEM_TYPE_PARENT
        }
    }

    private fun adapterPositionToItemIndex(adapterPosition: Int): Int {
        return if (isDetailExpanded() && adapterPosition > expandedDetailPosition && adapterPosition <= expandedDetailPosition + expandedItemCount) {
            expandedDetailPosition
        } else if (isDetailExpanded() && adapterPosition > expandedDetailPosition) {
            adapterPosition - expandedItemCount
        } else {
            adapterPosition
        }
    }

    private fun isDetailExpanded(): Boolean {
        return expandedDetailPosition != RecyclerView.NO_POSITION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ListItemServerBinding.inflate(layoutInflater, parent, false)
        return when (viewType) {
            ITEM_TYPE_PARENT -> ServerParentViewHolder(binding)
            ITEM_TYPE_CHILD -> ServerChildViewHolder(binding)
            else -> throw RuntimeException("Unexpected item type!")
        }
    }

    private fun findInstanceForPosition(position: Int): DiscoveredInstance {
        return if (isDetailExpanded() && position > expandedDetailPosition && position <= expandedDetailPosition + expandedItemCount) {
            // Detail
            val visibleIndex = position - expandedDetailPosition - 1
            val apiObjectIndex = detailIndexMapping[visibleIndex]!!
            DiscoveredInstance(expandedItem!!.instance.peerList?.getOrNull(apiObjectIndex)!!, expandedItem!!.peerListInstancesCachedOnly[apiObjectIndex])
        } else if (isDetailExpanded() && position > expandedDetailPosition) {
            getItem(position - expandedItemCount)
        } else {
            getItem(position)
        }
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val instance = findInstanceForPosition(position)
        if (holder is ServerParentViewHolder) {
            holder.bind(instance)
        } else if (holder is ServerChildViewHolder) {
            holder.bind(instance)
        }
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int, payloads: MutableList<Any>) {
        when (getItemViewType(position)) {
            ITEM_TYPE_PARENT -> {
                val instance = findInstanceForPosition(position)
                (holder as ServerParentViewHolder).bind(instance, payloads)
            }
            else -> onBindViewHolder(holder, position)
        }
    }


    private fun getParentItem(adapterPosition: Int): DiscoveredInstance {
        return getItem(adapterPositionToItemIndex(adapterPosition))!!
    }

    fun getDiscoveredInstance(position: Int): DiscoveredInstance {
        return findInstanceForPosition(position)
    }


    private fun collapseDetail(expandAfterReload: Boolean = false) {
        if (!isDetailExpanded()) {
            return
        }
        val hasPreviousItems = expandedItemCount > 0
        if (expandAfterReload) {
            expandedItem = getItem(expandedDetailPosition)
        }

        notifyItemChanged(expandedDetailPosition, ServerItemAnimator.COLLAPSE_DETAIL)
        if (hasPreviousItems) {
            notifyItemRangeRemoved(expandedDetailPosition + 1, expandedItemCount)
        }
        expandedDetailPosition = RecyclerView.NO_POSITION
        expandedItemCount = 0
        detailIndexMapping.clear()
    }

    private fun loadDetailForItem(adapterPosition: Int) {
        // show detail items below this
        val parentItem = getParentItem(adapterPosition)
        expandedDetailPosition = adapterPositionToItemIndex(adapterPosition)
        notifyItemChanged(expandedDetailPosition, ServerItemAnimator.EXPAND_DETAIL)
        expandedItem = parentItem
        expandedItemCount = parentItem.instance.peerList?.size ?: 0
        if (expandedItemCount > 0) {
            // Here we order the items as they should be, then we create an index mapping to convert between the API version
            // and our wanted version
            val orderedPeerList = parentItem.instance.peerList?.sortedWith(compareBy { it.displayName })!!
            orderedPeerList.forEachIndexed { sortedIndex, item ->
                val indexInAPIObject = parentItem.instance.peerList.indexOf(item)
                // Should always be a valid number
                detailIndexMapping[sortedIndex] = indexInAPIObject
            }
            notifyItemRangeInserted(expandedDetailPosition + 1, expandedItemCount)
        } else {
            collapseDetail()
        }
    }


    inner class ServerParentViewHolder(private val binding: ListItemServerBinding) : ServerViewHolder(binding) {

        fun bind(discoveredInstance: DiscoveredInstance, partialChanges: List<Any>? = null) {
            if (partialChanges == null ||
                    partialChanges.isEmpty() ||
                    !(partialChanges.contains(ServerItemAnimator.COLLAPSE_DETAIL) || partialChanges.contains(ServerItemAnimator.EXPAND_DETAIL))) {
                val instance = discoveredInstance.instance
                if (instance.peerList?.isNotEmpty() == true) {
                    binding.serverName.setText(R.string.server_selection_secure_internet)
                } else {
                    binding.serverName.text = FormattingUtils.formatDisplayName(instance)
                }
                if (!TextUtils.isEmpty(instance.logoUri)) {
                    Picasso.get()
                            .load(instance.logoUri)
                            .fit()
                            .into(binding.serverIcon)
                } else if (instance.isCustom) {
                    binding.serverIcon.setImageResource(R.drawable.ic_custom_url)
                } else if (instance.peerList?.isNotEmpty() == true ){
                    binding.serverIcon.setImageResource(R.drawable.ic_secure_internet)
                } else {
                    binding.serverIcon.setImageResource(R.drawable.ic_institute)
                }
                val hasPeers = instance.peerList?.isNotEmpty() == true
                binding.groupArrow.isVisible = hasPeers
                if (hasPeers) {
                    itemView.setOnClickListener {
                        val collapsingSelf = expandedDetailPosition == adapterPosition
                        collapseDetail()
                        if (collapsingSelf) {
                            return@setOnClickListener
                        }
                        loadDetailForItem(adapterPosition)
                    }
                }
                if (discoveredInstance.isCachedOnly) {
                    binding.serverName.alpha = 0.5f
                    binding.serverIcon.alpha = 0.5f
                } else {
                    binding.serverName.alpha = 1f
                    binding.serverIcon.alpha = 1f
                }
            }
            setExpanded(adapterPosition == expandedDetailPosition)
        }


        fun expand() {
            binding.groupArrow.rotation = ROTATION_COLLAPSED
            binding.groupArrow.animate().rotation(ROTATION_EXPANDED).setDuration(ANIM_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            itemView.setHasTransientState(true)
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            itemView.setHasTransientState(false)
                        }
                    })
        }

        fun collapse() {
            binding.groupArrow.rotation = ROTATION_EXPANDED
            binding.groupArrow.animate().rotation(ROTATION_COLLAPSED).setDuration(ANIM_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            itemView.setHasTransientState(true)
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            itemView.setHasTransientState(false)
                        }
                    })
        }

        private fun setExpanded(expanded: Boolean) {
            binding.groupArrow.rotation = if (expanded) ROTATION_EXPANDED else ROTATION_COLLAPSED
        }

    }
}
