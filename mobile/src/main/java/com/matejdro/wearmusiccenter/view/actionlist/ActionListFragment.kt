package com.matejdro.wearmusiccenter.view.actionlist

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.NinePatchDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.databinding.FragmentActionListBinding
import com.matejdro.wearmusiccenter.util.IdentifiedItem
import com.matejdro.wearmusiccenter.view.TitledActivity
import com.matejdro.wearmusiccenter.view.mainactivity.ConfigActivityComponentProvider
import timber.log.Timber

class ActionListFragment : LifecycleFragment() {
    private lateinit var viewModel: ActionListViewModel
    private lateinit var binding: FragmentActionListBinding
    private lateinit var adapter: RecyclerView.Adapter<ListItemHolder>
    private lateinit var dragDropManager: RecyclerViewDragDropManager

    private var actions: List<IdentifiedItem<PhoneAction>> = emptyList()
    private var ignoreNextUpdate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = ActionListViewModelFactory(
                (activity as ConfigActivityComponentProvider).provideConfigActivityComponent())

        viewModel = ViewModelProviders.of(this, factory)[ActionListViewModel::class.java]

        viewModel.actions.observe(this, actionListListener)
    }

    override fun onStart() {
        super.onStart()

        val activity = activity
        if (activity is TitledActivity) {
            activity.updateActivityTitle(getString(R.string.actions_menu))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_action_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = binding.recycler

        dragDropManager = RecyclerViewDragDropManager()
        dragDropManager.setInitiateOnLongPress(true)
        dragDropManager.setInitiateOnMove(false)
        dragDropManager.setInitiateOnTouch(false)
        dragDropManager.setDraggingItemShadowDrawable(ResourcesCompat.getDrawable(
                resources,
                R.drawable.material_shadow_z3,
                null) as NinePatchDrawable)

        @Suppress("UNCHECKED_CAST")
        adapter = dragDropManager.createWrappedAdapter(ListItemAdapter()) as RecyclerView.Adapter<ListItemHolder>
        recycler.adapter = adapter
        recycler.itemAnimator = DraggableItemAnimator()
        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        recycler.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        dragDropManager.attachRecyclerView(recycler)
    }

    override fun onDestroy() {
        super.onDestroy()

        dragDropManager.release()
    }

    val actionListListener = Observer<List<IdentifiedItem<PhoneAction>>> {
        Timber.d("Received: %s", it)
        if (it == null) {
            return@Observer
        }

        this.actions = it

        if (!ignoreNextUpdate) {
            adapter.notifyDataSetChanged()
        }
        ignoreNextUpdate = false
    }

    private inner class ListItemAdapter : RecyclerView.Adapter<ListItemHolder>(),
            DraggableItemAdapter<ListItemHolder> {
        init {
            setHasStableIds(true)
        }

        override fun onBindViewHolder(holder: ListItemHolder, position: Int) {
            val phoneAction = actions[position].item

            holder.text.text = phoneAction.getName()


            val icon = phoneAction.getIcon()
            if (icon is VectorDrawable) {
                holder.icon.setColorFilter(Color.BLACK)
            } else {
                holder.icon.clearColorFilter()
            }
            holder.icon.setImageDrawable(icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ListItemHolder {
            val view = layoutInflater.inflate(R.layout.item_action_list, parent, false)
            return ListItemHolder(view)
        }

        override fun getItemCount(): Int = actions.size

        override fun getItemId(position: Int): Long = actions[position].id.toLong()

        override fun onGetItemDraggableRange(holder: ListItemHolder?, position: Int)
                : ItemDraggableRange? = null

        override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

        override fun onCheckCanStartDrag(holder: ListItemHolder, position: Int, x: Int, y: Int): Boolean {
            return true
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            ignoreNextUpdate = true
            viewModel.moveItem(fromPosition, toPosition)
        }
    }

    private inner class ListItemHolder(itemView: View) : AbstractDraggableItemViewHolder(itemView) {
        val icon = itemView.findViewById(R.id.icon) as ImageView
        val text = itemView.findViewById(R.id.text) as TextView

        init {
            /* itemView.setOnLongClickListener(View.OnLongClickListener {
                 itemView.isSelected = true
                 itemView.background = ColorDrawable(Color.RED)
                 Timber.d("Selected %s", itemView)
                 adapter.notifyItemChanged(adapterPosition)
                 true
             })*/
        }
    }
}