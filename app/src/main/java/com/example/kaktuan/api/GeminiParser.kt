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

        // 1. Susun narasi profil fisik dengan tambahan gender
        val infoFisik = StringBuilder("Profil pengguna saat ini: ")
        infoFisik.append(if (!gender.isNullOrEmpty()) "Jenis kelamin $gender. " else "")
        infoFisik.append(if (umur != null && umur > 0) "Umur $umur tahun. " else "Umur tidak diketahui. ")
        infoFisik.append(if (tinggi != null && tinggi > 0.0) "Tinggi badan $tinggi cm. " else "")
        infoFisik.append(if (berat != null && berat > 0.0) "Berat badan $berat kg. " else "")

        // 2. Susun narasi kondisi penyakit
        val instruksiKesehatan = if (daftarPenyakit.isNotEmpty()) {
            "Pengguna memiliki riwayat penyakit: ${daftarPenyakit.joinToString(", ")}. Berikan analisis mendalam apakah bahan-bahan ini aman untuk kondisi penyakit tersebut."
        } else {
            "Analisis secara umum apakah komposisi makanan ini sehat dan aman untuk konsumsi harian."
        }

        return """
        Anda adalah seorang ahli gizi profesional.

        Analisis teks OCR dari label makanan berikut berdasarkan profil fisik dan kondisi kesehatan pengguna.
        
        $infoFisik
        $instruksiKesehatan

        Tugas Anda:
        Evaluasi apakah nilai informasi nilai gizi (seperti energi total, gula, lemak, natrium) dan bahan-bahannya proporsional atau berbahaya bagi profil tubuh dan penyakit pengguna di atas. Perhitungkan juga standar Angka Kecukupan Gizi (AKG) yang berbeda berdasarkan jenis kelamin.

        Kembalikan HANYA JSON VALID. Dilarang menambahkan teks pengantar, penutup, atau markdown (seperti ```json).

        Format:
        {
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
            "conclusion": "Tuliskan kesimpulan keamanan gizi spesifik berdasarkan jenis kelamin, umur, berat badan, tinggi badan, dan riwayat penyakit pengguna di sini dalam 2-3 kalimat.",
            "specific_warnings": ["Sebutkan peringatan bahan atau kandungan yang melebihi batas harian pengguna"]
          }
        }

        Gunakan null jika data tidak ditemukan.

        Teks OCR:
        $ocrText
        """.trimIndent()
    }
}