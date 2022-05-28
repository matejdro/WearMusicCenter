package com.matejdro.wearmusiccenter.actions.playback

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.appcompat.content.res.AppCompatResources
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.actions.ActionHandler
import com.matejdro.wearmusiccenter.actions.PhoneAction
import com.matejdro.wearmusiccenter.actions.SelectableAction
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.actionconfigs.ActionConfigFragment
import com.matejdro.wearmusiccenter.view.actionconfigs.SkipSecondsConfigFragment
import javax.inject.Inject


/**
 * Class name needs to be kept at "thirty seconds" for backwards compatibility
 */
class SkipThirtySecondsAction : SelectableAction {
    constructor(context: Context) : super(context)
    constructor(context: Context, bundle: PersistableBundle) : super(context, bundle) {
        secondsToSkip = bundle.getInt(KEY_SECONDS_TO_SKIP, DEFAULT_SECONDS_TO_SKIP)
    }

    var secondsToSkip: Int = DEFAULT_SECONDS_TO_SKIP

    override fun retrieveTitle(): String = context.getString(R.string.action_skip_seconds, secondsToSkip)
    override val defaultIcon: Drawable
        get() = AppCompatResources.getDrawable(context, R.drawable.action_skip_30_seconds)!!
    override val configFragment: Class<out ActionConfigFragment<out PhoneAction>>
        get() = SkipSecondsConfigFragment::class.java

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)

        bundle.putInt(KEY_SECONDS_TO_SKIP, secondsToSkip)
    }

    class Handler @Inject constructor(private val service: MusicService) : ActionHandler<SkipThirtySecondsAction> {
        override suspend fun handleAction(action: SkipThirtySecondsAction) {
            val currentPos = service.currentMediaController?.playbackState?.position ?: return
            service.currentMediaController?.transportControls?.seekTo(currentPos + action.secondsToSkip * 1_000)
        }
    }

}

private const val KEY_SECONDS_TO_SKIP = "SECONDS_TO_SKIP"
private const val DEFAULT_SECONDS_TO_SKIP = 30
