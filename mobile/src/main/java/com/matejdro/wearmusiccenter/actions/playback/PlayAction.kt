package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.PersistableBundle
import android.view.KeyEvent
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.SelectableAction
import com.matejdro.wearmusiccenter.music.MusicService
import javax.inject.Inject

class PlayAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun retrieveTitle(): String = context.getString(R.string.action_play)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.action_play)!!

    class Handler @Inject constructor(private val service: MusicService) : ActionHandler<PlayAction> {
        override suspend fun handleAction(action: PlayAction) {
            val mediaController = service.currentMediaController

            if (mediaController != null) {
                mediaController.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                mediaController.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                return
            }

            //Media controller is null. Lets try to send play button to previously started service

            val audioService = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            audioService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            audioService.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        }
    }
}
