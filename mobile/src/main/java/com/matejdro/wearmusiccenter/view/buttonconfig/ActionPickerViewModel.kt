package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.RootActionList
import com.matejdro.wearmusiccenter.view.ActivityResultReceiver
import java.util.*

class ActionPickerViewModel(showNone: Boolean, application: Application) : AndroidViewModel(application) {
    val displayedActions = MutableLiveData<List<PhoneAction>>()
    val selectedAction = MutableLiveData<PhoneAction>()
    val activityStarter = MutableLiveData<Intent>()

    private val backStack = Stack<List<PhoneAction>>()
    private var activityResultReceiver: ActivityResultReceiver? = null

    init {
        RootActionList(getApplication(), showNone).onActionPicked(this)
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
 }

class ActionPickerViewModelFactory(private val application: Application, private val showNone: Boolean) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>?): T
            = ActionPickerViewModel(showNone, application) as T

}
