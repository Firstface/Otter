package com.example.otter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.example.otter.databinding.ActivityMainBinding
import com.example.otter.ui.HomeFragment
import com.example.otter.ui.ProfileFragment
import com.example.otter.ui.RecommendationFragment


/**
 * MainActivity 是应用的主活动，负责管理底部导航栏和 fragment 切换。
 * 1. 创建并保存 fragment 实例
 * 2. 初始化 fragment 并添加到容器
 * 3. 设置底部导航栏点击监听器，切换 fragment
 */
class MainActivity : AppCompatActivity() {
    /**
     * 活动布局绑定对象
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * 首页 fragment 实例，使用 lazy 初始化，确保在首次访问时才创建
     */
    private val homeFragment by lazy { HomeFragment() }
    /**
     * 推荐 fragment 实例，使用 lazy 初始化，确保在首次访问时才创建
     */
    private val recommendationFragment by lazy { RecommendationFragment() }
    /**
     * 个人中心 fragment 实例，使用 lazy 初始化，确保在首次访问时才创建
     */
    private val profileFragment by lazy { ProfileFragment() }
    /**
     * 当前活跃的 fragment，默认是首页 fragment
     */
    private var activeFragment: Fragment = homeFragment

    /**
     * 活动创建时调用，负责初始化 UI 和 fragment 管理
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**
         * 检查是否是首次创建活动
         * 1. 如果是，添加所有 fragment 到容器并隐藏所有 fragment 但首页 fragment
         * 2. 如果不是，直接切换到上次活跃的 fragment
         */
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, profileFragment, "3").hide(profileFragment)
                .add(R.id.fragment_container, recommendationFragment, "2").hide(recommendationFragment)
                .add(R.id.fragment_container, homeFragment, "1")
                .commit()
        }

        /**
         * 设置底部导航栏点击监听器，切换 fragment
         */
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> switchFragment(homeFragment)
                R.id.navigation_recommend -> switchFragment(recommendationFragment)
                R.id.navigation_profile -> switchFragment(profileFragment)
            }
            true
        }
    }

        /**
         * 切换 fragment 方法
         * 1. 隐藏当前活跃 fragment
         * 2. 显示目标 fragment
         * 3. 更新活跃 fragment 引用
         */
    private fun switchFragment(targetFragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment)
        transaction.show(targetFragment)
        transaction.commit()
        activeFragment = targetFragment
    }
}