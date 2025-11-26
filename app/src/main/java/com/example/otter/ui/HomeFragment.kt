package com.example.otter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.otter.R
import com.example.otter.adapter.ToolsAdapter
import com.example.otter.databinding.FragmentHomeBinding
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

        // 设置三个绿色卡片的内容
        setupGreenCards()

        // 设置 RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val rvTools = binding.rvTools

        // 1. 设置布局管理器
        rvTools.layoutManager = GridLayoutManager(requireContext(), 4)

        // 2. 准备数据 (使用占位符图标以避免编译错误)
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

        // 3. 创建并设置适配器
        rvTools.adapter = ToolsAdapter(toolList)
    }

    private fun setupGreenCards() {
        // 卡片1：修实况Live
        binding.cardLivePhoto.ivCardIcon.setImageResource(R.drawable.ic_live)
        binding.cardLivePhoto.tvCardText.text = "修实况Live"

        // 卡片2：人像美化
        binding.cardBeautify.ivCardIcon.setImageResource(R.drawable.ic_magic)
        binding.cardBeautify.tvCardText.text = "人像美化"

        // 卡片3：拼图
        binding.cardCollage.ivCardIcon.setImageResource(R.drawable.ic_puzzle)
        binding.cardCollage.tvCardText.text = "拼图"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}