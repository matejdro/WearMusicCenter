package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.VectorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.databinding.PopupActionPickerBinding
import com.matejdro.wearmusiccenter.di.InjectableViewModelFactory
import dagger.Provides
import dagger.android.AndroidInjection
import javax.inject.Inject
import javax.inject.Named

class ActionPickerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ACTION_BUNDLE = "Action"
        const val VIEW_MODEL_REQUEST_CODE = 7961
        const val EXTRA_DISPLAY_NONE = "DisplayNone"
    }

    private val viewModel : ActionPickerViewModel by viewModels { viewModelFactory }
    private lateinit var recycler : RecyclerView
    private lateinit var adapter : ActionsAdapter

    private var displayNone = false

    @Inject
    lateinit var viewModelFactory: InjectableViewModelFactory<ActionPickerViewModel>

    @Inject
    lateinit var customIconStorage: CustomIconStorage

    private var oldRecyclerSize = 0

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        displayNone = intent.getBooleanExtra(EXTRA_DISPLAY_NONE, true)

        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        val binding = PopupActionPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel.displayedActions.observe(this, listObserver)
        viewModel.selectedAction.observe(this, pickObserver)
        viewModel.activityStarter.observe(this, activityOpenObserver)

        recycler = binding.recycler
        adapter = ActionsAdapter()
        recycler.adapter = adapter
        recycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )
        recycler.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        binding.cancelButton.setOnClickListener { finish() }

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

    private val activityOpenObserver = Observer<Intent?> {
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionsHolder {
            val view = layoutInflater.inflate(R.layout.item_action, parent, false)
            return ActionsHolder(view)
        }

        override fun onBindViewHolder(holder: ActionsHolder, position: Int) {
            val action = viewModel.displayedActions.value?.get(position) ?: return

            val icon = customIconStorage[action]
            if (icon is VectorDrawable) {
                holder.iconView.setColorFilter(Color.BLACK)
            } else {
                holder.iconView.clearColorFilter()
            }

            holder.textView.text = action.title
            holder.iconView.setImageDrawable(icon)
        }

    }

    private inner class ActionsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
        val iconView: ImageView = itemView.findViewById(R.id.icon)

        init {
            itemView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                viewModel.onActionTapped(adapterPosition)
            }
        }
    }

    @dagger.Module
    class Module {
        @Provides
        @Named(ActionPickerViewModel.ARG_SHOW_NONE)
        fun displayNone(actionPickerActivity: ActionPickerActivity) = actionPickerActivity.displayNone
    }
}
