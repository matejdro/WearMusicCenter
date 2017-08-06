package com.matejdro.wearmusiccenter.util

import android.os.PersistableBundle
import com.matejdro.wearutils.messages.ParcelPacker
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Class that helps with serializing [PersistableBundle] into files {
 */
object BundleFileSerialization {
    /**
     * Write bundle to file.
     */
    fun writeToFile(bundle: PersistableBundle, file: File) {
        FileOutputStream(file).use {
            val configBundleBytes = ParcelPacker.getData(bundle)

            it.writeInt(configBundleBytes.size)
            it.write(configBundleBytes)
        }
    }

    /**
     * Read bundle from file
     *
     * @param maxSize Maximum size of the file. If file is bigger than that, we assume there is something else (not the bundle) saved in the file and fail.
     */
    fun readFromFile(file: File, maxSize: Int = 10_000_000): PersistableBundle? {
        if (!file.exists()) {
            return null
        }

        FileInputStream(file).use {
            val configSize = it.readInt()

            if (configSize > maxSize) {
                // Treat extraordinary long config as reading error
                Timber.e("Config too large! Non-config stream?")
                return null
            }

            val configData = ByteArray(configSize)
            it.read(configData)

            return ParcelPacker.getParcelable(configData, PersistableBundle.CREATOR)
        }
    }
}