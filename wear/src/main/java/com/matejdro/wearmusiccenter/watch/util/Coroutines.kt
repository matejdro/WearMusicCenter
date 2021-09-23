package com.matejdro.wearmusiccenter.watch.util

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearutils.lifecycle.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.tajchert.exceptionwear.ExceptionService
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> CoroutineScope.launchWithErrorHandling(
        androidContext: Context,
        targetResource: MutableLiveData<Resource<T>>,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        block: suspend () -> Unit
) {
    launch(coroutineContext) {
        try {
            block()
        } catch (e: GooglePlayServicesRepairableException) {
            val errorText = androidContext.getString(R.string.error_play_services)
            targetResource.postValue(Resource.error(errorText, null, e))

            Timber.e(e, "Play Services error: %d", e.connectionStatusCode)
        } catch (e: CancellationException) {
            // Just pass cancellation through
            throw e
        } catch (e: Exception) {
            val errorText = androidContext.getString(R.string.error)
            targetResource.postValue(Resource.error(errorText, null, null))

            Timber.e(e, "Unknown error")
            ExceptionService.reportException(androidContext, e)
        }
    }
}
