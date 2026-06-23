package com.example.kaktuan.ui.scan

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.example.kaktuan.logic.GeminiParser
import com.example.kaktuan.logic.OCRTextCleaner
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.supabase.SupabaseDatabaseHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var scanFrame: View
    private lateinit var scanLine: View
    private lateinit var imgCaptured: ImageView
    private lateinit var darkOverlay: View

    private var currentProductName: String = "Produk Tidak Dikenali"
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var scanAnimator: ObjectAnimator? = null

    private var currentOcrText: String = ""
    private var currentGeminiAnalysis: String = ""

    private lateinit var databaseHelper: SupabaseDatabaseHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Aplikasi butuh izin kamera", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFinder = view.findViewById(R.id.viewFinder)
        btnCapture = view.findViewById(R.id.btnCapture)
        progressBar = view.findViewById(R.id.progressBar)
        scanFrame = view.findViewById(R.id.scanFrame)
        scanLine = view.findViewById(R.id.scanLine)
        imgCaptured = view.findViewById(R.id.imgCaptured)
        darkOverlay = view.findViewById(R.id.darkOverlay)

        cameraExecutor = Executors.newSingleThreadExecutor()
        databaseHelper = SupabaseDatabaseHelper()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startScanAnimation()

        btnCapture.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
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
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch(exc: Exception) {
                Log.e("ScanFragment", "Kamera gagal dimuat", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startScanAnimation() {
        val density = resources.displayMetrics.density
        val frameHeightPx = 220 * density

        scanAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, frameHeightPx).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        progressBar.visibility = View.VISIBLE
        btnCapture.isEnabled = false

        scanAnimator?.cancel()
        scanFrame.visibility = View.GONE
        darkOverlay.visibility = View.GONE

        val photoFile = File(requireContext().cacheDir, "scan.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val croppedBitmap = cropBitmapCenter(bitmap)

                    requireActivity().runOnUiThread {
                        imgCaptured.visibility = View.VISIBLE
                        imgCaptured.setImageBitmap(croppedBitmap)
                        viewFinder.visibility = View.GONE
                    }

                    val base64Image = bitmapToBase64(croppedBitmap)
                    sendToCloudVision(base64Image)
                }

                override fun onError(exception: ImageCaptureException) {
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        btnCapture.isEnabled = true
                        Log.e("CameraX", exception.message ?: "")
                    }
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
                        ambilDataPenyakitLaluKeGemini(cleanedText)
                    } else {
                        progressBar.visibility = View.GONE
                        btnCapture.isEnabled = true
                        showResultBottomSheet("Teks tidak ditemukan pada gambar. Silakan posisikan ulang kemasan makanan.", false)
                    }
                }

                override fun onFailure(call: Call<VisionResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    showResultBottomSheet("Gagal menghubungi server OCR: ${t.message}", false)
                    Log.e("API_ERROR", t.message ?: "Unknown Error")
                }
            })
    }

    private fun ambilDataPenyakitLaluKeGemini(ocrText: String) {
        currentOcrText = ocrText
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id

        if (uid != null) {
            databaseHelper.getUserProfile(uid) { user ->
                val penyakitPasien = user?.healthConditions ?: emptyList()
                val umurPasien = user?.age
                val tinggiPasien = user?.height
                val beratPasien = user?.weight
                val genderPasien = user?.gender

                val promptFinal = GeminiParser.buildPrompt(
                    ocrText, penyakitPasien, umurPasien, tinggiPasien, beratPasien, genderPasien
                )
                sendToGemini(promptFinal)
            }
        } else {
            val promptFinal = GeminiParser.buildPrompt(
                ocrText, emptyList(), null, null, null, null
            )
            sendToGemini(promptFinal)
        }
    }

    private fun sendToGemini(promptFinal: String) {
        GeminiRetrofitClient.instance.generateContent(BuildConfig.GEMINI_API_KEY, GeminiRequest(listOf(Content(listOf(Part(promptFinal))))))
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                        try {
                            val json = JSONObject(result.replace("```json", "").replace("```", "").trim())
                            currentGeminiAnalysis = json.toString()
                            currentProductName = json.optString("product_name", "Produk Tidak Dikenali")

                            val analysis = json.getJSONObject("analysis")
                            val status = if (analysis.optBoolean("is_safe", true)) "✅ LAYAK" else "❌ TIDAK LAYAK"
                            showResultBottomSheet("$status\n\n${analysis.getString("conclusion")}", true)
                        } catch (e: Exception) {
                            showResultBottomSheet("Gagal membedah hasil AI.", false)
                        }
                    }
                }
                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    showResultBottomSheet("Koneksi gagal.", false)
                }
            })
    }

    private fun showResultBottomSheet(resultText: String, isSuccess: Boolean) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_result, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val tvSheetResult = bottomSheetView.findViewById<TextView>(R.id.tvSheetResult)
        val btnSheetBatal = bottomSheetView.findViewById<Button>(R.id.btnSheetBatal)
        val btnSheetSimpan = bottomSheetView.findViewById<Button>(R.id.btnSheetSimpan)

        tvSheetResult.text = resultText

        if (isSuccess) {
            btnSheetSimpan.visibility = View.VISIBLE
        } else {
            btnSheetSimpan.visibility = View.GONE
        }

        btnSheetBatal.setOnClickListener {
            bottomSheetDialog.dismiss()
            resetScanner()
        }

        btnSheetSimpan.setOnClickListener {
            btnSheetSimpan.isEnabled = false
            simpanKeRiwayat(bottomSheetDialog)
        }

        bottomSheetDialog.setOnCancelListener {
            resetScanner()
        }

        bottomSheetDialog.show()
    }

    private fun simpanKeRiwayat(dialog: BottomSheetDialog) {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Upload Gambar ke Storage
                val photoFile = File(requireContext().cacheDir, "scan.jpg")
                val fileName = "scan_${System.currentTimeMillis()}.jpg" // Nama file gambar tidak masalah pakai millis

                SupabaseClient.client.storage.from("scan_images").upload(fileName, photoFile.readBytes())
                val publicUrl = SupabaseClient.client.storage.from("scan_images").publicUrl(fileName)

                // 2. Parsing JSON Gemini
                val jsonObject = JSONObject(currentGeminiAnalysis)
                val score = jsonObject.optInt("health_score", 0)
                val analisisJson = Json.parseToJsonElement(currentGeminiAnalysis).jsonObject

                // PERBAIKAN DI SINI: Buat UUID yang valid sesuai standar Supabase
                val uniqueScanId = java.util.UUID.randomUUID().toString()

                // 3. Simpan ke tabel History
                val historyData = buildJsonObject {
                    put("id", uniqueScanId) // Sekarang ini adalah UUID yang valid!
                    put("user_id", uid)
                    put("product_name", currentProductName)
                    put("ocr_text", currentOcrText)
                    put("recommendation", "Hasil analisis AI")
                    put("health_score", score)
                    put("analisis_kesehatan", analisisJson)
                    put("photo_url", publicUrl)
                    put("status", "Dimakan")
                    put("timestamp", System.currentTimeMillis())
                }

                SupabaseClient.client.postgrest["history"].insert(historyData)

                // 4. OTOMATISASI NOTIFIKASI (UNTUK SEMUA SKOR)
                val judulNotif: String
                val pesanNotif: String

                if (score < 50) {
                    judulNotif = "⚠️ Peringatan Keamanan!"
                    pesanNotif = "Hati-hati! '$currentProductName' mendapat skor kesehatan $score. Kurangi konsumsinya agar kondisi Anda tetap stabil."
                } else {
                    judulNotif = "✅ Pilihan Sehat!"
                    pesanNotif = "Bagus sekali! '$currentProductName' mendapat skor kesehatan $score. Pertahankan kebiasaan makan sehat Anda!"
                }

                // SOLUSI: Gunakan buildJsonObject (sama seperti tabel history)
                val dataNotifikasi = buildJsonObject {
                    put("user_id", uid)
                    put("title", judulNotif)
                    put("message", pesanNotif)
                    put("is_read", false)
                    put("scan_id", uniqueScanId)
                }

                // Eksekusi insert ke tabel notifications
                SupabaseClient.client.postgrest["notifications"].insert(dataNotifikasi)
                Log.d("ScanFragment", "Notifikasi otomatis berhasil dikirim!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Tersimpan dengan gambar!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    resetScanner()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ScanFragment", "Gagal upload: ${e.message}")
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetScanner() {
        imgCaptured.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        btnCapture.isEnabled = true
        scanFrame.visibility = View.VISIBLE
        darkOverlay.visibility = View.VISIBLE
        startScanAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        scanAnimator?.cancel()
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