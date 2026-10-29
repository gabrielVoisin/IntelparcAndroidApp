package com.drautomation.intelpark.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Camera preview that reports the first QR code it reads (ML Kit, on the phone). */
@Composable
fun QrScanner(modifier: Modifier, torch: Boolean, onCode: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latest = rememberUpdatedState(onCode)
    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            val executor = ContextCompat.getMainExecutor(context)
            setImageAnalysisAnalyzer(
                executor,
                MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
                    result?.getValue(scanner)?.firstOrNull()?.rawValue?.let { latest.value(it) }
                },
            )
        }
    }
    DisposableEffect(owner) {
        controller.bindToLifecycle(owner)
        onDispose {
            controller.unbind()
            scanner.close()
        }
    }
    LaunchedEffect(torch) { controller.enableTorch(torch) }
    AndroidView(factory = { PreviewView(it).apply { this.controller = controller } }, modifier = modifier)
}

/** Camera preview for the plate photo; returns the controller used to take the picture. */
@Composable
fun rememberPhotoController(): LifecycleCameraController {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) } }
    DisposableEffect(owner) {
        controller.bindToLifecycle(owner)
        onDispose { controller.unbind() }
    }
    return controller
}

@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier) {
    AndroidView(factory = { PreviewView(it).apply { this.controller = controller } }, modifier = modifier)
}

suspend fun takePhoto(context: Context, controller: LifecycleCameraController): File = suspendCancellableCoroutine { cont ->
    val file = File(context.cacheDir, "plate-${System.currentTimeMillis()}.jpg")
    controller.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                cont.resume(file)
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resumeWithException(exception)
            }
        },
    )
}

/** Reads the text of the photo on the phone and keeps what looks like a licence plate. */
suspend fun readPlate(context: Context, photo: File): String? {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val text = recognizer.process(InputImage.fromFilePath(context, Uri.fromFile(photo))).await()
        val plate = Regex("^[A-Z]{1,3}[- ]?[0-9]{1,6}([- ]?[A-Z]{1,3})?$")
        text.textBlocks.flatMap { it.lines }.map { it.text.uppercase().trim() }
            .firstOrNull { plate.matches(it) }
    } catch (e: Exception) {
        null
    } finally {
        recognizer.close()
    }
}

/** Evidence photo: at most 1600 px and JPEG 85 % (the server accepts 3 MB). */
suspend fun shrinkJpeg(photo: File): File = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(photo.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
    val bitmap = BitmapFactory.decodeFile(photo.path, BitmapFactory.Options().apply { inSampleSize = sample })
    val out = File(photo.parentFile, photo.nameWithoutExtension + "-small.jpg")
    FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    bitmap.recycle()
    out
}
