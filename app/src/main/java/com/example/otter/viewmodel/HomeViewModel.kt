package com.example.otter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.otter.model.FunctionType
import com.example.otter.model.RecommendationItem
import com.example.otter.model.RecommendationType
import com.example.otter.model.ToolItem
import com.example.otter.model.ToolType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 使用密封类来定义从主屏幕出发的、明确的导航事件
sealed class HomeNavigationEvent {
    data class ToPhotoSelection(val functionName: String) : HomeNavigationEvent()
    object ToCamera : HomeNavigationEvent()
}

class HomeViewModel : ViewModel() {

    // StateFlow，用于持有工具列表数据
    private val _toolList = MutableStateFlow<List<ToolItem>>(emptyList())
    val toolList = _toolList.asStateFlow()

    // StateFlow，用于持有推荐列表数据
    private val _recommendationList = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val recommendationList = _recommendationList.asStateFlow()

    // SharedFlow，用于处理一次性的导航事件
    private val _navigationEvent = MutableSharedFlow<HomeNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        // ViewModel 初始化时即加载所有静态数据
        loadData()
    }

    private fun loadData() {
        _toolList.value = ToolType.values().map { ToolItem(it) }
        _recommendationList.value = RecommendationType.values().map { RecommendationItem(it) }
    }

    // --- 事件处理方法 ---

    /** 当工具列表中的某一项被点击时调用 */
    fun onToolClick(tool: ToolItem) {
        if(tool.type.displayName != "所有工具"){
            viewModelScope.launch {
                _navigationEvent.emit(HomeNavigationEvent.ToPhotoSelection(tool.type.displayName))
            }
        }
    }

    /** 当“导入”按钮被点击时调用 */
    fun onImportClick() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.ToPhotoSelection(FunctionType.IMPORT.displayName))
        }
    }

    /** 当“相机”按钮被点击时调用 */
    fun onCameraClick() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.ToCamera)
        }
    }

    /** 当“修实况Live”卡片被点击时调用 */
    fun onLivePhotoCardClick() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.ToPhotoSelection(FunctionType.LIVE_PHOTO_EDIT.displayName))
        }
    }

    /** 当“人像美化”卡片被点击时调用 */
    fun onBeautifyCardClick() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.ToPhotoSelection(FunctionType.PORTRAIT_BEAUTY.displayName))
        }
    }

    /** 当“拼图”卡片被点击时调用 */
    fun onCollageCardClick() {
        viewModelScope.launch {
            _navigationEvent.emit(HomeNavigationEvent.ToPhotoSelection(FunctionType.COLLAGE.displayName))
        }
    }
}
