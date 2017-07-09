package com.matejdro.wearmusiccenter.actions

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import com.matejdro.wearmusiccenter.music.MusicService
import com.matejdro.wearmusiccenter.view.buttonconfig.ActionPickerViewModel
import com.matejdro.wearutils.serialization.Bundlable
import timber.log.Timber

abstract class PhoneAction : Bundlable {
    protected val context : Context

    constructor(context : Context) : super() {
        this.context = context
    }
    constructor(context : Context, bundle: PersistableBundle) : super(bundle) {
        this.context = context
    }

    abstract fun execute(service : MusicService)
    abstract fun onActionPicked(actionPicker : ActionPickerViewModel)
    abstract fun getName() : String
    abstract fun getIcon() : Drawable

    override fun writeToBundle(bundle: PersistableBundle) {
        super.writeToBundle(bundle)
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

    protected open fun isEqualToAction(other : PhoneAction) : Boolean {
        return true
    }

    companion object {
        const val FIELD_ICON_BITMAP = "Icon"

        @Suppress("UNCHECKED_CAST")
        fun <T : PhoneAction> deserialize(context : Context, bundle: PersistableBundle?) : T? {
            val className = bundle?.getString(Bundlable.CLASS_KEY) ?: return null

            try {
                val cls = Class.forName(className)
                val constructor = cls.getConstructor(Context::class.java, PersistableBundle::class.java)

                return constructor.newInstance(context, bundle) as T?
            } catch(e: ReflectiveOperationException) {
                Timber.e(e, "PhoneAction deserialization error")
                e.printStackTrace()
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