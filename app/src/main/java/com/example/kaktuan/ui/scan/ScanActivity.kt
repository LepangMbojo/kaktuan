package com.example.kaktuan.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.kaktuan.api.AnnotateRequest
import com.example.kaktuan.BuildConfig
import com.example.kaktuan.api.Feature
import com.example.kaktuan.api.ImageSource
import com.example.kaktuan.R
import com.example.kaktuan.api.RetrofitClient
import com.example.kaktuan.api.VisionRequest
import com.example.kaktuan.api.VisionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.kaktuan.logic.OCRTextCleaner
import android.widget.TextView
import android.view.View
import android.widget.ProgressBar
import android.graphics.Bitmap
import java.io.File
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.kaktuan.api.GeminiRequest
import com.example.kaktuan.api.GeminiResponse
import com.example.kaktuan.api.GeminiRetrofitClient
import com.example.kaktuan.api.Content
import com.example.kaktuan.api.Part
import com.example.kaktuan.logic.GeminiParser

class ScanActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var scanFrame: View
    private lateinit var imgCaptured: ImageView
    private lateinit var btnRetake: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Aplikasi butuh izin kamera untuk memindai komposisi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)
        scanFrame = findViewById(R.id.scanFrame)
        imgCaptured = findViewById(R.id.imgCaptured)
        btnRetake = findViewById(R.id.btnRetake)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Memanggil fungsi jepret gambar saat tombol ditekan
        btnCapture.setOnClickListener {
            takePhoto()
        }

        btnRetake.setOnClickListener {

            imgCaptured.visibility = View.GONE

            viewFinder.visibility = View.VISIBLE

            tvResult.text = ""

            tvResult.visibility = View.GONE

            btnRetake.visibility = View.GONE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch(exc: Exception) {
                Log.e("ScanActivity", "Kamera gagal dimuat", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return

        progressBar.visibility = View.VISIBLE
        tvResult.visibility = View.GONE
        btnCapture.isEnabled = false

        val photoFile = File(
            cacheDir,
            "scan.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile)
                .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {

                    val bitmap =
                        BitmapFactory.decodeFile(
                            photoFile.absolutePath
                        )

                    val croppedBitmap =
                        cropBitmapCenter(bitmap)

                    runOnUiThread {

                        imgCaptured.visibility = View.VISIBLE

                        imgCaptured.setImageBitmap(
                            croppedBitmap
                        )

                        viewFinder.visibility = View.GONE
                        btnRetake.visibility = View.VISIBLE
                    }

                    val base64Image =
                        bitmapToBase64(croppedBitmap)

                    sendToCloudVision(base64Image)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    progressBar.visibility = View.GONE

                    btnCapture.isEnabled = true

                    Log.e(
                        "CameraX",
                        exception.message ?: ""
                    )
                }
            }
        )
    }

    private fun sendToCloudVision(base64Image: String) {
        val request = VisionRequest(
            listOf(AnnotateRequest(ImageSource(base64Image), listOf(Feature())))
        )

        val apiKey = BuildConfig.VISION_API_KEY

        RetrofitClient.instance.analyzeImage(apiKey, request)
            .enqueue(object : Callback<VisionResponse> {

                override fun onResponse(call: Call<VisionResponse>, response: Response<VisionResponse>) {
                    btnCapture.isEnabled = true

                    Log.w("API_STATUS", "Server merespons dengan kode: ${response.code()}")

                    val hasilTeks = response.body()?.responses?.get(0)?.fullTextAnnotation?.text

                    if (hasilTeks != null) {

                        val cleanedText =
                            OCRTextCleaner.clean(hasilTeks)

                        sendToGemini(cleanedText)

                    } else {

                        progressBar.visibility = View.GONE

                        btnCapture.isEnabled = true

                        tvResult.visibility = View.VISIBLE

                        tvResult.text = "Teks tidak ditemukan"
                    }

                }

                override fun onFailure(
                    call: Call<VisionResponse>,
                    t: Throwable
                ) {

                    progressBar.visibility = View.GONE

                    btnCapture.isEnabled = true

                    tvResult.visibility = View.VISIBLE

                    tvResult.text =
                        "Gagal menghubungi server"

                    Log.e(
                        "API_ERROR",
                        t.message ?: "Unknown Error"
                    )
                }
            })
    }
    private fun sendToGemini(
        ocrText: String
    ) {
        Log.d(
            "GEMINI_KEY",
            BuildConfig.GEMINI_API_KEY
        )
        val limitedText =
            ocrText.take(1500)

        val prompt =
            GeminiParser.buildPrompt(
                ocrText
            )

        val request =
            GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(prompt)
                        )
                    )
                )
            )

        GeminiRetrofitClient.instance
            .generateContent(
                BuildConfig.GEMINI_API_KEY,
                request
            )
            .enqueue(
                object : Callback<GeminiResponse> {

                    override fun onResponse(
                        call: Call<GeminiResponse>,
                        response: Response<GeminiResponse>
                    ) {

                        Log.d(
                            "GEMINI_CODE",
                            response.code().toString()
                        )

                        Log.d(
                            "GEMINI_ERROR_BODY",
                            response.errorBody()?.string() ?: "NO_ERROR"
                        )

                        progressBar.visibility =
                            View.GONE

                        val result =
                            response.body()
                                ?.candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text

                        Log.d(
                            "GEMINI_JSON",
                            result ?: "NULL"
                        )

                        tvResult.visibility =
                            View.VISIBLE

                        tvResult.text =
                            result ?: "Gemini tidak mengembalikan data"
                    }

                    override fun onFailure(
                        call: Call<GeminiResponse>,
                        t: Throwable
                    ) {

                        progressBar.visibility =
                            View.GONE

                        tvResult.visibility =
                            View.VISIBLE

                        tvResult.text =
                            t.message ?: "Unknown Error"

                        Log.e(
                            "GEMINI_ERROR",
                            "FULL ERROR",
                            t
                        )
                    }

                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    private fun cropBitmapCenter(
        bitmap: Bitmap
    ): Bitmap {

        val cropWidth =
            (bitmap.width * 0.75).toInt()

        val cropHeight =
            (cropWidth / 1.45).toInt()

        val left =
            (bitmap.width - cropWidth) / 2

        val top =
            (bitmap.height - cropHeight) / 2

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            cropWidth,
            cropHeight
        )
    }
    private fun bitmapToBase64(
        bitmap: Bitmap
    ): String {

        val stream =
            ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            90,
            stream
        )

        val bytes =
            stream.toByteArray()

        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP
        )
    }

}