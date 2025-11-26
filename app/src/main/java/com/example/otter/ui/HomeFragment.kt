package com.example.otter.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.otter.PhotoSelectionActivity
import com.example.otter.R
import com.example.otter.adapter.RecommendationAdapter
import com.example.otter.adapter.ToolsAdapter
import com.example.otter.databinding.FragmentHomeBinding
import com.example.otter.model.RecommendationItem
import com.example.otter.model.ToolItem

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupGreenCards()
        setupToolsRecyclerView()
        setupRecommendationRecyclerView()
    }

    private fun setupClickListeners() {
        // Set click listeners for all function buttons
        binding.btnImport.setOnClickListener { openPhotoSelection("导入") }
        binding.btnCamera.setOnClickListener { openPhotoSelection("相机") }
        binding.cardLivePhoto.root.setOnClickListener { openPhotoSelection("修实况Live") }
        binding.cardBeautify.root.setOnClickListener { openPhotoSelection("人像美化") }
        binding.cardCollage.root.setOnClickListener { openPhotoSelection("拼图") }
    }

    private fun openPhotoSelection(functionName: String) {
        val intent = Intent(requireContext(), PhotoSelectionActivity::class.java).apply {
            putExtra("SELECTED_FUNCTION_NAME", functionName)
        }
        startActivity(intent)
    }

    private fun setupToolsRecyclerView() {
        val rvTools = binding.rvTools
        rvTools.layoutManager = GridLayoutManager(requireContext(), 4)

        val toolList = listOf(
            ToolItem("相机", R.drawable.ic_linked_camera),
            ToolItem("批量修图", R.drawable.ic_batch_edit),
            ToolItem("画质超清", R.drawable.ic_hd),
            ToolItem("魔法消除", R.drawable.ic_eraser),
            ToolItem("智能抠图", R.drawable.ic_cutout),
            ToolItem("一键出片", R.drawable.ic_film),
            ToolItem("一键美化", R.drawable.ic_magic),
            ToolItem("所有工具", R.drawable.ic_grid_all)
        )

        rvTools.adapter = ToolsAdapter(toolList)
        // TODO: Add click listener to the adapter items as well
    }

    private fun setupRecommendationRecyclerView() {
        val rvRecommendations = binding.rvRecommendations
        rvRecommendations.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Using placeholder drawables for now
        val recommendationList = listOf(
            RecommendationItem(android.R.drawable.ic_menu_report_image, "海边风景"),
            RecommendationItem(android.R.drawable.ic_menu_camera, "城市夜景"),
            RecommendationItem(android.R.drawable.ic_menu_gallery, "静谧森林"),
            RecommendationItem(android.R.drawable.ic_menu_slideshow, "日落黄昏"),
            RecommendationItem(android.R.drawable.ic_menu_mapmode, "雪山之巅")
        )

        rvRecommendations.adapter = RecommendationAdapter(recommendationList)
    }

    private fun setupGreenCards() {
        binding.cardLivePhoto.ivCardIcon.setImageResource(R.drawable.ic_live)
        binding.cardLivePhoto.tvCardText.text = "修实况Live"

        binding.cardBeautify.ivCardIcon.setImageResource(R.drawable.ic_magic)
        binding.cardBeautify.tvCardText.text = "人像美化"

        binding.cardCollage.ivCardIcon.setImageResource(R.drawable.ic_puzzle)
        binding.cardCollage.tvCardText.text = "拼图"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}