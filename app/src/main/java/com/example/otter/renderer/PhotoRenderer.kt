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

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 aTexCoord;" +
                "varying vec2 vTexCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  vTexCoord = aTexCoord;" +
                "}"

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
    private var surfaceJustCreated = false

    private var pendingBitmap: Bitmap? = null
    var currentBitmap: Bitmap? = null
        private set

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureUniformHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    @Volatile var scaleFactor = 1.0f
    @Volatile var translationX = 0.0f
    @Volatile var translationY = 0.0f

    private val squareCoords = floatArrayOf(
        0.0f, 0.0f,   // Top left
        0.0f, 1.0f,   // Bottom left
        1.0f, 1.0f,   // Bottom right
        1.0f, 0.0f    // Top right
    )

    private val texCoords = floatArrayOf(
        0.0f, 0.0f,   // Top Left
        0.0f, 1.0f,   // Bottom Left
        1.0f, 1.0f,   // Bottom Right
        1.0f, 0.0f    // Top Right
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        setupBuffers()
        setupShaders()
        setupTexture()

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "sTexture")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        surfaceJustCreated = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        Matrix.orthoM(projectionMatrix, 0, 0f, viewWidth, viewHeight, 0f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val newBitmapToLoad = pendingBitmap
        if (newBitmapToLoad != null) {
            currentBitmap = newBitmapToLoad
            loadTextureFromBitmap(newBitmapToLoad)
            pendingBitmap = null
            surfaceJustCreated = false
        } else if (surfaceJustCreated) {
            currentBitmap?.let {
                loadTextureFromBitmap(it)
            }
            surfaceJustCreated = false
        }

        val bitmap = currentBitmap ?: return

        val widthRatio = viewWidth / bitmap.width
        val heightRatio = viewHeight / bitmap.height
        val baseScale = minOf(widthRatio, heightRatio)

        val drawWidth = bitmap.width * baseScale * scaleFactor
        val drawHeight = bitmap.height * baseScale * scaleFactor

        val startX = (viewWidth - drawWidth) / 2f + translationX
        val startY = (viewHeight - drawHeight) / 2f + translationY

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, startX, startY, 0f)
        Matrix.scaleM(modelMatrix, 0, drawWidth, drawHeight, 1f)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

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

    fun updateBitmap(newBitmap: Bitmap, resetTransform: Boolean) {
        if (resetTransform) {
            scaleFactor = 1.0f
            translationX = 0.0f
            translationY = 0.0f
        }
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
        if (bitmap.isRecycled) {
            return
        }
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
