package com.matejdro.wearmusiccenter.view.actionlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.ActionConfig
import com.matejdro.wearmusiccenter.config.actionlist.ActionList
import com.matejdro.wearmusiccenter.di.LocalActivityConfig
import com.matejdro.wearmusiccenter.util.IdentifiedItem
import com.matejdro.wearutils.lifecycle.SingleLiveEvent
import javax.inject.Inject

class ActionListViewModel @Inject constructor(@param:LocalActivityConfig val actionConfig: ActionConfig) : ViewModel() {
    val actions = MutableLiveData<List<IdentifiedItem<PhoneAction>>>()
    val openActionEditor = SingleLiveEvent<Int?>()
    private var actionStore: MutableList<IdentifiedItem<PhoneAction>>

    private var lastId = 0

    private val actionListConfig: ActionList = actionConfig.getActionList()

    init {

        actionStore = ArrayList(actionListConfig.actions.map(this::itemFromPhoneAction))

        actions.value = actionStore
    }

    fun moveItem(from: Int, to: Int) {
        val item = actionStore.removeAt(from)
        actionStore.add(to, item)

        saveActions()
    }


    fun deleteAction(position: Int) {
        actionStore.removeAt(position)
        saveActions()
    }

    fun actionEditFinished(newAction: PhoneAction, editPosition: Int) {
        actionStore[editPosition].item = newAction
        saveActions()
    }

    fun editAction(position: Int) {
        openActionEditor.value = position
        openActionEditor.value = null
    }

    fun addAction(action: PhoneAction) {
        actionStore.add(itemFromPhoneAction(action))
        saveActions()
    }

    private fun itemFromPhoneAction(action: PhoneAction): IdentifiedItem<PhoneAction>
            = IdentifiedItem(lastId++, action)

    private fun saveActions() {
        actions.value = actionStore

        actionListConfig.actions = actionStore.map(IdentifiedItem<PhoneAction>::item)
        actionListConfig.commit()
    }
}
