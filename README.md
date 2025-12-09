# Otter 🦦

> 一个基于现代 Android 技术栈（Kotlin + MVVM + Version Catalog）构建的高性能图片编辑与相机应用。

![Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp)

![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)
![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-blue.svg)
![OpenGL ES](https://img.shields.io/badge/Rendering-OpenGL_ES_2.0-purple.svg)
![CameraX](https://img.shields.io/badge/Jetpack-CameraX-red.svg)

## 📖 项目简介

**Otter** 是一款专注于流畅体验的 Android 图像处理应用。它不仅复刻了主流修图应用（如醒图/Hypic）的核心流程，还在工程架构上采用了最新的 Android 开发规范。

用户可以使用 Otter 进行自定义相机拍摄，从本地相册快速加载图片，并使用专业的编辑工具（如智能裁剪、自由涂鸦）进行创作。

项目采用现代化 **MVVM 架构**，并实现：
- Android 14+ 媒体权限适配  
- OpenGL ES 2.0 高性能渲染引擎  
- 自定义裁剪、画笔、缩放旋转手势  
- 批量修图 + 高度定制 UI  


## ✨ 核心功能

### 📸 1. 专业相机 (Pro Camera)
基于 **CameraX** 构建的自定义相机，提供比系统相机更灵活的交互。
* **极速预览**：基于 `ProcessCameraProvider` 实现低延迟预览。
* **沉浸式拍摄**：支持前后置镜头切换、闪光灯控制及手势对焦。
* **生命周期感知**：完美处理相机资源的释放与重连。

### 🎨 2. 图片工作台 (Creative Editor)
全功能的图片编辑画布，支持多图层叠加操作。
* **智能裁剪**：内置 `CropOverlayView`，支持自由比例与固定比例（1:1, 4:3, 16:9）裁剪。
* **创意涂鸦**：基于 Canvas 的 `DrawingOverlayView`，支持自定义画笔颜色与粗细，流畅绘制。
* **实时渲染**：高效的图片处理引擎，编辑效果实时可见。

### 🖼️ 3. 智能相册 (Smart Gallery)
* **高效加载**：使用 `ContentResolver` + 协程异步加载本地媒体文件。
* **相册分类**：自动扫描设备文件夹，提供清晰的分类浏览体验。
* **多选模式**：支持批量选择图片导入编辑。

---

## 🛠️ 技术栈与架构

本项目严格遵循现代 Android 开发的最佳实践：

* **语言**: [Kotlin](https://kotlinlang.org/) (100% 纯 Kotlin 代码)
* **架构模式**: **MVVM** (Model-View-ViewModel)，实现 UI 与逻辑分离。
* **构建系统**: **Gradle Version Catalog** (`libs.versions.toml`)
    * 统一管理所有依赖版本，便于维护和升级。
* **UI 技术**:
    * **ViewBinding**: 类型安全的视图交互。
    * **Custom Views**: 自定义 `View` 实现复杂的裁剪和涂鸦交互。
    * **ConstraintLayout**: 构建复杂的响应式布局。
* **核心库**:
    * **Jetpack CameraX**: 相机支持。
    * **Jetpack Lifecycle**: 生命周期感知。
    * **Glide**: 图片加载与缓存。
    * **Coroutines**: 异步任务处理。

---

## 📂 项目结构

```text
com.example.otter
├── adapter/         # RecyclerView 适配器 (相册、工具栏)
├── model/           # 数据实体模型
├── ui/              # Fragment 页面 (Home, Profile, Recommendation)
├── view/            # 自定义 View (裁剪框、涂鸦板)
├── viewmodel/       # 业务逻辑与状态管理
├── util/            # 工具类 (媒体加载器等)
├── CustomCameraActivity.kt   # 相机容器
├── PhotoEditingActivity.kt   # 编辑器容器
└── PhotoSelectionActivity.kt # 选图容器
```
## 🛠 快速开始 (Getting Started)
### 构建


```
git clone https://github.com/your_username/Otter.git
cd Otter
./gradlew installDebug
```

## 📎 作者
Created by V_E for Hypic Android Training Camp.
