package com.example.model

import androidx.compose.ui.geometry.Offset

enum class ShapeType(val id: String, val labelFa: String, val descriptionFa: String) {
    TEXT("text", "متن", "تبدیل هر متن فارسی یا انگلیسی به شکل سه‌بعدی"),
    CIRCLE("circle", "دایره", "دایره برجسته کلاسیک در مرکز تصویر"),
    STAR("star", "ستاره", "ستاره پنج‌پر درخشان در مرکز"),
    HEART("heart", "قلب", "شکل قلب با منحنی ریاضی دقیق"),
    DIAMOND("diamond", "الماس سه‌بعدی", "الماس چندلایه با شیب عمق پلکانی"),
    CUBE("cube", "مکعب سه‌بعدی", "مکعب ایزومتریک با سطوح عمق متفاوت"),
    SPHERE("sphere", "کره سایه‌دار", "کره سه‌بعدی نرم با برجستگی تدریجی"),
    RIPPLE("ripple", "موج سه‌بعدی", "حلقه‌های موج متحدالمرکز با خطای دید عمیق"),
    DRAW("draw", "نقاشی دستی با انگشت", "طراحی مستقیم نقشهٔ عمق روی صفحه لمسی"),
    UPLOAD("upload", "آپلود نقشهٔ عمق (سفید = نزدیک)", "انتخاب تصویر دلخواه از گالری گوشی")
}

enum class PatternType(val id: String, val labelFa: String) {
    RANDOM_COLOR("rand", "نقطه‌های رنگی تصادفی"),
    MONO_DOTS("mono", "نقطه‌های سیاه‌وسفید"),
    NEON_MATRIX("neon", "ماتریکس نئونی سایبرپانک"),
    GEOMETRIC_MOSAIC("mosaic", "موزاییک هندسی رنگی"),
    PLASMA_WAVE("plasma", "امواج پلاسما و منشور"),
    COSMIC_STARS("cosmic", "بافت کهکشانی و ستاره‌ای"),
    UPLOAD("upload", "آپلود تصویر دلخواه (الگوی سفارشی)")
}

data class DepthStroke(
    val points: List<Offset>,
    val depthValue: Float = 1f, // 0f = black (far/eraser), 1f = white (near)
    val strokeWidthRatio: Float = 0.06f // Relative to min(W, H)
)

data class StereogramConfig(
    val shapeType: ShapeType = ShapeType.TEXT,
    val text: String = "داوود",
    val patternType: PatternType = PatternType.RANDOM_COLOR,
    val invertDepth: Boolean = false,
    val showGuideDots: Boolean = true,
    val width: Int = 1000,
    val height: Int = 650,
    val stripWidth: Int = 140,
    val depthMultiplier: Int = 30,
    val blurRadius: Int = 4
)
