package com.example.kaktuan.logic

object GeminiParser {

    fun buildPrompt(
        ocrText: String,
        daftarPenyakit: List<String>,
        umur: Int?,
        tinggi: Double?,
        berat: Double?,
        gender: String?
    ): String {

        // 1. Susun narasi profil fisik
        val infoFisik = StringBuilder("Profil pengguna saat ini: ")
        infoFisik.append(if (!gender.isNullOrEmpty()) "Jenis kelamin $gender. " else "")
        infoFisik.append(if (umur != null && umur > 0) "Umur $umur tahun. " else "Umur tidak diketahui. ")
        infoFisik.append(if (tinggi != null && tinggi > 0.0) "Tinggi badan $tinggi cm. " else "")
        infoFisik.append(if (berat != null && berat > 0.0) "Berat badan $berat kg. " else "")

        // 2. Susun narasi kondisi penyakit (DIPERBAIKI)
        val instruksiKesehatan = if (daftarPenyakit.isNotEmpty()) {
            "Pengguna memiliki riwayat penyakit: ${daftarPenyakit.joinToString(", ")}. PERHATIAN: Nilai 'is_safe' menjadi false HANYA JIKA komposisi ini mengandung bahan yang secara medis sangat membahayakan atau memicu fatal penyakit bawaan tersebut."
        } else {
            "Pengguna TIDAK MEMILIKI riwayat penyakit. Oleh karena itu, pada dasarnya semua makanan kemasan bernilai 'is_safe': true (LAYAK). Jika makanannya tergolong kurang sehat, tetap berikan 'is_safe': true, namun berikan teguran atau anjuran pembatasan porsi."
        }

        return """
        Anda adalah seorang ahli gizi profesional yang logis dan realistis.

        Analisis teks OCR dari label makanan berikut berdasarkan profil fisik dan kondisi kesehatan pengguna.
        
        $infoFisik
        $instruksiKesehatan

        Tugas Anda:
        1. Identifikasi Nama Produk.
        2. Tentukan 'health_score' (0-100).
        3. Evaluasi gizi.
        4. ATURAN WAJIB UNTUK "is_safe":
           - Default: true (LAYAK) untuk profil sehat.
           - Ubah menjadi false (TIDAK LAYAK) HANYA JIKA komposisi langsung bertentangan secara fatal dengan penyakit pengguna (misal: gula sangat tinggi untuk diabetes, atau alergen).
           - JANGAN gunakan false hanya karena makanan tersebut adalah makanan ringan, mengandung pengawet, atau pewarna buatan.

        Kembalikan HANYA JSON VALID. Dilarang menambahkan teks pengantar, penutup, atau markdown (seperti ```json).

        Format:
        {
          "product_name": "Nama produk di sini",
          "health_score": 0,
          "ingredients": [],
          "nutrition": {
            "serving_size": "",
            "servings_per_container": "",
            "total_energy": { "value": 0, "unit": "kkal" },
            "protein": { "value": 0, "unit": "g" },
            "fat": { "value": 0, "unit": "g" },
            "carbohydrate": { "value": 0, "unit": "g" },
            "sugar": { "value": 0, "unit": "g" },
            "sodium": { "value": 0, "unit": "mg" }
          },
          "analysis": {
            "is_safe": true,
            "conclusion": "Kesimpulan gizi spesifik (Jika is_safe true tapi makanannya tidak sehat, beritahu batas porsinya di kalimat ini)",
            "specific_warnings": ["Sebutkan peringatan bahan atau kandungan yang melebihi batas harian pengguna"]
          }
        }

        Gunakan null jika data tidak ditemukan.

        Teks OCR:
        $ocrText
        """.trimIndent()
    }
}