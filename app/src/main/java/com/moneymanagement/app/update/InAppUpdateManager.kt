package com.moneymanagement.app.update

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "In-app update download completed.")
                _isUpdateDownloaded.value = true
            }
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                Log.d(TAG, "In-app update downloading: $bytesDownloaded / $totalBytes")
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "In-app update download failed with error code: ${state.installErrorCode()}")
            }
            else -> {
                Log.d(TAG, "In-app update status: ${state.installStatus()}")
            }
        }
    }

    fun registerListener() {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register install listener: ${e.localizedMessage}")
        }
    }

    fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister install listener: ${e.localizedMessage}")
        }
    }

    fun checkAndResumeIfDownloaded() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _isUpdateDownloaded.value = true
            }
        }
    }

    fun checkForUpdate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        isUserInitiated: Boolean = false,
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _isUpdateDownloaded.value = true
                if (isUserInitiated) {
                    Toast.makeText(context, "Update downloaded! Restart to apply.", Toast.LENGTH_SHORT).show()
                }
                return@addOnSuccessListener
            }

            if (availability == UpdateAvailability.UPDATE_AVAILABLE && isFlexibleAllowed) {
                try {
                    val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, launcher, updateOptions)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting in-app update flow", e)
                    if (isUserInitiated) {
                        Toast.makeText(context, "Could not start update: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume in-progress update flow
                try {
                    val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, launcher, updateOptions)
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming update flow", e)
                }
            } else {
                if (isUserInitiated) {
                    Toast.makeText(context, "You are using the latest version!", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to check for app update: ${e.localizedMessage}")
            if (isUserInitiated) {
                Toast.makeText(context, "Could not check for updates right now.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete update", e)
        }
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
