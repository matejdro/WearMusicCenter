package com.matejdro.wearmusiccenter.view.actionconfigs

import android.widget.EditText
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.playback.ReverseThirtySecondsAction

class ReverseSecondsConfigFragment : ActionConfigFragment<ReverseThirtySecondsAction>(R.layout.fragment_config_skip_seconds) {
    override fun onLoad(action: ReverseThirtySecondsAction) {
        requireView().findViewById<EditText>(R.id.seconds_box).setText(action.secondsToReverse.toString())
    }

    override fun save(action: ReverseThirtySecondsAction) {
        action.secondsToReverse = requireView().findViewById<EditText>(R.id.seconds_box).text.toString().toInt()
    }
}
