package uz.androbeck.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.R
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import uz.androbeck.scan.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private var camera: Camera? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val REGEX_OLD_PASSPORT = "(?<documentNumber>[A-Z0-9<]{9})(?<checkDigitDocumentNumber>[0-9ILDSOG]{1})(?<nationality>[A-Z<]{3})(?<dateOfBirth>[0-9ILDSOG]{6})(?<checkDigitDateOfBirth>[0-9ILDSOG]{1})(?<sex>[FM<]){1}(?<expirationDate>[0-9ILDSOG]{6})(?<checkDigitExpiration>[0-9ILDSOG]{1})"

    private val LAST_NAME = "[A-Z]"
    private val PASSPORT_TD_3_LINE_2_REGEX =
        "[A-Z]{2}[0-9]{7}([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        checkPermission()
    }

    private fun checkPermission() {
        val permissions: Array<String> = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        Permissions.check(this, permissions, null, null, object : PermissionHandler() {
            override fun onGranted() {
                startCamera()
            }

            override fun onDenied(context: Context?, deniedPermissions: ArrayList<String>?) {
                super.onDenied(context, deniedPermissions)
                Toast.makeText(this@MainActivity, "Denied", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startCamera() {
        try {
            println("::scan -> start camera")
            cameraProviderFuture.addListener(
                {
                    val preview = Preview.Builder().build()
                        .also { it.setSurfaceProvider(binding.cameraPreviewView.surfaceProvider) }

                    try {
                        cameraProviderFuture.get().bind(preview, imageAnalyzer)
                    } catch (e: Exception) {
                        println("::exception -> $e")
                    }
                }, ContextCompat.getMainExecutor(this)
            )
        } catch (e: Exception) {

        }
    }

    private fun ProcessCameraProvider.bind(
        preview: Preview, imageAnalyzer: ImageAnalysis
    ) = try {
        println("::scan -> bin ----")

        unbindAll()
//        imageCapture = ImageCapture.Builder()
//            .setTargetRotation(requireView().display.rotation)
//            .build()
        camera = bindToLifecycle(
            this@MainActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
        )
    } catch (ise: IllegalStateException) {
        // Thrown if binding is not done from the main thread

    }

    private val imageAnalyzer by lazy {
        println("::scan -> image analizer")

        ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also {
            it.setAnalyzer(
                cameraExecutor, TextReaderAnalyzer(::onTextFound)
            )
        }
    }

    private class TextReaderAnalyzer(
        private val textFoundListener: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        init {
            println("::scan -> TextReaderAnalyzer")
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            if (imageProxy.image == null) {
                println("::scan -> image == null ")
                return
            }
            imageProxy.image?.let {
                imageToText(
                    imageProxy, InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
                )
            }
        }

        @SuppressLint("UnsafeOptInUsageError")
        private fun imageToText(imageProxy: ImageProxy, image: InputImage) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image).addOnSuccessListener {
                println("::text -> ${it.text}")
                for (block in it.textBlocks){
                    val blockText = block.text
                    println("::block text -> $blockText")
                }
                textFoundListener.invoke(it.text)
                imageProxy.close()
            }.addOnCanceledListener {}
        }
    }

    private fun onTextFound(sb: String) {
        println("::scan -> onTextFound")
        a
        Log.d("AAAAAAA", sb)
        try {
            val passportPan = Regex(PASSPORT_TD_3_LINE_2_REGEX).find(sb)?.value
            val lastName = Regex(LAST_NAME).find(sb)?.value
            println("::scan last name -> $lastName")
            if (!passportPan.isNullOrEmpty()) {
                println("::scan passport pan -> $passportPan")
                cameraExecutor.apply {
                    shutdown()
                }
            }

        } catch (e: Exception) {

        }
    }
}