package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.PersistableBundle
import androidx.annotation.CallSuper
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.serialization.Bundlable
import timber.log.Timber

abstract class PhoneAction : Bundlable {
    protected val context : Context
    var customIconUri : Uri? = null
    var customTitle: String? = null

    constructor(context : Context) : super() {
        this.context = context
    }
    constructor(context : Context, bundle: PersistableBundle) : super(bundle) {
        this.context = context

        bundle.getString(KEY_CUSTOM_ICON_URI)?.also {
            customIconUri = Uri.parse(it)
        }

        customTitle = bundle.getString(KEY_CUSTOM_TITLE)
    }

    abstract fun onActionPicked(actionPicker: ActionPickerViewModel)
    protected abstract fun retrieveTitle(): String
    abstract val defaultIcon: Drawable

    val title: String
        get() = customTitle ?: retrieveTitle()

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)

        bundle.putString(KEY_CUSTOM_ICON_URI, customIconUri?.toString())
        bundle.putString(KEY_CUSTOM_TITLE, customTitle)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other.javaClass != this.javaClass) return false

        return isEqualToAction(other as PhoneAction)
    }

    override fun hashCode(): Int {
        return 0
    }

    @CallSuper
    protected open fun isEqualToAction(other : PhoneAction) : Boolean {
        return customIconUri == other.customIconUri &&
                customTitle == other.customTitle
    }

    companion object {
        const val KEY_CUSTOM_ICON_URI = "CUSTOM_ICON_URI"
        const val KEY_CUSTOM_TITLE = "CUSTOM_TITLE"

        @Suppress("UNCHECKED_CAST")
        fun <T : PhoneAction> deserialize(context : Context, bundle: PersistableBundle?) : T? {
            val className = bundle?.getString(CLASS_KEY) ?: return null

            return try {
                val cls = Class.forName(className)
                val constructor = cls.getConstructor(Context::class.java, PersistableBundle::class.java)

                constructor.newInstance(context, bundle) as T?
            } catch (e: ReflectiveOperationException) {
                Timber.e(e, "PhoneAction deserialization error")
                null
            }
        }
    }
}
