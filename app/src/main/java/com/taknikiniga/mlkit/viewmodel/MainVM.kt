package com.taknikiniga.mlkit.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.tflite.client.TfLiteClient
import com.google.android.gms.tflite.java.TfLite
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.taknikiniga.mlkit.status.ModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainVM : ViewModel() {

    val TAG = "MainVM"

    var moduleInstallClient: ModuleInstallClient? = null
    var optionalApiClient: TfLiteClient? = null

    // TfLite Module Install Status
    private var moduleInstallStatus =
        MutableStateFlow<ModuleStatus>(ModuleStatus.hasModuleInstalled(0))
    val moduleInstallState = moduleInstallStatus.asStateFlow()

    private val _processedText = MutableStateFlow<String>("")
    val processedText = _processedText.asStateFlow()


    fun setModuleInstallerClient(context: Context) {
        moduleInstallClient = ModuleInstall.getClient(context)
    }

    fun setTfLite(context: Context) {
        optionalApiClient = TfLite.getClient(context)
    }

    fun checkModuleInDevice() = viewModelScope.launch {

        moduleInstallClient?.let {
            it.areModulesAvailable(optionalApiClient).addOnSuccessListener {

                // Do Something here if module is available

                Log.e(TAG, "Module Already Downloaded")


            }.addOnFailureListener {
                // Sending Urgent Install Request
                Log.e(TAG, "Module Not Found ${it.message}")

                installModuleInDevice()

                // Sending Deferred Install Request
//                sendDeferredInstallRequest()
            }
        }
    }


    // Module Installer Request
    val getModuleInstallRequest = optionalApiClient?.let {
        ModuleInstallRequest.newBuilder().addApi(it).setListener(installStatusListener)
            .build()
    }

    fun installModuleInDevice() = viewModelScope.launch {
        // Emit Downloading Status
        moduleInstallStatus.value =
            ModuleStatus.hasModuleInstalled(ModuleInstallStatusUpdate.InstallState.STATE_DOWNLOADING)

        moduleInstallClient?.let {
            it.installModules(getModuleInstallRequest!!)
                .addOnSuccessListener {
                    moduleInstallStatus.value =
                        ModuleStatus.hasModuleInstalled(ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED)
                }
                .addOnFailureListener {
                    Log.e(TAG, "ModuleErr -> ${it.message}")

                    moduleInstallStatus.value =
                        ModuleStatus.hasModuleInstalled(ModuleInstallStatusUpdate.InstallState.STATE_FAILED)

                }
        }
    }


    fun isTerminateState(@ModuleInstallStatusUpdate.InstallState state: Int): Boolean {
        return state == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED || state == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED || state == ModuleInstallStatusUpdate.InstallState.STATE_FAILED
    }

    val installStatusListener =
        InstallStatusListener { update ->
            update.progressInfo?.let {
                val progress = (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
                moduleInstallStatus.value = ModuleStatus.moduleInstallProgress(progress)

            }

            if (isTerminateState(update.installState)) {
                moduleInstallStatus.value = ModuleStatus.terminated

            }


        }



    //  Create an instance of TextRecognizer

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    // Prepare the input image

    fun imageAnalysis(bitmap: Bitmap) = viewModelScope.launch {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image).addOnSuccessListener {
            it?.let {

                _processedText.value =it.text

            }
        }.addOnFailureListener {
            _processedText.value ="${it.message}"
        }
    }


}