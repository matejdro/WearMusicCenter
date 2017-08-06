package com.matejdro.wearmusiccenter.view.actionlist

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.playback.PauseAction
import com.matejdro.wearmusiccenter.actions.playback.PlayAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToNextAction
import com.matejdro.wearmusiccenter.actions.playback.SkipToPrevAction
import com.matejdro.wearmusiccenter.actions.volume.DecreaseVolumeAction
import com.matejdro.wearmusiccenter.actions.volume.IncreaseVolumeAction
import com.matejdro.wearmusiccenter.di.ConfigActivityComponent
import com.matejdro.wearmusiccenter.util.IdentifiedItem

class ActionListViewModel(private val configActivityComponent: ConfigActivityComponent) : ViewModel() {
    val actions = MutableLiveData<List<IdentifiedItem<PhoneAction>>>()
    private var actionStore: MutableList<IdentifiedItem<PhoneAction>> = arrayListOf()

    private var lastId = 0

    init {
        val context = WearMusicCenter.getAppComponent().provideContext()
        // Mocked data. TODO: add actual storage

        addItemInternal(PlayAction(context))
        addItemInternal(PauseAction(context))
        addItemInternal(SkipToNextAction(context))
        addItemInternal(SkipToPrevAction(context))
        addItemInternal(IncreaseVolumeAction(context))
        addItemInternal(DecreaseVolumeAction(context))

        actions.value = actionStore
    }

    fun moveItem(from: Int, to: Int) {
        val item = actionStore.removeAt(from)
        actionStore.add(to, item)

        actions.value = actionStore
    }

    private fun addItemInternal(action: PhoneAction) {
        actionStore.add(IdentifiedItem(lastId++, action))
    }
}

class ActionListViewModelFactory(private val configActivityComponent: ConfigActivityComponent) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>?): T = ActionListViewModel(configActivityComponent) as T
}