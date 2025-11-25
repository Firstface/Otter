package com.example.otter

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapp.adapter.ToolsAdapter
// import com.example.myapp.adapter.ToolsAdapter
import com.example.otter.databinding.ActivityMainBinding
import com.example.otter.model.ToolItem

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置三个绿色卡片的内容
        setupGreenCards()

        val rvTools = binding.rvTools

        // 3. 准备数据
        // 注意：如果你还没有这些 drawable 图标，代码会报红，请先添加图片资源或暂时注释掉
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

        // 4. 设置适配器
         val adapter = ToolsAdapter(toolList)
         rvTools.adapter = adapter

        // 5. 设置布局管理器 (关键步骤)
        // 在 Activity 中，Context 就是 'this'
        // spanCount = 4 表示一行显示 4 个，满了自动换行
        val layoutManager = GridLayoutManager(this, 4)
        rvTools.layoutManager = layoutManager
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
}