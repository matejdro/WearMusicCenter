package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.SelectableAction
import com.matejdro.wearmusiccenter.music.MusicService
import javax.inject.Inject

class SkipThirtySecondsAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle)

    override fun retrieveTitle(): String = context.getString(R.string.action_skip_30_seconds)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.action_skip_30_seconds)!!

    class Handler @Inject constructor(private val service: MusicService) : ActionHandler<SkipThirtySecondsAction> {
        override suspend fun handleAction(action: SkipThirtySecondsAction) {
            val currentPos = service.currentMediaController?.playbackState?.position ?: return
            service.currentMediaController?.transportControls?.seekTo(currentPos + 30_000)
        }
    }

}
