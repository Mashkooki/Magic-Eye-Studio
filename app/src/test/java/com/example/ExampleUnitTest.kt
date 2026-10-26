package com.example

import com.example.engine.StereogramEngine
import com.example.model.PatternType
import com.example.model.ShapeType
import com.example.model.StereogramConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultConfig_matchesOriginalHtmlDefaults() {
        val config = StereogramConfig()
        assertEquals(ShapeType.TEXT, config.shapeType)
        assertEquals("داوود", config.text)
        assertEquals(PatternType.RANDOM_COLOR, config.patternType)
        assertEquals(1000, config.width)
        assertEquals(650, config.height)
        assertEquals(140, config.stripWidth)
        assertEquals(30, config.depthMultiplier)
        assertEquals(4, config.blurRadius)
    }

    @Test
    fun boxBlur_smoothsSharpEdgesCorrectly() {
        val w = 10
        val h = 10
        val data = FloatArray(w * h) { idx ->
            val x = idx % w
            if (x >= 5) 1f else 0f
        }
        val blurred = StereogramEngine.boxBlur(data, w, h, 2)
        // Left boundary stays near 0, right boundary stays near 1, center transitions smoothly
        assertEquals(0f, blurred[0], 0.01f)
        assertEquals(1f, blurred[w - 1], 0.01f)
        assertTrue(blurred[4] > 0.1f && blurred[4] < 0.9f)
    }
}
