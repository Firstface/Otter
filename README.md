Otter - 仿醒图 (Hypic) Android 客户端
<p align="left"> <img src="https://img.shields.io/badge/Language-Kotlin-orange.svg" alt="Kotlin"> <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android"> <img src="https://img.shields.io/badge/Architecture-MVVM-blue.svg" alt="MVVM"> <img src="https://img.shields.io/badge/Rendering-OpenGL_ES-purple.svg" alt="OpenGL"> </p>

Otter 是一款基于 Android 原生开发的图片编辑与美化应用，旨在深度复刻“醒图 (Hypic)” App 的 UI 风格与核心交互体验。

本项目采用现代化的 MVVM 架构，实现了从系统级媒体管理（适配 Android 14+）到高性能图形渲染（基于 OpenGL ES）的完整链路。项目不仅注重 UI 的还原度，更在工程架构、兼容性适配（Scoped Storage）以及图像处理算法上进行了深度实践。

✨ 核心功能 (Features)
1. 🎨 沉浸式 UI 体验
高度还原视觉：复刻醒图品牌色系（Neon Green #CCFF00 / Black），适配深色模式。

动态首页：基于 RecyclerView 的多类型 Item 布局，实现了功能卡片、网格工具栏的配置化扩展。

交互细节：实现自定义底部 Tab 导航栏、顶部沉浸式状态栏，以及 Activity 间的平滑过渡动画。

2. ⚡ 高性能修图引擎 (Core Editing Engine)

OpenGL 渲染管线：摒弃传统的 ImageView，采用 GLSurfaceView + 自定义 PhotoRender，利用 GPU 加速图像显示与变换，确保高分辨率图片操作流畅 。


手势交互系统：支持单指平移、双指缩放/旋转的无缝操作体验 。

高级编辑工具：


智能裁剪：自定义 CropOverlayView，攻克了屏幕坐标系与纹理坐标系的映射算法，实现高精度裁剪 。


画笔涂鸦：支持自定义画笔路径绘制 。


历史回溯：基于栈（Stack）结构实现了 Undo/Redo（撤销/重做）机制，保障用户编辑安全 。

3. 🖼️ 智能媒体管理 (Media Management)

多模式选择：支持“单选修图”与“批量修图”两种工作流，包含底部待编辑队列预览 。

深度适配 Android 14：

完美适配 Scoped Storage（分区存储），使用 Uri 替代绝对路径。

完整支持 Android 14 Partial Access（部分照片访问权限），提供“管理已选照片”入口，形成权限请求闭环 。

自定义相册与相机：重构相册选择页与相机页，支持分页加载与缩略图实时预览。

🏗️ 技术栈 (Tech Stack)
语言: Kotlin

架构模式: MVVM (Model-View-ViewModel)

使用 ViewModel + LiveData/Flow 实现 UI 与数据驱动的解耦 。

图形与渲染:


OpenGL ES 2.0+: 处理底层纹理渲染 。

GLSurfaceView: 高性能图像展示容器。


Custom View: 实现裁剪遮罩、画笔轨迹等复杂 UI 。

UI 构建:

ViewBinding (替代 findViewById) 。

ConstraintLayout & RecyclerView.

Material Design Components.

异步与数据:

Coroutines: 异步任务处理（IO 操作、图片加载）。

MediaStore API: 系统级媒体资源查询与管理。

📱 技术难点与适配 (Highlights)
1. Android 14 权限适配 (Partial Access)
针对 Android 14 引入的隐私变更，项目实现了完整的权限逻辑：

兼容 READ_MEDIA_VISUAL_USER_SELECTED 权限。

在用户选择“部分允许”后，动态调整相册加载逻辑，并提供二次修改授权的入口。

统一权限校验工具类，抹平了 Android 10 - 14 的版本差异。

2. OpenGL 坐标系与屏幕交互
解决了触摸事件（Screen Coordinates）与 OpenGL 纹理坐标（Texture Coordinates）之间的转换难题。

实现了 CropOverlayView（遮罩层）与底层 GL 渲染图像的联动，确保裁剪框操作与最终图片输出的一致性 。

3. 厂商兼容性 (Vendor Compatibility)
解决小米、华为等设备在文件路径读取上的差异，全面弃用 _data 字段，转而使用 ContentUris 生成标准 Uri。

📅 更新日志 (Changelog)
v1.3.0 - 修图引擎核心实现 (Current)

Feat: 引入 GLSurfaceView 和 PhotoRender，重构修图底层架构 。


Feat: 实现裁剪功能，自定义遮罩层，解决坐标映射问题 。


Feat: 添加画笔功能及 Undo/Redo（撤销重做）栈逻辑 。


Fix: 修复保存相片后的权限同步与显示 Bug 。

v1.2.0 - 媒体管理与适配

Feat: 完善相册页，提取 MediaLoader 工具类，实现 Android 14 权限深度适配 。


Feat: 增加“批量修图”模式入口及底部选图栏 UI 。


Refactor: 全面重构 PhotoSelectionActivity 和 CustomCameraActivity 至 MVVM 架构 。

v1.1.0 - 架构重构

Refactor: 引入 HomeViewModel，解耦主界面逻辑 。


Feat: 将硬编码字符串提取为枚举类型，优化代码可维护性 。

v1.0.0 - 基础 UI 构建

UI: 完成主界面 Fragment 化改造，确立品牌色调（荧光绿）。

🚀 待办事项 (Todo)
[ ] 特效升级: 接入 LUT 滤镜算法，实现色彩调节功能。

[ ] 性能优化: 优化 OpenGL 纹理内存占用，防止 OOM。

[ ] UI 细节: 完善主页点击后的 Activity 过渡动画。

[ ] 文本编辑: 添加图片上层文字贴纸功能。
