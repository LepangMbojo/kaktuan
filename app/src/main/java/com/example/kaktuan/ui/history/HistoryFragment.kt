package com.example.kaktuan.ui.history

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kaktuan.databinding.FragmentHistoryBinding
import com.example.kaktuan.model.ScanHistory
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.supabase.SupabaseDatabaseHelper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseHelper: SupabaseDatabaseHelper

    private var masterHistoryList: List<ScanHistory> = emptyList()
    private var currentFilter = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = SupabaseDatabaseHelper()

        setupFilters()
        setupSearch()
        muatRiwayat()
    }

    private fun setupFilters() {
        binding.filterSemua.setOnClickListener {
            currentFilter = "Semua"
            updateFilterUI(binding.filterSemua)
            terapkanFilterPencarian()
        }
        binding.filterAman.setOnClickListener {
            currentFilter = "Aman"
            updateFilterUI(binding.filterAman)
            terapkanFilterPencarian()
        }
        binding.filterBahaya.setOnClickListener {
            currentFilter = "Bahaya"
            updateFilterUI(binding.filterBahaya)
            terapkanFilterPencarian()
        }
    }

    private fun updateFilterUI(activeView: TextView) {
        val filters = listOf(binding.filterSemua, binding.filterAman, binding.filterBahaya)
        filters.forEach { tv ->
            if (tv == activeView) {
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#38C6A5"))
                tv.setTextColor(Color.parseColor("#0F172A"))
            } else {
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                tv.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    private fun setupSearch() {
        binding.etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                terapkanFilterPencarian()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun muatRiwayat() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return tampilkanKosong()

        databaseHelper.getScanHistory(uid) { historyList ->
            if (!isAdded || activity == null) return@getScanHistory

            activity?.runOnUiThread {
                if (historyList.isNullOrEmpty()) {
                    tampilkanKosong()
                } else {
                    masterHistoryList = historyList.sortedByDescending { it.timestamp }

                    val totalScan = masterHistoryList.size
                    val countAman = masterHistoryList.count {
                        (it.healthScore ?: 0) >= 40
                    }

                    val countBahaya = masterHistoryList.count {
                        (it.healthScore ?: 0) < 40
                    }

                    binding.tvTotalScan.text = totalScan.toString()
                    binding.tvAman.text = countAman.toString()
                    binding.tvBahaya.text = countBahaya.toString()

                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE

                    terapkanFilterPencarian()
                }
            }
        }
    }

    private fun terapkanFilterPencarian() {
        val query = binding.etSearchHistory.text.toString().trim().lowercase()

        val filteredList = masterHistoryList.filter { item ->
            val matchesSearch = item.productName.lowercase().contains(query)
            val isAman = (item.healthScore ?: 0) >= 40
            val matchesCategory = when (currentFilter) {
                "Aman" -> isAman
                "Bahaya" -> !isAman
                else -> true
            }

            matchesSearch && matchesCategory
        }

        if (filteredList.isEmpty()) {
            tampilkanKosong()
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE

            val adapter = HistoryAdapter(
                listHistory = filteredList,
                onItemClick = { selectedItem ->
                    val detailFragment = DetailScanFragment.newInstance(selectedItem.scanId)
                    parentFragmentManager.beginTransaction()
                        .replace(com.example.kaktuan.R.id.frameContainer, detailFragment)
                        .addToBackStack(null)
                        .commit()
                },
                onEditClick = { selectedItem ->
                    tampilkanDialogEditNama(selectedItem)
                }
            )
            binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
            binding.rvHistory.adapter = adapter
        }
    }

    private fun tampilkanDialogEditNama(history: ScanHistory) {
        val context = requireContext()
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Edit Nama Produk")

        val input = android.widget.EditText(context)
        input.setText(history.productName)
        input.setSelection(input.text.length)

        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(50, 20, 50, 0)
        val container = android.widget.FrameLayout(context)
        container.addView(input, layoutParams)
        builder.setView(container)

        builder.setPositiveButton("Simpan") { dialog, _ ->
            val namaBaru = input.text.toString().trim()
            if (namaBaru.isNotEmpty() && namaBaru != history.productName) {
                updateNamaKeSupabase(history.scanId, namaBaru)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateNamaKeSupabase(docId: String, namaBaru: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Membangun payload JSON untuk nama kolom di Supabase
                val updateData = buildJsonObject {
                    put("product_name", namaBaru) // Sesuaikan jika nama kolom Anda berbeda
                }

                // Melakukan update langsung berdasarkan ID tabel history
                SupabaseClient.client.postgrest["history"].update(updateData) {
                    filter { eq("id", docId) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Nama berhasil diubah", Toast.LENGTH_SHORT).show()
                    // Muat ulang data agar tampilan langsung ter-refresh
                    muatRiwayat()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mengubah: ${e.message}", Toast.LENGTH_SHORT).show()
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