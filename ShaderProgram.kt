package com.example.game3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Mesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 6
    private val buffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun bindAndDraw(positionHandle: Int, normalHandle: Int) {
        val stride = 6 * 4
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(3)
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, stride, buffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}

class ShaderProgram {
    val programId: Int
    val aPosition: Int
    val aNormal: Int
    val uMVPMatrix: Int
    val uModelMatrix: Int
    val uColor: Int
    val uEmissive: Int
    val uFogColor: Int
    val uFogDensity: Int
    val uLightDir: Int

    init {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
        }
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        uMVPMatrix = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uColor = GLES20.glGetUniformLocation(programId, "uColor")
        uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uLightDir = GLES20.glGetUniformLocation(programId, "uLightDir")
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vWorldNormal;
            varying float vDepth;
            void main() {
                vec4 pos = uMVPMatrix * vec4(aPosition, 1.0);
                gl_Position = pos;
                vDepth = pos.z;
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform float uEmissive;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uLightDir;
            varying vec3 vWorldNormal;
            varying float vDepth;
            void main() {
                vec3 n = normalize(vWorldNormal);
                float diff = max(dot(n, normalize(uLightDir)), 0.0);
                float hemi = 0.42 + 0.14 * n.y;
                float shade = clamp(hemi + diff * 0.65 + uEmissive, 0.0, 1.45);
                vec3 litColor = uColor.rgb * shade;
                float fogFactor = 1.0 - exp(-pow(max(vDepth, 0.0) * uFogDensity, 1.6));
                fogFactor = clamp(fogFactor, 0.0, 1.0);
                vec3 finalRgb = mix(litColor, uFogColor, fogFactor * (1.0 - min(uEmissive, 0.8)));
                gl_FragColor = vec4(finalRgb, uColor.a);
            }
        """
    }
}
