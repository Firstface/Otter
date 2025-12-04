package com.example.otter.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 图片渲染器 (OpenGL ES 2.0)
 *
 * 功能：
 * 1. 在 GLSurfaceView 中渲染 Bitmap。
 * 2. 支持通过矩阵变换实现缩放 (Scale) 和平移 (Translate)。
 * 3. 支持通过 Fragment Shader 实现亮度调节。
 */
class PhotoRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // --- Shader 代码 ---

    // 顶点着色器：负责顶点坐标变换和纹理坐标传递
    // uMVPMatrix: 模型视图投影矩阵，决定绘制的位置和大小
    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 aTexCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  vTexCoord = aTexCoord;" +
                "}"

    // 片元着色器：负责像素着色
    // uBrightness: 亮度增量，直接叠加在 RGB 颜色上
    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform sampler2D sTexture;" +
                "varying vec2 vTexCoord;" +
                "uniform float uBrightness;" +
                "void main() {" +
                "  vec4 color = texture2D(sTexture, vTexCoord);" +
                "  color.rgb += uBrightness;" + // 简单的亮度调节算法
                "  gl_FragColor = color;" +
                "}"

    // --- OpenGL 数据缓冲 ---
    private lateinit var vertexBuffer: FloatBuffer // 顶点坐标数据
    private lateinit var texCoordBuffer: FloatBuffer // 纹理坐标数据

    // --- OpenGL 句柄 (Handles) ---
    private var program: Int = 0 // 编译链接后的 Shader 程序 ID
    private var textureId: Int = 0 // 纹理 ID

    // --- 状态管理 ---
    private var surfaceJustCreated = false // 标记 Surface 是否刚被创建/重建
    private var pendingBitmap: Bitmap? = null // 等待加载到 GPU 的新图片
    var currentBitmap: Bitmap? = null
        private set

    // --- 视图尺寸 ---
    private var viewWidth = 0f
    private var viewHeight = 0f

    // --- Shader 变量句柄 ---
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureUniformHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var brightnessHandle: Int = 0

    // --- 矩阵 (Matrix) ---
    // mvpMatrix = projectionMatrix * modelMatrix
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16) // 投影矩阵 (将 OpenGL 坐标映射到屏幕)
    private val modelMatrix = FloatArray(16)      // 模型矩阵 (处理图片本身的移动、缩放)

    // --- 变换参数 (由外部控制) ---
    @Volatile var scaleFactor = 1.0f  // 缩放比例
    @Volatile var translationX = 0.0f // X轴平移量
    @Volatile var translationY = 0.0f // Y轴平移量
    @Volatile var brightness = 0.0f   // 亮度值 (-1.0 到 1.0)

    // --- 基础几何数据 ---
    // 定义一个单位正方形 (0,0) -> (1,1)
    // 后续会通过矩阵将其拉伸成图片的实际大小
    private val squareCoords = floatArrayOf(
        0.0f, 0.0f,   // 左上
        0.0f, 1.0f,   // 左下
        1.0f, 1.0f,   // 右下
        1.0f, 0.0f    // 右上
    )

    // 纹理坐标 (对应上面的顶点，定义图片如何贴在这个正方形上)
    // OpenGL 纹理坐标系：(0,0)左上 -> (1,1)右下 (视具体坐标系定义而定，Android Bitmap通常左上为原点)
    private val texCoords = floatArrayOf(
        0.0f, 0.0f,   // 左上
        0.0f, 1.0f,   // 左下
        1.0f, 1.0f,   // 右下
        1.0f, 0.0f    // 右上
    )

    /**
     * Surface 创建时调用
     * 初始化 OpenGL 环境，编译 Shader，生成纹理 ID
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景清除颜色为黑色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 初始化数据缓冲和 Shader
        setupBuffers()
        setupShaders()
        setupTexture()

        // 获取 Shader 中变量的句柄 (用于后续传值)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "sTexture")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        brightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness")

        surfaceJustCreated = true
    }

    /**
     * Surface 尺寸改变时调用 (例如横竖屏切换)
     * 在这里设置投影矩阵
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()

        // 设置正交投影 (Orthographic Projection)
        // 这使得我们可以直接使用屏幕像素坐标系进行计算，而不是 OpenGL 的 -1 到 1 坐标系
        // 坐标系范围：左上(0,0) -> 右下(width, height)
        Matrix.orthoM(projectionMatrix, 0, 0f, viewWidth, viewHeight, 0f, -1f, 1f)
    }

    /**
     * 每一帧绘制时调用
     * 核心渲染逻辑：计算变换 -> 传值给 Shader -> 绘制
     */
    override fun onDrawFrame(gl: GL10?) {
        // 1. 清除屏幕
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 2. 纹理加载逻辑 (必须在 GL 线程执行)
        val newBitmapToLoad = pendingBitmap
        if (newBitmapToLoad != null) {
            // 有新图片，加载它
            currentBitmap = newBitmapToLoad
            loadTextureFromBitmap(newBitmapToLoad)
            pendingBitmap = null
            surfaceJustCreated = false
        } else if (surfaceJustCreated) {
            // Surface 重建了 (如从后台切回)，需要重新上传当前图片
            currentBitmap?.let {
                loadTextureFromBitmap(it)
            }
            surfaceJustCreated = false
        }

        val bitmap = currentBitmap ?: return

        // 3. 计算“Fit Center” (保持比例居中) 的显示大小
        // 计算宽和高的缩放比，取较小值以保证图片完整显示在屏幕内
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)

        // 图片在屏幕上的实际绘制尺寸 (未包含用户的手势缩放)
        val drawWidth = bitmap.width * baseScale * scaleFactor
        val drawHeight = bitmap.height * baseScale * scaleFactor

        // 4. 计算起始坐标 (居中 + 用户平移)
        val startX = (viewWidth - drawWidth) / 2f + translationX
        val startY = (viewHeight - drawHeight) / 2f + translationY

        // 5. 设置模型矩阵 (Model Matrix)
        // 顺序很重要：先重置 -> 平移 -> 缩放
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, startX, startY, 0f)
        // 将单位正方形 (1x1) 缩放为实际绘制大小 (drawWidth x drawHeight)
        Matrix.scaleM(modelMatrix, 0, drawWidth, drawHeight, 1f)

        // 6. 计算 MVP 矩阵 (Projection * Model)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        // 7. 使用 Shader 程序
        GLES20.glUseProgram(program)

        // 8. 传递 Uniform 变量 (矩阵、亮度)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(brightnessHandle, brightness)

        // 9. 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)

        // 10. 传递 Attribute 变量 (顶点坐标、纹理坐标)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        // 11. 执行绘制 (绘制一个矩形面)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        // 12. 清理状态
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /**
     * 更新当前显示的图片
     * @param newBitmap 新的 Bitmap 对象
     * @param resetTransform 是否重置缩放和平移状态
     */
    fun updateBitmap(newBitmap: Bitmap, resetTransform: Boolean) {
        if (resetTransform) {
            scaleFactor = 1.0f
            translationX = 0.0f
            translationY = 0.0f
            brightness = 0.0f
        }
        // 将 Bitmap 存入 pending，等待在 GL 线程 (onDrawFrame) 中上传
        this.pendingBitmap = newBitmap
    }

    // --- 辅助方法 ---

    // 初始化顶点和纹理坐标 Buffer (将 Java 数组转为 native ByteBuffer)
    private fun setupBuffers() {
        var bb = ByteBuffer.allocateDirect(squareCoords.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply { put(squareCoords); position(0) }

        bb = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder())
        texCoordBuffer = bb.asFloatBuffer().apply { put(texCoords); position(0) }
    }

    // 编译并链接 Shader 程序
    private fun setupShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    // 生成并配置纹理对象
    private fun setupTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 设置纹理过滤参数 (Linear: 线性插值，图像缩放时更平滑)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        // 设置纹理环绕参数 (Clamp to Edge: 边缘拉伸，防止出现黑边)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    // 将 Bitmap 上传到 GPU 纹理
    private fun loadTextureFromBitmap(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            return
        }
        try {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            // 核心方法：将 Android Bitmap 数据复制到当前绑定的 OpenGL 纹理中
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 编译单个 Shader
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}