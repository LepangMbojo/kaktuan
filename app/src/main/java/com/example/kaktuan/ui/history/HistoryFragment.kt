package com.example.kaktuan.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kaktuan.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    // Setup ViewBinding
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Nanti kita akan memanggil data riwayat dari Firestore di sini.
        // Jika data ada, kita sembunyikan layoutEmptyState dan munculkan rvHistory.

        // Konfigurasi sementara: Pastikan tampilan kosong (Empty State) muncul
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
    }

    // Mencegah memory leak saat fragment ditutup
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}