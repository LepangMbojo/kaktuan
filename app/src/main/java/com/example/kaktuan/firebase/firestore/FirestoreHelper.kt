package com.example.kaktuan.firebase.firestore

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    fun getDatabase() = db
}