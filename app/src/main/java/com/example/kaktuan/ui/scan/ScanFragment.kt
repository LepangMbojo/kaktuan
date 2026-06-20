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
import com.example.kaktuan.logic.GeminiParser
import com.example.kaktuan.logic.OCRTextCleaner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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



    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    private var scanAnimator: ObjectAnimator? = null



// Variabel penampung data sementara untuk disimpan

    private var currentOcrText: String = ""

    private var currentGeminiAnalysis: String = ""



// Inisialisasi Firebase

    private lateinit var auth: FirebaseAuth

    private lateinit var firestoreHelper: FirestoreHelper



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

        auth = FirebaseAuth.getInstance()

        firestoreHelper = FirestoreHelper()



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

        val frameHeightPx = 220 * density // Mengikuti tinggi scanFrame di XML (220dp)



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



// Hentikan animasi saat memproses

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

                        ambilDataPenyakitLaluKeGemini(cleanedText)

                    } else {

                        progressBar.visibility = View.GONE

                        btnCapture.isEnabled = true

// Teks tidak ketemu -> isSuccess = false (Tombol simpan hilang)

                        showResultBottomSheet("Teks tidak ditemukan pada gambar. Silakan posisikan ulang kemasan makanan.", false)

                    }

                }



                override fun onFailure(call: Call<VisionResponse>, t: Throwable) {

                    progressBar.visibility = View.GONE

                    btnCapture.isEnabled = true

// Koneksi OCR gagal -> isSuccess = false

                    showResultBottomSheet("Gagal menghubungi server OCR: ${t.message}", false)

                    Log.e("API_ERROR", t.message ?: "Unknown Error")

                }

            })

    }



    private fun ambilDataPenyakitLaluKeGemini(ocrText: String) {

        currentOcrText = ocrText

        val uid = auth.currentUser?.uid



        if (uid != null) {

            firestoreHelper.getUserProfile(uid) { user ->

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

        val request = GeminiRequest(

            contents = listOf(Content(parts = listOf(Part(promptFinal))))

        )



        GeminiRetrofitClient.instance

            .generateContent(BuildConfig.GEMINI_API_KEY, request)

            .enqueue(object : Callback<GeminiResponse> {

                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {

                    progressBar.visibility = View.GONE



                    if (response.isSuccessful) {

                        val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text



                        if (result != null) {

                            try {

                                val cleanJsonStr = result.replace("```json", "").replace("```", "").trim()

                                currentGeminiAnalysis = cleanJsonStr



                                val jsonObject = JSONObject(cleanJsonStr)

                                val analysis = jsonObject.getJSONObject("analysis")

                                val isSafe = analysis.getBoolean("is_safe")

                                val conclusion = analysis.getString("conclusion")



                                val statusTeks = if (isSafe) "✅ LAYAK DIMAKAN" else "❌ TIDAK LAYAK!"

                                val uiText = "$statusTeks\n\n$conclusion"



// Data BERHASIL didapatkan -> isSuccess = true

                                showResultBottomSheet(uiText, true)



                            } catch (e: Exception) {

                                Log.e("JSON_PARSE_ERROR", "Gagal membedah JSON: ${e.message}")

                                currentGeminiAnalysis = result

// Meskipun JSON gagal dibedah, AI tetap merespon teks -> isSuccess = true

                                showResultBottomSheet("Hasil (Mentah):\n$result", true)

                            }

                        } else {

// AI merespons kosong -> isSuccess = false

                            showResultBottomSheet("Tidak ada respons dari AI.", false)

                        }

                    } else {

                        btnCapture.isEnabled = true

                        if (response.code() == 429) {

// Kena limit -> isSuccess = false (Tombol simpan hilang)

                            showResultBottomSheet("Ups! Limit AI sedang penuh.\nSilakan tunggu sekitar 1 menit dan coba scan lagi ya.", false)

                        } else {

// Error server lain -> isSuccess = false

                            showResultBottomSheet("Gagal memproses (Kode: ${response.code()}).", false)

                        }

                    }

                }



                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {

                    progressBar.visibility = View.GONE

                    btnCapture.isEnabled = true

// Koneksi gagal -> isSuccess = false

                    showResultBottomSheet("Gagal menghubungi Gemini: Periksa koneksi internet Anda.", false)

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



// LOGIKA BARU: Jika gagal/tidak ada data, sembunyikan tombol Simpan

        if (isSuccess) {

            btnSheetSimpan.visibility = View.VISIBLE

        } else {

            btnSheetSimpan.visibility = View.GONE

        }



// Logika Tombol Batal di Pop-up

        btnSheetBatal.setOnClickListener {

            bottomSheetDialog.dismiss()

            resetScanner()

        }



// Logika Tombol Simpan di Pop-up

        btnSheetSimpan.setOnClickListener {

            btnSheetSimpan.isEnabled = false // Mencegah double klik

            simpanKeRiwayat(bottomSheetDialog)

        }



// Jika user menutup pop-up dengan menggeser (swipe down) atau memencet area luar

        bottomSheetDialog.setOnCancelListener {

            resetScanner()

        }



        bottomSheetDialog.show()

    }



    private fun simpanKeRiwayat(dialog: BottomSheetDialog) {

        val uid = auth.currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()



// 1. Ubah String JSON dari Gemini menjadi struktur Map/Object

        val gson = Gson()

        val type = object : TypeToken<Map<String, Any>>() {}.type



        var analisisMap: Map<String, Any> = emptyMap()

        try {

// Proses konversi dari String ke Map

            analisisMap = gson.fromJson(currentGeminiAnalysis, type)

        } catch (e: Exception) {

            Log.e("GSON_ERROR", "Gagal mengkonversi JSON: ${e.message}")

// Fallback jika gagal, simpan sebagai string mentah di dalam map

            analisisMap = mapOf("raw_data" to currentGeminiAnalysis)

        }



// 2. Masukkan ke dalam data history

        val historyData = hashMapOf(

            "timestamp" to System.currentTimeMillis(),

            "komposisi_makanan" to currentOcrText, // Teks struk/kamera

            "analisis_kesehatan" to analisisMap, // SEKARANG BERUPA OBJECT!

            "status" to "Dimakan"

        )



        db.collection("users").document(uid).collection("history")

            .add(historyData)

            .addOnSuccessListener {

                Toast.makeText(requireContext(), "Berhasil disimpan ke riwayat!", Toast.LENGTH_SHORT).show()

                dialog.dismiss()

                resetScanner()

            }

            .addOnFailureListener { e ->

                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()

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