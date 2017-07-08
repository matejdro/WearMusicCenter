package com.matejdro.wearmusiccenter.common.buttonconfig

import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.proto.WatchActions
import com.matejdro.wearutils.serialization.Bundlable

class ButtonInfo : Bundlable {
    companion object {
        const val KEY_PHYSICAL_BUTTON = "PHYSICAL_BUTTON"
        const val KEY_BUTTON_INDEX = "BUTTON_INDEX"
        const val KEY_GESTURE = "BUTTON_GESTURE"
    }

    val physicalButton : Boolean
    val buttonIndex : Int
    @ButtonGesture val gesture : Int

    constructor(physicalButton: Boolean, buttonIndex: Int, @ButtonGesture gesture : Int) : super() {
        this.physicalButton = physicalButton
        this.buttonIndex = buttonIndex
        this.gesture = gesture
    }

    constructor(bundle: PersistableBundle) : super(bundle) {
        physicalButton = bundle.getInt(KEY_PHYSICAL_BUTTON, 0) == 1
        buttonIndex = bundle.getInt(KEY_BUTTON_INDEX, 0)
        gesture = bundle.getInt(KEY_GESTURE, 0)
    }

    constructor(protoButtonInfo: WatchActions.ProtoButtonInfo) {
        physicalButton = protoButtonInfo.physicalButton
        buttonIndex = protoButtonInfo.buttonIndex
        gesture = protoButtonInfo.gesture
    }

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)

        bundle.putInt(KEY_PHYSICAL_BUTTON, if (physicalButton) 1 else 0)
        bundle.putInt(KEY_BUTTON_INDEX, buttonIndex)
        bundle.putInt(KEY_GESTURE, 0)
    }

    fun copy(physicalButton: Boolean = this.physicalButton,
             buttonIndex: Int = this.buttonIndex,
             gesture: Int = this.gesture) = ButtonInfo(physicalButton, buttonIndex, gesture)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ButtonInfo

        if (physicalButton != other.physicalButton) return false
        if (buttonIndex != other.buttonIndex) return false
        if (gesture != other.gesture) return false

        return true
    }

    fun getKey() : String {
        return "$physicalButton.$buttonIndex.$gesture"
    }

    fun buildProtoVersion() : WatchActions.ProtoButtonInfo.Builder = WatchActions.ProtoButtonInfo
            .newBuilder()
            .setPhysicalButton(physicalButton)
            .setButtonIndex(buttonIndex)
            .setGesture(gesture)

    override fun hashCode(): Int {
        var result = physicalButton.hashCode()
        result = 31 * result + buttonIndex
        result = 31 * result + gesture
        return result
    }

    override fun toString(): String {
        return "ButtonInfo(physicalButton=$physicalButton, buttonIndex=$buttonIndex, gesture=$gesture)"
    }


}
