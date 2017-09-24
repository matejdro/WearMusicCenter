package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.VectorDrawable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.databinding.PopupActionPickerBinding

class ActionPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ACTION_BUNDLE = "Action"
        const val VIEW_MODEL_REQUEST_CODE = 7961
        const val EXTRA_DISPLAY_NONE = "DisplayNone"
    }

    private lateinit var viewModel : ActionPickerViewModel
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : ActionsAdapter

    private var oldRecyclerSize = 0

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<PopupActionPickerBinding>(this,
                com.matejdro.wearmusiccenter.R.layout.popup_action_picker)
        binding.activity = this

        val displayNone = intent.getBooleanExtra(EXTRA_DISPLAY_NONE, true)

        val factory = ActionPickerViewModelFactory(
                application,
                displayNone
        )

        viewModel = ViewModelProviders.of(this, factory)[ActionPickerViewModel::class.java]
        viewModel.displayedActions.observe(this, listObserver)
        viewModel.selectedAction.observe(this, pickObserver)
        viewModel.activityStarter.observe(this, activityOpenObserver)

        recycler = binding.recycler
        adapter = ActionsAdapter()
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


        setFinishOnTouchOutside(true)
    }

    private val listObserver = Observer<List<PhoneAction>> {
        if (it == null) {
            return@Observer
        }

        adapter.notifyItemRangeRemoved(0, oldRecyclerSize)
        adapter.notifyItemRangeInserted(0, adapter.itemCount)
        oldRecyclerSize = adapter.itemCount
    }

    private val pickObserver = Observer<PhoneAction> {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_ACTION_BUNDLE, it?.serialize())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private val activityOpenObserver = Observer<Intent> {
        if (it == null) {
            return@Observer
        }

        startActivityForResult(it, VIEW_MODEL_REQUEST_CODE)
    }

    override fun onBackPressed() {
        if (!viewModel.tryGoBack()) {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIEW_MODEL_REQUEST_CODE) {
            viewModel.onActivityResultReceived(requestCode, resultCode, data)
        }
    }

    private inner class ActionsAdapter : RecyclerView.Adapter<ActionsHolder>() {
        override fun getItemCount(): Int {
            return viewModel.displayedActions.value?.size ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ActionsHolder {
            val view = layoutInflater.inflate(R.layout.item_action, parent, false)
            return ActionsHolder(view)
        }

        override fun onBindViewHolder(holder: ActionsHolder, position: Int) {
            val action = viewModel.displayedActions.value?.get(position) ?: return

            val icon = action.getIcon()
            if (icon is VectorDrawable) {
                holder.iconView.setColorFilter(Color.BLACK)
            } else {
                holder.iconView.clearColorFilter()
            }

            holder.textView.text = action.getTitle()
            holder.iconView.setImageDrawable(icon)
        }

    }

    private inner class ActionsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
        val iconView: ImageView = itemView.findViewById(R.id.icon)

        init {
            itemView.setOnClickListener {
                viewModel.onActionTapped(adapterPosition)
            }
        }
    }
}
