package com.taknikiniga.mlkit

import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.taknikiniga.mlkit.status.ModuleStatus
import com.taknikiniga.mlkit.ui.theme.MlKitTheme
import com.taknikiniga.mlkit.viewmodel.MainVM

class MainActivity : ComponentActivity() {
    lateinit var mainVm: Lazy<MainVM>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MlKitTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    mainVm = viewModels<MainVM>()
                    val scrollState = rememberScrollState()

                    // Setting Module Installer Client
                    mainVm.value.setModuleInstallerClient(this)

                    // Setting TF Lite Client
                    mainVm.value.setTfLite(this)

                    // Checking If Module is available or not
                    mainVm.value.checkModuleInDevice()

                    // Observing Module Install Status

                    mainVm.value.moduleInstallState.collectAsState().apply {
                        when (this.value) {
                            is ModuleStatus.hasModuleInstalled -> {
                                when ((this.value as ModuleStatus.hasModuleInstalled).hasInstalled) {
                                    STATE_COMPLETED -> {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Downloading Successful",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    STATE_FAILED -> {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Downloading Failed... Something Went Wrong",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    }

                                }
                            }

                            is ModuleStatus.moduleInstallProgress -> {
                                ModuleInstallProgress(progress = (this.value as ModuleStatus.moduleInstallProgress).progress.toFloat())
                            }

                            ModuleStatus.terminated -> {
                                mainVm.value.apply {
                                    moduleInstallClient?.unregisterListener(installStatusListener)
                                }
                            }


                        }
                    }


                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val data = mainVm.value.processedText.collectAsState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.90f)

                        ) {
                            Text(
                                text = data.value, modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp).scrollable(scrollState, orientation = Orientation.Vertical)
                            )
                        }

                        BTNImagePicker()
                    }


                }
            }
        }
    }

    @Composable
    fun BTNImagePicker() {
        MlKitTheme {
            Surface(
                shape = MaterialTheme.shapes.medium, shadowElevation = 3.dp,
                onClick = {
                    imagePickerIntent.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pick Image",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }

    }


    @Composable
    fun ModuleInstallProgress(progress: Float) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(progress = progress)
                Spacer(modifier = Modifier.padding(5.dp))
                Text(text = "Please Wait...")
            }
        }
    }

    private val imagePickerIntent =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            if (it != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                mainVm.value.imageAnalysis(bitmap)
                return@registerForActivityResult

            }

            // User Not Picked Any Image
        }


}

