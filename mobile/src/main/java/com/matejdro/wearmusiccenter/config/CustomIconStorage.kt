package com.matejdro.wearmusiccenter.config

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.util.ArraySet
import android.util.LruCache
import com.matejdro.wearmusiccenter.di.GlobalConfig
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject
class CustomIconStorage @Inject constructor(val context: Context,
                                            @GlobalConfig val config : ActionConfigProvider) {

    companion object {
        private val UNSAFE_CHARACTERS_PATTERN = Pattern.compile("[^\\w.\\-_]")

        private const val PREF_NAME = "custom_icon_storage"
        private const val PREF_KEY_NUM_SAVES = "num_saves"

        private const val GC_SAVES_THRESHOLD = 1

        private const val MAX_MEMORY_LRU_STORE_SIZE_BYTES = 5_000_000

    }

    val storageFolder = File(context.filesDir, "icon_store")
    val metaPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val memoryIconStore = object : LruCache<Uri, Bitmap>(MAX_MEMORY_LRU_STORE_SIZE_BYTES) {
        override fun sizeOf(key: Uri, value: Bitmap): Int {
            return value.byteCount
        }
    }

    init {
        storageFolder.mkdirs()
    }

    fun getIcon(iconUri : Uri) : Drawable? {
        var bitmap = memoryIconStore[iconUri]
        if (bitmap == null) {
            bitmap = loadImageFromFile(iconUri) ?: return null
            memoryIconStore.put(iconUri, bitmap)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun loadImageFromFile(uri : Uri) : Bitmap? {
        val file = getFileForUri(uri)
        if (!file.exists()) {
            return null
        }

        val bitmap : Bitmap
        try {
            bitmap = FileInputStream(file).use<FileInputStream, Bitmap> {
                return BitmapFactory.decodeStream(it)
            }
        } catch (e : IOException) {
            Timber.e(e, "Image loading error")
            return null
        }

        return bitmap
    }

    fun setIcon(iconUri: Uri, icon : Bitmap) {
        incrementSets()

        memoryIconStore.put(iconUri, icon)

        try {
            FileOutputStream(getFileForUri(iconUri)).use {
                icon.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e : IOException) {
            Timber.e(e, "Image saving error")
        }
    }

    private fun incrementSets() {
        var numSets = metaPreferences.getInt(PREF_KEY_NUM_SAVES, 0)
        if (++numSets > GC_SAVES_THRESHOLD) {
            gc()
            numSets = 0
        }

        metaPreferences.edit().putInt(PREF_KEY_NUM_SAVES, numSets).apply()
    }

    private fun gc() {
        val utilizedIconUris = ArraySet<String>()

        utilizedIconUris.addAll(getAllCustomUriFiles(config.getPlayingConfig()))
        utilizedIconUris.addAll(getAllCustomUriFiles(config.getStoppedConfig()))
        //TODO action list config

        storageFolder
                .listFiles()
                .filter { !utilizedIconUris.contains(it.name) }
                .forEach { it.delete() }

    }

    private fun getAllCustomUriFiles(config : ActionConfigStorage) : Collection<String> {
        return config.getAllActions()
                .map { it.value.customIconUri }
                .filterNotNull()
                .map { getFileForUri(it).name }
    }

    private fun getFileForUri(uri : Uri) : File {
        var fileName = uri.toString()
        fileName = UNSAFE_CHARACTERS_PATTERN.matcher(fileName).replaceAll("")
        fileName += ".png"

        return File(storageFolder, fileName)
    }
}

