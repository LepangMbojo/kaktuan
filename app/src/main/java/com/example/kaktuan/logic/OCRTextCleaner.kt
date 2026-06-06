package com.example.kaktuan.logic

object OCRTextCleaner {

    fun clean(text: String): String {

        return text
            .lowercase()

            // hapus enter
            .replace("\n", " ")

            // hapus tab
            .replace("\t", " ")

            // hapus karakter aneh
            .replace(Regex("[^a-zA-Z0-9% ]"), " ")

            // rapikan spasi
            .replace(Regex("\\s+"), " ")

            .trim()
    }
}