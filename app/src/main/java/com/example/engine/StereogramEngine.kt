package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.DepthStroke
import com.example.model.PatternType
import com.example.model.ShapeType
import com.example.model.StereogramConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object StereogramEngine {

    data class GenerationResult(
        val stereogramBitmap: Bitmap,
        val depthPreviewBitmap: Bitmap
    )

    fun renderDepthPreview(
        config: StereogramConfig,
        customDepthBitmap: Bitmap?,
        strokes: List<DepthStroke>,
        typeface: Typeface?
    ): Bitmap {
        val rawBitmap = drawRawShapeBitmap(
            width = config.width,
            height = config.height,
            shapeType = config.shapeType,
            text = config.text,
            customDepthBitmap = customDepthBitmap,
            strokes = strokes,
            typeface = typeface
        )
        if (config.blurRadius <= 0 && !config.invertDepth) {
            return rawBitmap
        }
        val w = config.width
        val h = config.height
        val depth = extractAndBlurDepth(rawBitmap, w, h, config.blurRadius)
        val previewPixels = IntArray(w * h)
        for (i in depth.indices) {
            var v = depth[i]
            if (config.invertDepth) v = 1f - v
            val gray = (v * 255f).roundToInt().coerceIn(0, 255)
            previewPixels[i] = Color.rgb(gray, gray, gray)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(previewPixels, 0, w, 0, 0, w, h)
        return out
    }

    fun generate(
        config: StereogramConfig,
        customDepthBitmap: Bitmap?,
        customPatternBitmap: Bitmap?,
        strokes: List<DepthStroke>,
        typeface: Typeface?
    ): GenerationResult {
        val w = config.width.coerceIn(400, 1600)
        val h = config.height.coerceIn(300, 1200)
        val s = config.stripWidth.coerceIn(60, 300)
        val m = min(config.depthMultiplier.coerceIn(4, 70), s - 2)
        val inv = config.invertDepth

        val rawDepthBitmap = drawRawShapeBitmap(
            width = w,
            height = h,
            shapeType = config.shapeType,
            text = config.text,
            customDepthBitmap = customDepthBitmap,
            strokes = strokes,
            typeface = typeface
        )

        val depthArray = extractAndBlurDepth(rawDepthBitmap, w, h, config.blurRadius)

        // Build blurred depth preview bitmap so the user sees exact blurred/inverted depth
        val previewPixels = IntArray(w * h)
        for (i in depthArray.indices) {
            var v = depthArray[i]
            if (inv) v = 1f - v
            val gray = (v * 255f).roundToInt().coerceIn(0, 255)
            previewPixels[i] = Color.rgb(gray, gray, gray)
        }
        val depthPreviewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        depthPreviewBitmap.setPixels(previewPixels, 0, w, 0, 0, w, h)

        val tile = makeTile(s, config.patternType, customPatternBitmap)
        val outPixels = IntArray(w * h)

        for (y in 0 until h) {
            val ty = (y % s) * s
            val rowOffset = y * w
            for (x in 0 until w) {
                val i = rowOffset + x
                if (x < s) {
                    outPixels[i] = tile[ty + x]
                } else {
                    var v = depthArray[i]
                    if (inv) v = 1f - v
                    val srcX = (x - s + (v * m).roundToInt()).coerceIn(0, x - 1)
                    outPixels[i] = outPixels[rowOffset + srcX]
                }
            }
        }

        val stereogramBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        stereogramBitmap.setPixels(outPixels, 0, w, 0, 0, w, h)

        if (config.showGuideDots) {
            val canvas = Canvas(stereogramBitmap)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = 2.5f
            }
            val leftDotX = w / 2f - s / 2f
            val rightDotX = w / 2f + s / 2f
            val dotY = 22f
            val dotRadius = 8f

            canvas.drawCircle(leftDotX, dotY, dotRadius, fillPaint)
            canvas.drawCircle(leftDotX, dotY, dotRadius, strokePaint)
            canvas.drawCircle(rightDotX, dotY, dotRadius, fillPaint)
            canvas.drawCircle(rightDotX, dotY, dotRadius, strokePaint)
        }

        return GenerationResult(
            stereogramBitmap = stereogramBitmap,
            depthPreviewBitmap = depthPreviewBitmap
        )
    }

    private fun drawRawShapeBitmap(
        width: Int,
        height: Int,
        shapeType: ShapeType,
        text: String,
        customDepthBitmap: Bitmap?,
        strokes: List<DepthStroke>,
        typeface: Typeface?
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val cx = width / 2f
        val cy = height / 2f
        val m = min(width, height).toFloat()

        when (shapeType) {
            ShapeType.TEXT -> {
                val content = text.ifBlank { " " }
                var fs = height * 0.5f
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(typeface ?: Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = fs
                val measuredWidth = paint.measureText(content)
                if (measuredWidth > width * 0.8f && measuredWidth > 0f) {
                    fs *= (width * 0.8f) / measuredWidth
                    paint.textSize = fs
                }
                val fontMetrics = paint.fontMetrics
                val baselineY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(content, cx, baselineY, paint)
            }

            ShapeType.CIRCLE -> {
                canvas.drawCircle(cx, cy, m * 0.32f, paint)
            }

            ShapeType.STAR -> {
                val path = createStarPath(cx, cy + m * 0.03f, m * 0.4f, m * 0.17f, 5)
                canvas.drawPath(path, paint)
            }

            ShapeType.HEART -> {
                val scale = m * 0.016f
                val path = Path()
                var first = true
                var a = 0.0
                while (a < 6.3) {
                    val x = (16.0 * sin(a).pow(3.0)).toFloat() * scale + cx
                    val y = (-(13.0 * cos(a) - 5.0 * cos(2.0 * a) - 2.0 * cos(3.0 * a) - cos(4.0 * a))).toFloat() * scale + (cy - m * 0.05f)
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    a += 0.02
                }
                path.close()
                canvas.drawPath(path, paint)
            }

            ShapeType.DIAMOND -> {
                // Multi-layered 3D faceted diamond
                val outerR = m * 0.40f
                val midR = m * 0.28f
                val innerR = m * 0.15f

                // Outer facet ring
                paint.color = Color.rgb(110, 110, 110)
                canvas.drawPath(createPolygonPath(cx, cy, outerR, 8, rotationRad = PI.toFloat() / 8f), paint)

                // Middle rotated diamond facet
                paint.color = Color.rgb(185, 185, 185)
                canvas.drawPath(createPolygonPath(cx, cy, midR, 4, rotationRad = 0f), paint)

                // Inner bright table facet
                paint.color = Color.WHITE
                canvas.drawPath(createPolygonPath(cx, cy, innerR, 8, rotationRad = 0f), paint)
            }

            ShapeType.CUBE -> {
                // Isometric 3D cube with distinct depth planes
                val r = m * 0.38f
                val topX = cx
                val topY = cy - r
                val rightX = cx + r * cos(PI / 6.0).toFloat()
                val rightY = cy - r * sin(PI / 6.0).toFloat()
                val bottomRightX = rightX
                val bottomRightY = cy + r * sin(PI / 6.0).toFloat()
                val bottomX = cx
                val bottomY = cy + r
                val bottomLeftX = cx - r * cos(PI / 6.0).toFloat()
                val bottomLeftY = bottomRightY
                val leftX = bottomLeftX
                val leftY = rightY

                // Top face (brightest / closest)
                val topFace = Path().apply {
                    moveTo(cx, cy)
                    lineTo(leftX, leftY)
                    lineTo(topX, topY)
                    lineTo(rightX, rightY)
                    close()
                }
                paint.color = Color.WHITE
                canvas.drawPath(topFace, paint)

                // Left face (medium depth)
                val leftFace = Path().apply {
                    moveTo(cx, cy)
                    lineTo(bottomX, bottomY)
                    lineTo(bottomLeftX, bottomLeftY)
                    lineTo(leftX, leftY)
                    close()
                }
                paint.color = Color.rgb(180, 180, 180)
                canvas.drawPath(leftFace, paint)

                // Right face (deeper shade)
                val rightFace = Path().apply {
                    moveTo(cx, cy)
                    lineTo(rightX, rightY)
                    lineTo(bottomRightX, bottomRightY)
                    lineTo(bottomX, bottomY)
                    close()
                }
                paint.color = Color.rgb(115, 115, 115)
                canvas.drawPath(rightFace, paint)
            }

            ShapeType.SPHERE -> {
                val radius = m * 0.36f
                val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cx - radius * 0.18f,
                        cy - radius * 0.18f,
                        radius * 1.15f,
                        intArrayOf(Color.WHITE, Color.rgb(190, 190, 190), Color.rgb(60, 60, 60), Color.BLACK),
                        floatArrayOf(0f, 0.5f, 0.88f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, radius, spherePaint)
            }

            ShapeType.RIPPLE -> {
                val maxR = m * 0.44f
                val rings = 4
                for (i in rings downTo 1) {
                    val ratio = i.toFloat() / rings
                    val r = maxR * ratio
                    val shade = if (i % 2 == 1) 255 - (i - 1) * 35 else 90
                    paint.color = Color.rgb(shade, shade, shade)
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }

            ShapeType.DRAW -> {
                if (strokes.isEmpty()) {
                    // Draw a subtle starter ring if user hasn't drawn yet
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = m * 0.07f
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawCircle(cx, cy, m * 0.25f, strokePaint)
                } else {
                    for (stroke in strokes) {
                        if (stroke.points.isEmpty()) continue
                        val gray = (stroke.depthValue * 255f).roundToInt().coerceIn(0, 255)
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(gray, gray, gray)
                            style = Paint.Style.STROKE
                            strokeWidth = max(6f, stroke.strokeWidthRatio * m)
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                        if (stroke.points.size == 1) {
                            val pt = stroke.points.first()
                            strokePaint.style = Paint.Style.FILL
                            canvas.drawCircle(
                                pt.x * width,
                                pt.y * height,
                                max(3f, stroke.strokeWidthRatio * m / 2f),
                                strokePaint
                            )
                        } else {
                            val path = Path()
                            stroke.points.forEachIndexed { idx, pt ->
                                val px = pt.x * width
                                val py = pt.y * height
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            canvas.drawPath(path, strokePaint)
                        }
                    }
                }
            }

            ShapeType.UPLOAD -> {
                if (customDepthBitmap != null) {
                    val k = min(
                        width.toFloat() / customDepthBitmap.width.toFloat(),
                        height.toFloat() / customDepthBitmap.height.toFloat()
                    )
                    val dstW = customDepthBitmap.width * k
                    val dstH = customDepthBitmap.height * k
                    val left = (width - dstW) / 2f
                    val top = (height - dstH) / 2f
                    val dstRect = RectF(left, top, left + dstW, top + dstH)
                    canvas.drawBitmap(customDepthBitmap, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    // Fallback star if image not picked yet
                    val path = createStarPath(cx, cy + m * 0.03f, m * 0.36f, m * 0.16f, 5)
                    canvas.drawPath(path, paint)
                }
            }
        }

        return bitmap
    }

    private fun extractAndBlurDepth(bitmap: Bitmap, w: Int, h: Int, radius: Int): FloatArray {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val depth = FloatArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            // Support both grayscale and color uploaded depth maps
            depth[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }
        return boxBlur(depth, w, h, radius)
    }

    /**
     * Exact port of the 2-pass separable boxBlur(d, W, H, r) from the user's HTML/JS code.
     */
    fun boxBlur(d: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        if (r < 1) return d
        val t = FloatArray(d.size)
        val windowSize = (2 * r + 1).toFloat()

        repeat(2) {
            // Horizontal pass
            for (y in 0 until h) {
                var s = 0f
                val o = y * w
                for (x in -r..r) {
                    val clampedX = min(w - 1, max(0, x))
                    s += d[o + clampedX]
                }
                for (x in 0 until w) {
                    t[o + x] = s / windowSize
                    val addIdx = o + min(w - 1, x + r + 1)
                    val subIdx = o + max(0, x - r)
                    s += d[addIdx] - d[subIdx]
                }
            }
            // Vertical pass
            for (x in 0 until w) {
                var s = 0f
                for (y in -r..r) {
                    val clampedY = min(h - 1, max(0, y))
                    s += t[clampedY * w + x]
                }
                for (y in 0 until h) {
                    d[y * w + x] = s / windowSize
                    val addIdx = min(h - 1, y + r + 1) * w + x
                    val subIdx = max(0, y - r) * w + x
                    s += t[addIdx] - t[subIdx]
                }
            }
        }
        return d
    }

    private fun makeTile(
        s: Int,
        patternType: PatternType,
        customPatternBitmap: Bitmap?
    ): IntArray {
        val pixels = IntArray(s * s)
        val rng = Random(System.nanoTime())

        if (patternType == PatternType.UPLOAD && customPatternBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(customPatternBitmap, s, s, true)
            scaled.getPixels(pixels, 0, s, 0, 0, s, s)
            return pixels
        }

        when (patternType) {
            PatternType.RANDOM_COLOR, PatternType.UPLOAD -> {
                for (i in 0 until s * s) {
                    pixels[i] = Color.rgb(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
                }
            }

            PatternType.MONO_DOTS -> {
                for (i in 0 until s * s) {
                    val v = if (rng.nextFloat() < 0.5f) 0 else 255
                    pixels[i] = Color.rgb(v, v, v)
                }
            }

            PatternType.NEON_MATRIX -> {
                val palette = intArrayOf(
                    Color.rgb(9, 11, 17),
                    Color.rgb(0, 229, 255),
                    Color.rgb(255, 46, 147),
                    Color.rgb(124, 77, 255),
                    Color.rgb(0, 230, 118),
                    Color.rgb(24, 32, 58)
                )
                for (y in 0 until s) {
                    for (x in 0 until s) {
                        val gridPeak = (x % 8 == 0 || y % 8 == 0)
                        val baseColor = if (gridPeak && rng.nextFloat() < 0.65f) {
                            palette[1 + rng.nextInt(4)]
                        } else {
                            palette[rng.nextInt(palette.size)]
                        }
                        val jitter = rng.nextInt(-25, 26)
                        val r = (Color.red(baseColor) + jitter).coerceIn(0, 255)
                        val g = (Color.green(baseColor) + jitter).coerceIn(0, 255)
                        val b = (Color.blue(baseColor) + jitter).coerceIn(0, 255)
                        pixels[y * s + x] = Color.rgb(r, g, b)
                    }
                }
            }

            PatternType.GEOMETRIC_MOSAIC -> {
                val cell = max(4, s / 18)
                val cols = (s / cell) + 2
                val rows = (s / cell) + 2
                val cellColors = IntArray(cols * rows) {
                    Color.HSVToColor(
                        floatArrayOf(
                            rng.nextFloat() * 360f,
                            0.65f + rng.nextFloat() * 0.35f,
                            0.45f + rng.nextFloat() * 0.55f
                        )
                    )
                }
                for (y in 0 until s) {
                    for (x in 0 until s) {
                        val cx = x / cell
                        val cy = y / cell
                        val base = cellColors[cy * cols + cx]
                        val grain = rng.nextInt(-48, 49)
                        val r = (Color.red(base) + grain).coerceIn(0, 255)
                        val g = (Color.green(base) + grain).coerceIn(0, 255)
                        val b = (Color.blue(base) + grain).coerceIn(0, 255)
                        pixels[y * s + x] = Color.rgb(r, g, b)
                    }
                }
            }

            PatternType.PLASMA_WAVE -> {
                for (y in 0 until s) {
                    val ny = y.toFloat() / s * 2f * PI.toFloat()
                    for (x in 0 until s) {
                        val nx = x.toFloat() / s * 2f * PI.toFloat()
                        val v1 = sin(nx * 2f + ny)
                        val v2 = cos(nx - ny * 2f)
                        val v3 = sin(sqrt((nx - PI.toFloat()).pow(2) + (ny - PI.toFloat()).pow(2)) * 3f)
                        val hue = (((v1 + v2 + v3 + 3f) / 6f) * 360f + rng.nextFloat() * 55f) % 360f
                        val bri = (0.35f + rng.nextFloat() * 0.65f)
                        pixels[y * s + x] = Color.HSVToColor(floatArrayOf(hue, 0.85f, bri))
                    }
                }
            }

            PatternType.COSMIC_STARS -> {
                for (y in 0 until s) {
                    for (x in 0 until s) {
                        val roll = rng.nextFloat()
                        pixels[y * s + x] = when {
                            roll < 0.08f -> Color.rgb(255, 255, 255)
                            roll < 0.22f -> Color.rgb(0, 229, 255)
                            roll < 0.36f -> Color.rgb(255, 110, 199)
                            roll < 0.55f -> Color.rgb(110, 85, 220)
                            else -> Color.rgb(
                                12 + rng.nextInt(45),
                                10 + rng.nextInt(40),
                                32 + rng.nextInt(75)
                            )
                        }
                    }
                }
            }
        }

        return pixels
    }

    private fun createStarPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path {
        val path = Path()
        for (i in 0 until points * 2) {
            val angle = PI * i / points - PI / 2.0
            val rad = if (i % 2 == 1) innerR else outerR
            val x = cx + (cos(angle) * rad).toFloat()
            val y = cy + (sin(angle) * rad).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun createPolygonPath(
        cx: Float,
        cy: Float,
        radius: Float,
        sides: Int,
        rotationRad: Float
    ): Path {
        val path = Path()
        for (i in 0 until sides) {
            val angle = (2.0 * PI * i / sides) - (PI / 2.0) + rotationRad
            val x = cx + (cos(angle) * radius).toFloat()
            val y = cy + (sin(angle) * radius).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
}
