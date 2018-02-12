package com.matejdro.wearmusiccenter.view.buttonconfig

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.RootActionList
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import com.matejdro.wearutils.lifecycle.SingleLiveEvent
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class ActionPickerViewModel @Inject constructor(@Named(ARG_SHOW_NONE) showNone: Boolean, context: Context) : ViewModel() {
    val displayedActions = MutableLiveData<List<PhoneAction>>()
    val selectedAction = SingleLiveEvent<PhoneAction>()
    val activityStarter = SingleLiveEvent<Intent>()

    private val backStack = Stack<List<PhoneAction>>()
    private var activityResultReceiver: ActivityResultReceiver? = null

    init {
        RootActionList(context, showNone).onActionPicked(this)
    }

    fun updateDisplayedActionsWithBackStack(actions : List<PhoneAction>) {
        if (displayedActions.value != null) {
            backStack.push(displayedActions.value)
        }

        displayedActions.value = actions
    }

    fun tryGoBack() : Boolean {
        if (backStack.isEmpty()) {
            return false
        }

        displayedActions.value = backStack.pop()

        return true
    }

    fun startActivityForResult(intent: Intent, receiver: ActivityResultReceiver) {
        activityResultReceiver = receiver
        activityStarter.value = intent
    }

    fun onActionTapped(index : Int) {
        displayedActions.value?.get(index)?.onActionPicked(this)
    }

    fun onActivityResultReceived(requestCode: Int, resultCode: Int, data: Intent?) {
        activityStarter.value = null

        activityResultReceiver?.onActivityResult(requestCode, resultCode, data)
        activityResultReceiver = null
    }

    companion object {
        const val ARG_SHOW_NONE = "ShowNone"
    }
}