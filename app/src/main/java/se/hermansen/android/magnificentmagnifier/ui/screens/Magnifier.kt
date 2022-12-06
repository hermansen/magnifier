package se.hermansen.android.magnificentmagnifier.ui.screens

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import se.hermansen.android.magnificentmagnifier.R
import se.hermansen.android.magnificentmagnifier.getCameraProvider

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Magnifier(
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberPermissionState(
        CAMERA
    )

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    PermissionRequired(permissionState = permissionState, permissionNotGrantedContent = { NoCameraPermissionGranted(modifier = Modifier.fillMaxSize()) }, permissionNotAvailableContent = { NoCameraPermissionAvailable() }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    this.scaleType = scaleType
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val previewUseCase = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                coroutineScope.launch {
                    val cameraProvider = context.getCameraProvider()
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, previewUseCase
                        )

                        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val scale = camera.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                                camera.cameraControl.setZoomRatio(scale)
                                return true
                            }
                        }
                        val scaleGestureDetector = ScaleGestureDetector(context, listener)
                        previewView.setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                view.performClick()
                            }
                            scaleGestureDetector.onTouchEvent(event)
                            return@setOnTouchListener true
                        }
                    } catch (ex: Exception) {
                        Log.e("Preview", "Binding failed", ex)
                    }
                }
                previewView
            }
        )
    }
}

@Composable
fun NoCameraPermissionGranted(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painter = painterResource(id = R.drawable.ic_baseline_camera_alt_24), contentDescription = "Camera", tint = MaterialTheme.colorScheme.surfaceTint)
    }
}

@Composable
fun NoCameraPermissionAvailable(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No camera")
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        ) {
            Text("Go to settings")
        }
    }
}