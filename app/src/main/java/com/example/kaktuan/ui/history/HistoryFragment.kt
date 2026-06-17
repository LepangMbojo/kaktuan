package com.example.kaktuan.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kaktuan.databinding.FragmentHistoryBinding
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        muatRiwayat()
    }

    private fun muatRiwayat() {
        val uid = auth.currentUser?.uid ?: return tampilkanKosong()

        firestoreHelper.getScanHistory(uid) { historyList ->
            activity?.runOnUiThread {
                if (historyList.isNullOrEmpty()) {
                    tampilkanKosong()
                } else {
                    // Urutkan terbaru di atas
                    val sorted = historyList.sortedByDescending { it.timestamp }

                    val adapter = HistoryAdapter(sorted) { item ->
                        // TODO: nanti bisa buka detail item di sini
                    }

                    binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvHistory.adapter = adapter

                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun tampilkanKosong() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}