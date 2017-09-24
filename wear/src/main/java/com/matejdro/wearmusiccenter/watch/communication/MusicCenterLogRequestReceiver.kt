package com.matejdro.wearmusiccenter.watch.communication

import com.matejdro.wearmusiccenter.common.CommPaths
import com.matejdro.wearutils.logging.WearLogRequestReceiver

class MusicCenterLogRequestReceiver : WearLogRequestReceiver() {
    override fun getLogsChannelPath(): String = CommPaths.CHANNEL_LOGS
}
