Otter - 仿醒图 (Hypic) Android 客户端
Otter 是一款基于 Android 原生开发的图片编辑与美化应用，旨在深度复刻“醒图 (Hypic)” App 的 UI 风格与核心交互体验。

本项目采用现代化的 MVVM 架构，并在Android 存储适配 (Scoped Storage) 与 Android 14 隐私权限 (Partial Access) 方面进行了深度的技术实现，确保在不同厂商（如小米、华为）与不同系统版本上的稳定运行。

✨ 核心功能 (Features)
🎨 沉浸式首页 (Immersive Home UI)

高度还原醒图的品牌色系（Neon Green #CCFF00 / Black）。

基于 RecyclerView 的动态功能卡片布局，支持配置化扩展。

自定义底部 Tab 导航栏与顶部沉浸式状态栏适配。

🖼️ 智能相册管理 (Smart Media Picker)

多模式支持：支持“单选修图”与“批量修图”两种模式。

高性能加载：基于 Kotlin 协程与分页加载策略，流畅处理海量图片。

深度适配：

完美适配 Android 10+ 分区存储（Scoped Storage），摒弃绝对路径，采用 Uri 机制。

Android 14 Ready：支持最新的“部分照片访问权限 (Partial Access)”，并包含动态权限管理入口。

📷 自定义相机 (Custom Camera)

重构的相机取景界面，支持实时预览与缩略图显示。

🛠️ 批量编辑工作流

支持多图选中后的批量操作预览（底部待编辑队列）。

🏗️ 技术栈 (Tech Stack)
语言: Kotlin

架构模式: MVVM (Model-View-ViewModel)

使用 ViewModel 解耦 UI 逻辑与数据处理。

使用 LiveData / Flow 进行数据驱动 UI 更新。

UI 构建:

XML Layouts (ConstraintLayout, RecyclerView)

ViewBinding (替代 findViewById)

Material Design Components

核心组件:

Coroutines: 异步任务处理（如 IO 线程读取相册）。

MediaStore API: 系统级媒体资源查询。

Fragment: 单 Activity 多 Fragment 架构。

📱 关键适配亮点 (Key Adaptations)
1. Android 14 权限适配 (Partial Access)
针对 Android 14 引入的隐私变更，项目实现了完整的权限请求闭环：

兼容 READ_MEDIA_VISUAL_USER_SELECTED 权限。

在用户选择“部分允许”后，提供“管理已选照片”入口，允许用户二次修改授权范围。

统一权限校验逻辑，兼容“全权”与“部分权限”两种状态。

2. 厂商兼容性 (Vendor Compatibility)
解决小米、华为等设备在文件路径读取上的差异：

全面弃用 DATA 字段，转而使用 ContentUris.withAppendedId 生成标准 Uri。

修复了不同 ROM 下权限拒绝后的回调处理 Bug。

📅 更新日志 (Changelog)
v1.2.0 (Latest) - 核心功能完善与适配
Feat: 完善相册页，提取 MediaLoader 工具类，实现 Android 14 权限深度适配。

Fix: 修复不同权限状态下相片获取失败的 Bug。

Feat: 增加“批量修图”模式入口及底部选图栏 UI。

Refactor: 全面重构 PhotoSelectionActivity 和 CustomCameraActivity 至 MVVM 架构。

v1.1.0 - 架构重构
Refactor: 引入 HomeViewModel，解耦主界面逻辑。

Feat: 将硬编码字符串提取为枚举类型，优化代码可维护性。

UI: 拍照界面添加缩略图预览。

v1.0.0 - 基础 UI 构建
UI: 完成主界面 Fragment 化改造，添加“推荐照片”与底部状态栏。

UI: 确立品牌色调（荧光绿），实现仿醒图首页布局。

Dev: 引入 ViewBinding 替代旧式绑定。

🚀 待办事项 (Todo)
[ ] 修图核心功能: 完成图片编辑界面的滤镜、调节功能的具体实现。

[ ] 自定义 View: 开发高性能的图片触摸视图（缩放、移动、贴纸）。

[ ] 特效引擎: 接入滤镜算法或 LUT 支持。

[ ] 项目解耦: 进一步优化模块间依赖。
