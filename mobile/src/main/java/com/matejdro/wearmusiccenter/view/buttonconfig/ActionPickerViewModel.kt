package com.matejdro.wearmusiccenter.view.buttonconfig

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.RootActionList
import java.util.*

class ActionPickerViewModel(application: Application) : AndroidViewModel(application) {
    val displayedActions = MutableLiveData<List<PhoneAction>>()
    val selectedAction = MutableLiveData<PhoneAction>()
    private val backStack = Stack<List<PhoneAction>>()

    init {
        RootActionList(application).onActionPicked(this)
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

    fun onActionTapped(index : Int) {
        displayedActions.value?.get(index)?.onActionPicked(this)
    }


 }
