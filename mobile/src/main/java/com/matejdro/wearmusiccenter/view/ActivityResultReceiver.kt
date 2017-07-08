package com.matejdro.wearmusiccenter.view

import android.content.Intent

interface ActivityResultReceiver {
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}