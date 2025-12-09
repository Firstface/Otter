# Otter - 高性能仿 “醒图 (Hypic)” 的 Android 客户端

![Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp)

![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)
![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-blue.svg)
![OpenGL ES](https://img.shields.io/badge/Rendering-OpenGL_ES_2.0-purple.svg)
![CameraX](https://img.shields.io/badge/Jetpack-CameraX-red.svg)

## 📖 项目简介 (Introduction)
**Otter** 是一款基于 Android 原生技术栈开发的图片编辑与美化应用，旨在深度复刻 **醒图 (Hypic)** 的 UI 风格与核心交互体验。

项目采用现代化 **MVVM 架构**，并实现：
- Android 14+ 媒体权限适配  
- OpenGL ES 2.0 高性能渲染引擎  
- 自定义裁剪、画笔、缩放旋转手势  
- 批量修图 + 高度定制 UI  

## ✨ 核心功能 (Features)

### 🎨 1. 沉浸式 UI 体验
- 还原醒图视觉风格（Neon Green #CCFF00 + Black）
- 动态首页（RecyclerView 多类型 Item）
- 自定义底部 Tab、沉浸式状态栏
- 支持深色模式

### ⚡ 2. 高性能修图引擎
- OpenGL ES 2.0 渲染管线（GLSurfaceView + PhotoRenderer）
- GPU 加速图像显示与矩阵变换
- 支持手势缩放、旋转、移动
- 自定义 CropOverlayView（裁剪）
- 自定义 DrawingOverlayView（画笔）
- Undo / Redo 历史堆栈

### 🖼️ 3. 智能媒体管理
- 单选修图 / 批量修图
- Android 14 Partial Access 权限深度适配
- 完全基于 Scoped Storage（Uri）
- 自定义相册与 CameraX 相机页面

## 🏗 技术栈 (Tech Stack)
| 维度 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM |
| 渲染 | OpenGL ES 2.0 |
| UI | ViewBinding + RecyclerView |
| 异步 | Kotlin Coroutines |
| 媒体管理 | MediaStore API |
| 相机 | CameraX |
| 自定义组件 | CropOverlayView / DrawingOverlayView |

## 📱 技术难点 (Highlights)
### ✔ Android 14 部分照片权限适配
- READ_MEDIA_VISUAL_USER_SELECTED  
- Partial Access 重新授权入口  
- MediaLoader 统一 Android 10–14 权限行为  

### ✔ OpenGL 坐标系映射
- Screen ↔ Texture 的双向转换  
- 裁剪遮罩与 GL 图像实时同步  
- 手势矩阵稳定性优化  

### ✔ 厂商兼容性
- 弃用 `_data` 字段  
- 统一使用 ContentUris 与 Uri  

## 📂 项目结构 (Project Structure)
```
com.example.otter
├── adapter/
├── model/
├── renderer/
├── ui/
├── util/
├── view/
├── viewmodel/
└── ...Activities
```

## 📅 更新日志 (Changelog)
### v1.3.0
- GLSurfaceView 修图引擎  
- 裁剪功能  
- 画笔 + Undo/Redo  
- 修复保存照片权限问题  

### v1.2.0
- Android 14 权限适配  
- 批量修图  
- 相册与相机 MVVM 重构  

### v1.1.0
- 引入 HomeViewModel  
- 枚举替代硬编码  

### v1.0.0
- 首版 UI  
- 品牌视觉体系  

## 🛠 快速开始 (Getting Started)
### 构建
```
git clone https://github.com/your_username/Otter.git
cd Otter
./gradlew installDebug
```

## 📎 作者
Created by V_E for Hypic Android Training Camp.
