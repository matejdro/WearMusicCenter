package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.PersistableBundle
import android.view.KeyEvent
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.SelectableAction

class PlayAction : SelectableAction {
    constructor(context : Context) : super(context)
    constructor(context : Context, bundle: PersistableBundle) : super(context, bundle)

    override fun execute(service: MusicService) {
        val mediaController = service.currentMediaController

        if (mediaController != null) {
            mediaController.transportControls.play()
            return
        }

        //Media controller is null. Lets try to send play button to previously started service

        val audioService = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audioService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    override fun getName(): String = context.getString(R.string.action_play)
    override fun retrieveIcon(): Drawable = context.getDrawable(R.drawable.action_play)
}