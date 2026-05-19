package com.example.kaktuan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: Button

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

        btnCapture.isEnabled = false // Nonaktifkan tombol sementara
        Toast.makeText(this, "Memproses gambar...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // 1. Ubah gambar ke Base64
                    val base64Image = imageProxyToBase64(image)
                    image.close() // Wajib ditutup agar kamera tidak freeze

                    // 2. Kirim ke Google Cloud Vision
                    sendToCloudVision(base64Image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Gagal memotret: ${exception.message}", exception)
                    btnCapture.isEnabled = true
                }
            }
        )
    }

    private fun imageProxyToBase64(image: ImageProxy): String {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
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
                        Log.d("OCR_RESULT", "Hasil Deteksi: \n$hasilTeks")
                        Toast.makeText(this@ScanActivity, "Berhasil! Cek Logcat.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ScanActivity, "Teks komposisi tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VisionResponse>, t: Throwable) {
                    btnCapture.isEnabled = true
                    Log.e("API_ERROR", t.message ?: "Unknown Error")
                    Toast.makeText(this@ScanActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
