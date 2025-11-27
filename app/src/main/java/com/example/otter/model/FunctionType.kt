package com.example.otter.model

enum class FunctionType(val displayName: String) {
    RANDOM("随机"),
    EDIT("修图"),
    VIDEO_EDIT("视频剪辑"),
    IMPORT("导入"),
    LIVE_PHOTO_EDIT("修实况Live"),
    PORTRAIT_BEAUTY("人像美化"),
    COLLAGE("拼图"),
    BATCH_EDIT("批量修图"),
    SUPER_RESOLUTION("画质超清"),
    MAGIC_ERASER("魔法消除"),
    SMART_CUTOUT("智能抠图"),
    ONE_CLICK_MOVIE("一键出片"),
    ONE_CLICK_BEAUTY("一键美化"),
}