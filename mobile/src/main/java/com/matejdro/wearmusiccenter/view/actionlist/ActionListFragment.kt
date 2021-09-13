package com.matejdro.wearmusiccenter.view.actionlist

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.NinePatchDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.os.Vibrator
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.databinding.FragmentActionListBinding
import com.matejdro.wearmusiccenter.di.InjectableViewModelFactory
import com.matejdro.wearmusiccenter.util.IdentifiedItem
import com.matejdro.wearmusiccenter.view.FabFragment
import com.matejdro.wearmusiccenter.view.TitledActivity
import com.matejdro.wearutils.miscutils.VibratorCompat
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ActionListFragment : Fragment(), FabFragment, RecyclerViewDragDropManager.OnItemDragEventListener {
    companion object {
        const val REQUEST_CODE_EDIT_WINDOW = 1031

        private const val STATE_LAST_EDITED_ACTION_POSITION = "LastEditedActionPosition"
    }

    private lateinit var viewModel: ActionListViewModel
    private lateinit var binding: FragmentActionListBinding
    private lateinit var adapter: RecyclerView.Adapter<ListItemHolder>
    private lateinit var dragDropManager: RecyclerViewDragDropManager
    private lateinit var vibrator: Vibrator

    private var actions: List<IdentifiedItem<PhoneAction>> = emptyList()
    private var ignoreNextUpdate: Boolean = false
    private var lastEditedActionPosition = -1

    @Inject
    lateinit var viewModelFactory: InjectableViewModelFactory<ActionListViewModel>

    @Inject
    lateinit var customIconStorage: CustomIconStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            lastEditedActionPosition = savedInstanceState.getInt(STATE_LAST_EDITED_ACTION_POSITION)
        }

        vibrator = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_LAST_EDITED_ACTION_POSITION, lastEditedActionPosition)

        super.onSaveInstanceState(outState)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = binding.recycler

        dragDropManager = RecyclerViewDragDropManager()
        dragDropManager.setInitiateOnLongPress(true)
        dragDropManager.setInitiateOnMove(false)
        dragDropManager.setInitiateOnTouch(false)
        dragDropManager.onItemDragEventListener = this
        dragDropManager.setDraggingItemShadowDrawable(ResourcesCompat.getDrawable(
                resources,
                R.drawable.material_shadow_z3,
                null) as NinePatchDrawable)

        @Suppress("UNCHECKED_CAST")
        adapter = dragDropManager.createWrappedAdapter(ListItemAdapter()) as RecyclerView.Adapter<ListItemHolder>
        recycler.adapter = adapter
        recycler.itemAnimator = DraggableItemAnimator()
        recycler.layoutManager =
            LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )

        recycler.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        dragDropManager.attachRecyclerView(recycler)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        viewModel = ViewModelProviders.of(this, viewModelFactory)[ActionListViewModel::class.java]

        viewModel.actions.observe(viewLifecycleOwner, actionListListener)
        viewModel.openActionEditor.observe(viewLifecycleOwner, openEditDialogListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        dragDropManager.release()
    }

    override fun onFabClicked() {
        val intent = Intent(context, ActionEditorActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_EDIT_WINDOW)
    }

    private val actionListListener = Observer<List<IdentifiedItem<PhoneAction>>> {
        if (it == null) {
            return@Observer
        }

        this.actions = it

        if (!ignoreNextUpdate) {
            adapter.notifyDataSetChanged()
        }
        ignoreNextUpdate = false
    }

    private val openEditDialogListener = Observer<Int> {
        if (it == null || it < 0) {
            return@Observer
        }

        lastEditedActionPosition = it

        val intent = Intent(context, ActionEditorActivity::class.java)
        intent.putExtra(ActionEditorActivity.EXTRA_ACTION, actions[it].item.serialize())
        startActivityForResult(intent, REQUEST_CODE_EDIT_WINDOW)
    }

    private fun buzz() {
        if (isHapticEnabled()) {
            VibratorCompat.vibrate(vibrator, 25)
        }
    }

    private fun isHapticEnabled(): Boolean {
        val contentResolver = activity!!.contentResolver

        val setting = Settings.System.getInt(contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        return setting != 0
    }

    override fun onItemDragStarted(position: Int) {
        buzz()
    }

    override fun onItemDragPositionChanged(fromPosition: Int, toPosition: Int) = Unit

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        buzz()
    }

    override fun onItemDragMoveDistanceUpdated(offsetX: Int, offsetY: Int) = Unit


    private inner class ListItemAdapter : RecyclerView.Adapter<ListItemHolder>(),
            DraggableItemAdapter<ListItemHolder> {
        init {
            setHasStableIds(true)
        }

        override fun onBindViewHolder(holder: ListItemHolder, position: Int) {
            val phoneAction = actions[position].item

            holder.text.text = phoneAction.title


            val icon = customIconStorage[phoneAction]
            if (icon is VectorDrawable) {
                holder.icon.setColorFilter(Color.BLACK)
            } else {
                holder.icon.clearColorFilter()
            }
            holder.icon.setImageDrawable(icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemHolder {
            val view = layoutInflater.inflate(R.layout.item_action_list, parent, false)
            return ListItemHolder(view)
        }

        override fun getItemCount(): Int = actions.size

        override fun getItemId(position: Int): Long = actions[position].id.toLong()

        override fun onGetItemDraggableRange(holder: ListItemHolder, position: Int): ItemDraggableRange? = null

        override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

        override fun onCheckCanStartDrag(holder: ListItemHolder, position: Int, x: Int, y: Int): Boolean {
            return true
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            ignoreNextUpdate = true
            viewModel.moveItem(fromPosition, toPosition)
        }

        override fun onItemDragStarted(position: Int) = Unit

        override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) = Unit
    }

    private inner class ListItemHolder(itemView: View) : AbstractDraggableItemViewHolder(itemView) {
        val icon = itemView.findViewById<ImageView>(R.id.icon)
        val text = itemView.findViewById<TextView>(R.id.text)

        init {
            itemView.setOnClickListener {
                viewModel.editAction(adapterPosition)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EDIT_WINDOW &&
                resultCode == Activity.RESULT_OK &&
                data != null) {
            if (data.getBooleanExtra(ActionEditorActivity.EXTRA_DELETING, false) && lastEditedActionPosition >= 0) {
                viewModel.deleteAction(lastEditedActionPosition)
                lastEditedActionPosition = -1
                return
            }

            val actionBundle = data.getParcelableExtra<PersistableBundle>(
                    ActionEditorActivity.EXTRA_ACTION) ?: return

            val newAction = PhoneAction.deserialize<PhoneAction>(context!!, actionBundle) ?: return

            if (lastEditedActionPosition < 0) {
                viewModel.addAction(newAction)
            } else {
                viewModel.actionEditFinished(newAction, lastEditedActionPosition)
            }

            lastEditedActionPosition = -1
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
