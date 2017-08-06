package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import android.support.annotation.CallSuper
import com.matejdro.wearmusiccenter.WearMusicCenter
import com.matejdro.wearmusiccenter.config.CustomIconStorage
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.serialization.Bundlable
import timber.log.Timber
import javax.inject.Inject

abstract class PhoneAction : Bundlable {
    protected val context : Context
    var customIconUri : Uri? = null
    var customTitle: String? = null

    @Inject
    protected lateinit var customIconStorage : CustomIconStorage

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

    init {
        @Suppress("LeakingThis")
        WearMusicCenter.getAppComponent().inject(this)
    }

    abstract fun execute(service : MusicService)
    abstract fun onActionPicked(actionPicker : ActionPickerViewModel)
    abstract protected fun retrieveTitle(): String
    abstract protected fun retrieveIcon(): Drawable

    fun getTitle(): String {
        return customTitle ?: retrieveTitle()
    }

    fun getIcon() : Drawable {
        val customIconUri = customIconUri ?: return retrieveIcon()
        val newIcon = customIconStorage.getIcon(customIconUri)

        if (newIcon == null) {
            this.customIconUri = null
            return retrieveIcon()
        }

        return newIcon
    }

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
            val className = bundle?.getString(Bundlable.CLASS_KEY) ?: return null

            try {
                val cls = Class.forName(className)
                val constructor = cls.getConstructor(Context::class.java, PersistableBundle::class.java)

                return constructor.newInstance(context, bundle) as T?
            } catch(e: ReflectiveOperationException) {
                Timber.e(e, "PhoneAction deserialization error")
                return null
            }
        }
    }

    class test(val a : Int) : Parcelable {
        companion object {
            @JvmField val CREATOR: Parcelable.Creator<test> = object : Parcelable.Creator<test> {
                override fun createFromParcel(source: Parcel): test = test(source)
                override fun newArray(size: Int): Array<test?> = arrayOfNulls(size)
            }
        }

        constructor(source: Parcel) : this(
        source.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(a)
        }
    }


}