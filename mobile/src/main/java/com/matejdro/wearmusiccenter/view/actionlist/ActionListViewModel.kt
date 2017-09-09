package com.matejdro.wearmusiccenter.view.actionlist

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.config.ActionConfigProvider
import com.matejdro.wearmusiccenter.config.actionlist.ActionListStorage
import com.matejdro.wearmusiccenter.di.ConfigActivityComponent
import com.matejdro.wearmusiccenter.di.LocalActivityConfig
import com.matejdro.wearmusiccenter.util.IdentifiedItem
import com.matejdro.wearutils.lifecycle.SingleLiveEvent
import javax.inject.Inject

class ActionListViewModel(configActivityComponent: ConfigActivityComponent) : ViewModel() {
    val actions = MutableLiveData<List<IdentifiedItem<PhoneAction>>>()
    val openActionEditor = SingleLiveEvent<Int>()
    private var actionStore: MutableList<IdentifiedItem<PhoneAction>>

    private var lastId = 0
    private var lastEditedActionPosition = -1

    @Inject
    @field:LocalActivityConfig
    internal lateinit var actionConfigProvider: ActionConfigProvider

    private val actionListConfig: ActionListStorage

    init {
        configActivityComponent.inject(this)
        actionListConfig = actionConfigProvider.getActionList()

        actionStore = ArrayList(actionListConfig.actions.map(this::itemFromPhoneAction))

        actions.value = actionStore
    }

    fun moveItem(from: Int, to: Int) {
        val item = actionStore.removeAt(from)
        actionStore.add(to, item)

        saveActions()
    }


    fun deleteLastEditedAction() {
        if (lastEditedActionPosition == -1) {
            return
        }

        actionStore.removeAt(lastEditedActionPosition)
        lastEditedActionPosition = -1
        saveActions()
    }

    fun actionEditFinished(newAction: PhoneAction) {
        if (lastEditedActionPosition == -1) {
            addAction(newAction)
        } else {
            actionStore[lastEditedActionPosition].item = newAction
        }
        lastEditedActionPosition = -1

        saveActions()
    }

    fun editAction(position: Int) {
        lastEditedActionPosition = position

        openActionEditor.value = position
        openActionEditor.value = null
    }

    private fun addAction(action: PhoneAction) {
        actionStore.add(itemFromPhoneAction(action))
    }

    private fun itemFromPhoneAction(action: PhoneAction): IdentifiedItem<PhoneAction>
            = IdentifiedItem(lastId++, action)

    private fun saveActions() {
        actions.value = actionStore

        actionListConfig.actions = actionStore.map(IdentifiedItem<PhoneAction>::item)
        actionListConfig.commit()
    }
}

class ActionListViewModelFactory(private val configActivityComponent: ConfigActivityComponent) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>?): T = ActionListViewModel(configActivityComponent) as T
}