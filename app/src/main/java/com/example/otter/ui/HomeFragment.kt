package com.example.otter.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.otter.CustomCameraActivity
import com.example.otter.PhotoSelectionActivity
import com.example.otter.R
import com.example.otter.adapter.RecommendationAdapter
import com.example.otter.adapter.ToolsAdapter
import com.example.otter.databinding.FragmentHomeBinding
import com.example.otter.model.FunctionType
import com.example.otter.viewmodel.HomeNavigationEvent
import com.example.otter.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

/**
 * 主界面Fragment，展示推荐列表和编辑工具列表
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 通过 ktx 扩展库，轻松获取 ViewModel 实例
    private val viewModel: HomeViewModel by viewModels()

    /**
     * 当视图被创建时，使用 Data Binding 绑定布局
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    /**
     * 当视图创建完成时，设置点击监听器、绿色卡片和 RecyclerView
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    /**
     * 初始化所有静态UI配置
     */
    private fun setupUI() {
        setupClickListeners()
        setupGreenCards()
        setupToolsRecyclerView()
        setupRecommendationRecyclerView()
    }

    /**
     * 监听 ViewModel 中的数据流和事件流
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle 确保仅在视图处于活跃状态时收集数据
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听工具列表数据
                launch {
                    viewModel.toolList.collect { toolList ->
                        binding.rvTools.adapter = ToolsAdapter(toolList) { tool ->
                            viewModel.onToolClick(tool) // 将点击事件委托给 ViewModel
                        }
                    }
                }

                // 监听推荐列表数据
                launch {
                    viewModel.recommendationList.collect { recommendationList ->
                        binding.rvRecommendations.adapter = RecommendationAdapter(recommendationList)
                    }
                }

                // 监听一次性的导航事件
                launch {
                    viewModel.navigationEvent.collect { event ->
                        event?.let {
                            when (it) {
                                is HomeNavigationEvent.ToPhotoSelection -> openPhotoSelection(it.functionName)
                                is HomeNavigationEvent.ToCamera -> openCamera()
                            }
                            viewModel.onEventHandled() // 事件处理完毕，通知ViewModel
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置所有点击监听器，并将事件委托给 ViewModel
     */
    private fun setupClickListeners() {
        binding.btnImport.setOnClickListener { viewModel.onImportClick() }
        binding.btnCamera.setOnClickListener { viewModel.onCameraClick() }
        binding.cardLivePhoto.root.setOnClickListener { viewModel.onLivePhotoCardClick() }
        binding.cardBeautify.root.setOnClickListener { viewModel.onBeautifyCardClick() }
        binding.cardCollage.root.setOnClickListener { viewModel.onCollageCardClick() }
    }
    /**
     * 打开相册选择界面，传递当前点击的功能名称
     */
    private fun openPhotoSelection(functionName: String) {
        val intent = Intent(requireContext(), PhotoSelectionActivity::class.java).apply {
            putExtra(PhotoSelectionActivity.SELECTED_FUNCTION_NAME, functionName)
        }
        startActivity(intent)
    }

    /**
     * 打开自定义相机界面
     */
    private fun openCamera() {
        val intent = Intent(requireContext(), CustomCameraActivity::class.java)
        startActivity(intent)
    }
    /**
     * 设置编辑工具列表的 RecyclerView，使用网格布局
     */
    private fun setupToolsRecyclerView() {
        binding.rvTools.layoutManager = GridLayoutManager(requireContext(), 4)
        // Adapter 将在 observeViewModel 中设置
    }
    /**
     * 设置推荐列表的 RecyclerView，使用水平线性布局
     */
    private fun setupRecommendationRecyclerView() {
        binding.rvRecommendations.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        // Adapter 将在 observeViewModel 中设置
    }
    /**
     * 设置绿色卡片的图标和文本，使用枚举类型 FunctionType 来确保类型安全
     */
    private fun setupGreenCards() {
        // 使用枚举来设置静态文本，消除了魔术字符串
        binding.cardLivePhoto.ivCardIcon.setImageResource(R.drawable.ic_live)
        binding.cardLivePhoto.tvCardText.text = FunctionType.LIVE_PHOTO_EDIT.displayName

        binding.cardBeautify.ivCardIcon.setImageResource(R.drawable.ic_magic)
        binding.cardBeautify.tvCardText.text = FunctionType.PORTRAIT_BEAUTY.displayName

        binding.cardCollage.ivCardIcon.setImageResource(R.drawable.ic_puzzle)
        binding.cardCollage.tvCardText.text = FunctionType.COLLAGE.displayName
    }
    /**
     * 当视图被销毁时，清除绑定以避免内存泄漏
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
