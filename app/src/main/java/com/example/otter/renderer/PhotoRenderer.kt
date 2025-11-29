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

class PhotoRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // 简单的顶点着色器
    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 aTexCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  vTexCoord = aTexCoord;" +
                "}"

    // 片段着色器
    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform sampler2D sTexture;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_FragColor = texture2D(sTexture, vTexCoord);" +
                "}"

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId: Int = 0

    // 持有待加载的 Bitmap
    private var pendingBitmap: Bitmap? = null
    // 当前渲染的 Bitmap (用于获取宽高)
    var currentBitmap: Bitmap? = null
        private set

    // 视图宽高 (屏幕像素)
    private var viewWidth = 0f
    private var viewHeight = 0f

    // Handles
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureUniformHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    // Matrices
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // --- 公开属性 (由 Activity 手势控制) ---
    // 缩放倍数
    @Volatile var scaleFactor = 1.0f
    // 平移距离 (单位：像素 Pixels)
    @Volatile var translationX = 0.0f
    @Volatile var translationY = 0.0f

    // 定义一个单位正方形 (0,0) -> (1,1)
    // 我们将在 ModelMatrix 中将其缩放到图片的实际像素大小
    private val squareCoords = floatArrayOf(
        0.0f, 0.0f,   // Top left
        0.0f, 1.0f,   // Bottom left
        1.0f, 1.0f,   // Bottom right
        1.0f, 0.0f    // Top right
    )

    // 纹理坐标 (Android Bitmap 左上角是 0,0)
    private val texCoords = floatArrayOf(
        0.0f, 0.0f,   // Top Left
        0.0f, 1.0f,   // Bottom Left
        1.0f, 1.0f,   // Bottom Right
        1.0f, 0.0f    // Top Right
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f) // 黑色背景
        setupBuffers()
        setupShaders()
        setupTexture()

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "sTexture")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()

        // 【关键】使用像素投影 (Pixel Projection)
        // 左上角为 (0,0)，X向右，Y向下。完全匹配 Android 屏幕坐标系。
        Matrix.orthoM(projectionMatrix, 0, 0f, viewWidth, viewHeight, 0f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 1. 加载新 Bitmap (如果 Activity 更新了图片)
        pendingBitmap?.let {
            loadTextureFromBitmap(it)
            currentBitmap = it
            pendingBitmap = null
        }

        val bitmap = currentBitmap ?: return

        // 2. 计算 Fit Center 基础缩放 (让图片完整显示在屏幕内)
        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = if (widthRatio < heightRatio) widthRatio else heightRatio

        // 3. 计算图片当前的真实显示尺寸 (基础缩放 * 手势缩放)
        val drawWidth = bitmap.width * baseScale * scaleFactor
        val drawHeight = bitmap.height * baseScale * scaleFactor

        // 4. 计算绘制起始点 (Top-Left)
        // 逻辑：屏幕中心 + 平移偏移 - 图片一半宽高
        val startX = (viewWidth - drawWidth) / 2f + translationX
        val startY = (viewHeight - drawHeight) / 2f + translationY

        // 5. 设置 Model Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // 移到位置
        Matrix.translateM(modelMatrix, 0, startX, startY, 0f)
        // 缩放到像素大小
        Matrix.scaleM(modelMatrix, 0, drawWidth, drawHeight, 1f)

        // 6. 组合矩阵: MVP = Projection * Model
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        // 7. 绘制
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /**
     * Activity 调用此方法更新图片
     */
    fun updateBitmap(newBitmap: Bitmap) {
        // 重置状态
        scaleFactor = 1.0f
        translationX = 0.0f
        translationY = 0.0f
        this.pendingBitmap = newBitmap
    }

    private fun setupBuffers() {
        var bb = ByteBuffer.allocateDirect(squareCoords.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply { put(squareCoords); position(0) }
        bb = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder())
        texCoordBuffer = bb.asFloatBuffer().apply { put(texCoords); position(0) }
    }

    private fun setupShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun setupTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun loadTextureFromBitmap(bitmap: Bitmap) {
        try {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}