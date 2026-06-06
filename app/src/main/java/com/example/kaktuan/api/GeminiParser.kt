package com.example.kaktuan.logic

object GeminiParser {

    fun buildPrompt(
        ocrText: String
    ): String {

        return """
        Anda adalah ahli gizi.

        Analisis teks OCR label makanan berikut.

        Kembalikan HANYA JSON VALID.

        Format:

        {
          "ingredients": [],
          "nutrition": {
            "serving_size": "",
            "servings_per_container": "",
            "total_energy": {
              "value": 0,
              "unit": "kkal"
            },
            "protein": {
              "value": 0,
              "unit": "g"
            },
            "fat": {
              "value": 0,
              "unit": "g"
            },
            "carbohydrate": {
              "value": 0,
              "unit": "g"
            },
            "sugar": {
              "value": 0,
              "unit": "g"
            },
            "sodium": {
              "value": 0,
              "unit": "mg"
            }
          },
          "warnings": []
        }

        Gunakan null jika data tidak ditemukan.

        Teks OCR:

        $ocrText

        """.trimIndent()
    }
}