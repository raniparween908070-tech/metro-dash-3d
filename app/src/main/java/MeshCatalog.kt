package com.example.game3d

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshCatalog {
    val cube: Mesh = buildCube()
    val coinDisc: Mesh = buildCylinder(16, 0.5f, 0.16f)
    val diamond: Mesh = buildOctahedron()
    val wheel: Mesh = buildCylinder(12, 0.5f, 0.35f)
    val trainNose: Mesh = buildSlopedNose()
    val sphere: Mesh = buildSphere(10, 12)
    val ramp: Mesh = buildRamp()

    private fun buildCube(): Mesh {
        val data = ArrayList<Float>(36 * 6)
        fun tri(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            data.addAll(listOf(x1, y1, z1, nx, ny, nz))
            data.addAll(listOf(x2, y2, z2, nx, ny, nz))
            data.addAll(listOf(x3, y3, z3, nx, ny, nz))
        }
        fun quad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            tri(ax, ay, az, bx, by, bz, cx, cy, cz, nx, ny, nz)
            tri(ax, ay, az, cx, cy, cz, dx, dy, dz, nx, ny, nz)
        }
        val h = 0.5f
        // Front (+Z)
        quad(-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0f, 0f, 1f)
        // Back (-Z)
        quad(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0f, 0f, -1f)
        // Left (-X)
        quad(-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1f, 0f, 0f)
        // Right (+X)
        quad(h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1f, 0f, 0f)
        // Top (+Y)
        quad(-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0f, 1f, 0f)
        // Bottom (-Y)
        quad(-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0f, -1f, 0f)
        return Mesh(data.toFloatArray())
    }

    private fun buildSlopedNose(): Mesh {
        // Aerodynamic wedge facing +Z (front of train facing player)
        val data = ArrayList<Float>()
        fun addFace(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float
        ) {
            val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
            val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            data.addAll(listOf(x1, y1, z1, nx, ny, nz, x2, y2, z2, nx, ny, nz, x3, y3, z3, nx, ny, nz))
        }
        val x = 0.5f
        val yB = -0.5f; val yT = 0.5f
        val zBack = -0.5f; val zFront = 0.5f; val zTop = 0.05f
        // Sloped windshield front
        addFace(-x, yB, zFront, x, yB, zFront, x, yT, zTop)
        addFace(-x, yB, zFront, x, yT, zTop, -x, yT, zTop)
        // Back face
        addFace(x, yB, zBack, -x, yB, zBack, -x, yT, zBack)
        addFace(x, yB, zBack, -x, yT, zBack, x, yT, zBack)
        // Top
        addFace(-x, yT, zTop, x, yT, zTop, x, yT, zBack)
        addFace(-x, yT, zTop, x, yT, zBack, -x, yT, zBack)
        // Bottom
        addFace(-x, yB, zBack, x, yB, zBack, x, yB, zFront)
        addFace(-x, yB, zBack, x, yB, zFront, -x, yB, zFront)
        // Left
        addFace(-x, yB, zBack, -x, yB, zFront, -x, yT, zTop)
        addFace(-x, yB, zBack, -x, yT, zTop, -x, yT, zBack)
        // Right
        addFace(x, yB, zFront, x, yB, zBack, x, yT, zBack)
        addFace(x, yB, zFront, x, yT, zBack, x, yT, zTop)
        return Mesh(data.toFloatArray())
    }

    private fun buildRamp(): Mesh {
        // Wedge rising from +Z (y=-0.5) up to -Z (y=+0.5)
        val data = ArrayList<Float>()
        fun addFace(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float
        ) {
            val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
            val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            data.addAll(listOf(x1, y1, z1, nx, ny, nz, x2, y2, z2, nx, ny, nz, x3, y3, z3, nx, ny, nz))
        }
        val h = 0.5f
        // Slope top
        addFace(-h, -h, h, h, -h, h, h, h, -h)
        addFace(-h, -h, h, h, h, -h, -h, h, -h)
        // Back wall (-Z)
        addFace(h, -h, -h, -h, -h, -h, -h, h, -h)
        addFace(h, -h, -h, -h, h, -h, h, h, -h)
        // Bottom
        addFace(-h, -h, -h, h, -h, -h, h, -h, h)
        addFace(-h, -h, -h, h, -h, h, -h, -h, h)
        // Sides
        addFace(-h, -h, -h, -h, -h, h, -h, h, -h)
        addFace(h, -h, h, h, -h, -h, h, h, -h)
        return Mesh(data.toFloatArray())
    }

    private fun buildCylinder(segments: Int, radius: Float, depth: Float): Mesh {
        val data = ArrayList<Float>()
        val halfD = depth * 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * radius; val y0 = sin(a0) * radius
            val x1 = cos(a1) * radius; val y1 = sin(a1) * radius
            // Front cap (+Z)
            data.addAll(listOf(0f, 0f, halfD, 0f, 0f, 1f, x0, y0, halfD, 0f, 0f, 1f, x1, y1, halfD, 0f, 0f, 1f))
            // Back cap (-Z)
            data.addAll(listOf(0f, 0f, -halfD, 0f, 0f, -1f, x1, y1, -halfD, 0f, 0f, -1f, x0, y0, -halfD, 0f, 0f, -1f))
            // Rim
            val nx0 = cos(a0); val ny0 = sin(a0)
            val nx1 = cos(a1); val ny1 = sin(a1)
            data.addAll(listOf(x0, y0, halfD, nx0, ny0, 0f, x0, y0, -halfD, nx0, ny0, 0f, x1, y1, -halfD, nx1, ny1, 0f))
            data.addAll(listOf(x0, y0, halfD, nx0, ny0, 0f, x1, y1, -halfD, nx1, ny1, 0f, x1, y1, halfD, nx1, ny1, 0f))
        }
        return Mesh(data.toFloatArray())
    }

    private fun buildOctahedron(): Mesh {
        val data = ArrayList<Float>()
        fun addFace(
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float
        ) {
            val ux = x2 - x1; val uy = y2 - y1; val uz = z2 - z1
            val vx = x3 - x1; val vy = y3 - y1; val vz = z3 - z1
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            data.addAll(listOf(x1, y1, z1, nx, ny, nz, x2, y2, z2, nx, ny, nz, x3, y3, z3, nx, ny, nz))
        }
        val r = 0.42f; val h = 0.62f
        val top = floatArrayOf(0f, h, 0f)
        val bot = floatArrayOf(0f, -h, 0f)
        val ring = arrayOf(
            floatArrayOf(0f, 0f, r),
            floatArrayOf(r, 0f, 0f),
            floatArrayOf(0f, 0f, -r),
            floatArrayOf(-r, 0f, 0f)
        )
        for (i in 0 until 4) {
            val a = ring[i]
            val b = ring[(i + 1) % 4]
            addFace(a[0], a[1], a[2], b[0], b[1], b[2], top[0], top[1], top[2])
            addFace(b[0], b[1], b[2], a[0], a[1], a[2], bot[0], bot[1], bot[2])
        }
        return Mesh(data.toFloatArray())
    }

    private fun buildSphere(stacks: Int, slices: Int): Mesh {
        val data = ArrayList<Float>()
        for (i in 0 until stacks) {
            val phi0 = (PI * i / stacks - PI / 2).toFloat()
            val phi1 = (PI * (i + 1) / stacks - PI / 2).toFloat()
            for (j in 0 until slices) {
                val th0 = (2.0 * PI * j / slices).toFloat()
                val th1 = (2.0 * PI * (j + 1) / slices).toFloat()
                fun v(phi: Float, th: Float): FloatArray {
                    val x = cos(phi) * cos(th)
                    val y = sin(phi)
                    val z = cos(phi) * sin(th)
                    return floatArrayOf(x * 0.5f, y * 0.5f, z * 0.5f, x, y, z)
                }
                val v00 = v(phi0, th0); val v10 = v(phi1, th0)
                val v11 = v(phi1, th1); val v01 = v(phi0, th1)
                data.addAll(v00.toList()); data.addAll(v10.toList()); data.addAll(v11.toList())
                data.addAll(v00.toList()); data.addAll(v11.toList()); data.addAll(v01.toList())
            }
        }
        return Mesh(data.toFloatArray())
    }
}
