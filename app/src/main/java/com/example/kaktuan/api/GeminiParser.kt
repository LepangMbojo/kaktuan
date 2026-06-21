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
        1. Identifikasi Nama Produk.
        2. Tentukan 'health_score' (0-100):
           - 100: Sangat sehat/aman.
           - 50-70: Perlu dibatasi.
           - 0-49: Sangat tidak disarankan untuk profil pengguna.
           - Berikan penalti skor besar jika kandungan (gula/garam/lemak/alergen) berbahaya bagi riwayat penyakit pengguna.
        3. Evaluasi gizi.

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
            "conclusion": "Kesimpulan keamanan gizi spesifik dalam 2-3 kalimat.",
            "specific_warnings": ["Sebutkan peringatan bahan atau kandungan yang melebihi batas harian pengguna"]
          }
        }

        Gunakan null jika data tidak ditemukan.

        Teks OCR:
        $ocrText
        """.trimIndent()
    }
}