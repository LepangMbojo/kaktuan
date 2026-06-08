package com.example.kaktuan.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.kaktuan.BuildConfig
import com.example.kaktuan.R
import com.example.kaktuan.api.AnnotateRequest
import com.example.kaktuan.api.Content
import com.example.kaktuan.api.Feature
import com.example.kaktuan.api.GeminiRequest
import com.example.kaktuan.api.GeminiResponse
import com.example.kaktuan.api.GeminiRetrofitClient
import com.example.kaktuan.api.ImageSource
import com.example.kaktuan.api.Part
import com.example.kaktuan.api.RetrofitClient
import com.example.kaktuan.api.VisionRequest
import com.example.kaktuan.api.VisionResponse
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.example.kaktuan.logic.OCRTextCleaner
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    // Inisialisasi Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreHelper: FirestoreHelper

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

        // Setup Firebase
        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

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

        val photoFile = File(cacheDir, "scan.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val croppedBitmap = cropBitmapCenter(bitmap)

                    runOnUiThread {
                        imgCaptured.visibility = View.VISIBLE
                        imgCaptured.setImageBitmap(croppedBitmap)
                        viewFinder.visibility = View.GONE
                        btnRetake.visibility = View.VISIBLE
                    }

                    val base64Image = bitmapToBase64(croppedBitmap)
                    sendToCloudVision(base64Image)
                }

                override fun onError(exception: ImageCaptureException) {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    Log.e("CameraX", exception.message ?: "")
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
                    Log.w("API_STATUS", "Server merespons dengan kode: ${response.code()}")
                    val hasilTeks = response.body()?.responses?.get(0)?.fullTextAnnotation?.text

                    if (hasilTeks != null) {
                        val cleanedText = OCRTextCleaner.clean(hasilTeks)
                        // LOMPAT KE FUNGSI PENARIKAN DATA FIRESTORE DULU
                        ambilDataPenyakitLaluKeGemini(cleanedText)
                    } else {
                        progressBar.visibility = View.GONE
                        btnCapture.isEnabled = true
                        tvResult.visibility = View.VISIBLE
                        tvResult.text = "Teks tidak ditemukan"
                    }
                }

                override fun onFailure(call: Call<VisionResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    tvResult.visibility = View.VISIBLE
                    tvResult.text = "Gagal menghubungi server"
                    Log.e("API_ERROR", t.message ?: "Unknown Error")
                }
            })
    }

    private fun ambilDataPenyakitLaluKeGemini(ocrText: String) {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestoreHelper.getUserProfile(uid) { user ->
                // 1. Ambil seluruh data yang dibutuhkan dari Firestore user
                val penyakitPasien = user?.healthConditions ?: emptyList()
                val umurPasien = user?.age       // Sesuaikan dengan properti di data class User Anda
                val tinggiPasien = user?.height // Sesuaikan dengan properti di data class User Anda
                val beratPasien = user?.weight
                val genderPasien = user?.gender
                // 2. Oper semua data ke buildPrompt yang baru
                val promptFinal = com.example.kaktuan.logic.GeminiParser.buildPrompt(
                    ocrText,
                    penyakitPasien,
                    umurPasien,
                    tinggiPasien,
                    beratPasien,
                    genderPasien
                )

                // 3. Kirim ke Gemini
                sendToGemini(promptFinal)
            }
        } else {
            // Jika user tidak ditemukan/belum login, kirim data kosong agar analisis bersifat umum
            val promptFinal = com.example.kaktuan.logic.GeminiParser.buildPrompt(
                ocrText,
                emptyList(),
                null,
                null,
                null,
                null
            )
            sendToGemini(promptFinal)
        }
    }

    // ==========================================
    // FUNGSI GEMINI YANG SUDAH DISESUAIKAN
    // ==========================================
    private fun sendToGemini(promptFinal: String) {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(promptFinal)
                    )
                )
            )
        )

        GeminiRetrofitClient.instance
            .generateContent(BuildConfig.GEMINI_API_KEY, request)
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true // Nyalakan tombol lagi di sini

                    val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    Log.d("HASIL_GEMINI", "Balasan dari AI:\n$result")
                    tvResult.visibility = View.VISIBLE

                    // Untuk sementara kita tampilkan JSON mentahnya dulu di layar
                    tvResult.text = result ?: "Gemini tidak mengembalikan data"
                }

                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    tvResult.visibility = View.VISIBLE
                    tvResult.text = t.message ?: "Unknown Error"
                    Log.e("API_ERROR", t.message ?: "Unknown Error")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun cropBitmapCenter(bitmap: Bitmap): Bitmap {
        val cropWidth = (bitmap.width * 0.75).toInt()
        val cropHeight = (cropWidth / 1.45).toInt()
        val left = (bitmap.width - cropWidth) / 2
        val top = (bitmap.height - cropHeight) / 2
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}