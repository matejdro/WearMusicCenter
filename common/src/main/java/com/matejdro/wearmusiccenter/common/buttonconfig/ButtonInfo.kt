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
    val buttonCode : Int
    @ButtonGesture val gesture : Int

    constructor(physicalButton: Boolean, buttonCode: Int, @ButtonGesture gesture : Int) : super() {
        this.physicalButton = physicalButton
        this.buttonCode = buttonCode
        this.gesture = gesture
    }

    constructor(bundle: PersistableBundle) : super(bundle) {
        physicalButton = bundle.getInt(KEY_PHYSICAL_BUTTON, 0) == 1
        buttonCode = bundle.getInt(KEY_BUTTON_INDEX, 0)
        gesture = bundle.getInt(KEY_GESTURE, 0)
    }

    constructor(protoButtonInfo: WatchActions.ProtoButtonInfo) {
        physicalButton = protoButtonInfo.physicalButton
        buttonCode = protoButtonInfo.buttonIndex
        gesture = protoButtonInfo.gesture
    }

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)

        bundle.putInt(KEY_PHYSICAL_BUTTON, if (physicalButton) 1 else 0)
        bundle.putInt(KEY_BUTTON_INDEX, buttonCode)
        bundle.putInt(KEY_GESTURE, gesture)
    }

    fun copy(physicalButton: Boolean = this.physicalButton,
             buttonCode: Int = this.buttonCode,
             gesture: Int = this.gesture) = ButtonInfo(physicalButton, buttonCode, gesture)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ButtonInfo

        if (physicalButton != other.physicalButton) return false
        if (buttonCode != other.buttonCode) return false
        if (gesture != other.gesture) return false

        return true
    }

    fun getKey() : String {
        return "$physicalButton.$buttonCode.$gesture"
    }

    fun getLegacyButtonInfo() : ButtonInfo {
        return if (buttonCode in KEYCODE_STEM_1..KEYCODE_STEM_3) {
            val buttonIndex = buttonCode - KEYCODE_STEM_1
            copy(buttonCode = buttonIndex)
        } else {
            this
        }
    }

    fun buildProtoVersion() : WatchActions.ProtoButtonInfo.Builder = WatchActions.ProtoButtonInfo
            .newBuilder()
            .setPhysicalButton(physicalButton)
            .setButtonIndex(buttonCode)
            .setGesture(gesture)

    override fun hashCode(): Int {
        var result = physicalButton.hashCode()
        result = 31 * result + buttonCode
        result = 31 * result + gesture
        return result
    }

    override fun toString(): String {
        return "ButtonInfo(physicalButton=$physicalButton, buttonCode=$buttonCode, gesture=$gesture)"
    }


}

// Copied from KeyEvent file for pre-N compatibility

private const val KEYCODE_STEM_1 = 265
private const val KEYCODE_STEM_2 = 266
private const val KEYCODE_STEM_3 = 267