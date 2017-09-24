package com.matejdro.wearmusiccenter.common

interface CommPaths {
    companion object {
        const val PHONE_APP_CAPABILITY = "MusicCenterPhone"
        const val WATCH_APP_CAPABILITY = "MusicCenterWatch"

        const val DATA_MUSIC_STATE = "/Music/State"
        const val DATA_WATCH_INFO = "/WatchInfo"
        const val ASSET_WATCH_INFO_BUTTON_PREFIX = "/WatchInfo/Button"

        const val MESSAGES_PREFIX = "wear://*/Messages/"
        const val MESSAGE_WATCH_OPENED = "/Messages/WatchOpened"
        const val MESSAGE_WATCH_CLOSED = "/Messages/WatchClosed"
        const val MESSAGE_ACK = "/Messages/ACK"
        const val MESSAGE_CHANGE_VOLUME = "/Messages/SetVolume"
        const val MESSAGE_EXECUTE_ACTION = "/Messages/Action"
        const val MESSAGE_EXECUTE_MENU_ACTION = "/Messages/MenuAction"
        const val MESSAGE_SEND_LOGS = "/SendLogs"

        const val CHANNEL_LOGS = "/Channel/Logs"

        const val ASSET_ALBUM_ART = "AlbumArt"

        const val DATA_ACTION_CONFIG_PREFIX = "/Actions"
        const val DATA_LIST_ITEMS = "/ActionList"

        const val DATA_PLAYING_ACTION_CONFIG = DATA_ACTION_CONFIG_PREFIX + "/Playback"
        const val DATA_STOPPING_ACTION_CONFIG = DATA_ACTION_CONFIG_PREFIX + "/Stopped"
        const val ASSET_BUTTON_ICON_PREFIX = "/Button_Icon_"

        const val PREFERENCES_PREFIX = "/Settings"
    }
}
