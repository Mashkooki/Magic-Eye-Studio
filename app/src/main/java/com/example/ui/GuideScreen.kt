package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GuideScreen(
    onBackToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToStudio() }

    val infiniteTransition = rememberInfiniteTransition(label = "guide_dots_fusion")
    val separationFactor by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_fusion_anim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("guide_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Focal Trainer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تمرین تطبیق چشم با نقاط راهنما",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "وقتی چشم‌های خود را روی فضای پشت گوشی متمرکز کنید، دو نقطهٔ سفید بالای تصویر به هم نزدیک می‌شوند تا جایی که یک نقطهٔ سوم در وسط شکل بگیرد:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF07090E))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cy = size.height / 2f
                        val cx = size.width / 2f
                        val baseOffset = 70f * separationFactor

                        // Left eye ghost dots
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.75f),
                            radius = 12f,
                            center = Offset(cx - baseOffset, cy)
                        )
                        // Right eye ghost dots
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.75f),
                            radius = 12f,
                            center = Offset(cx + baseOffset, cy)
                        )
                        // Center fused dot
                        drawCircle(
                            color = Color.White,
                            radius = 14f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 18f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Step-by-step viewing instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "گام‌به‌گام تا دیدن تصویر سه‌بعدی",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                GuideStepRow(
                    stepNumber = "۱",
                    title = "فاصله و زاویهٔ مناسب",
                    description = "صفحهٔ گوشی را کاملاً افقی و در فاصلهٔ حدود ۳۰ تا ۴۰ سانتی‌متری صورت خود نگه دارید. نور صفحه را بالا ببرید و سر خود را کج نکنید."
                )
                GuideStepRow(
                    stepNumber = "۲",
                    title = "نگاه به بی‌نهایت (پشت صفحه)",
                    description = "به دو نقطهٔ سفید بالای تصویر نگاه کنید اما فوکوس چشم را طوری شُل کنید که انگار به جسمی در نیم‌متری پشت صفحهٔ گوشی نگاه می‌کنید."
                )
                GuideStepRow(
                    stepNumber = "۳",
                    title = "تبدیل دو نقطه به سه نقطه",
                    description = "با شُل شدن عضلات چشم، هر نقطه دو تا دیده می‌شود (۴ نقطه). فاصلهٔ نگاه را تنظیم کنید تا دو نقطهٔ میانی روی هم بیفتند و ۳ نقطه ببینید."
                )
                GuideStepRow(
                    stepNumber = "۴",
                    title = "ظهور ناگهانی حجم سه‌بعدی",
                    description = "چند ثانیه همان حالت نگاه را حفظ کنید؛ مغز شما الگوهای تکراری را قفل می‌کند و ناگهان شکل یا متن پنهان با حجم واقعی از صفحه بیرون می‌زند!"
                )
            }
        }

        // Parameter explanation card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "راهنمای تنظیمات استودیو",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                ParameterGuideItem(
                    name = "عرض نوار پایه (۶۰ تا ۳۰۰)",
                    desc = "فاصلهٔ تکرار افقی الگو را تعیین می‌کند. روی صفحهٔ موبایل، مقادیر بین ۱۱۰ تا ۱۶۰ راحت‌ترین قفل دید سه‌بعدی را ایجاد می‌کنند."
                )
                ParameterGuideItem(
                    name = "شدت عمق (۴ تا ۷۰)",
                    desc = "میزان برجستگی شکل نسبت به پس‌زمینه را تنظیم می‌کند. مقدار ۲۵ تا ۳۵ برای متن و اشکال استاندارد ایده‌آل است."
                )
                ParameterGuideItem(
                    name = "نرمی لبه‌ها (۰ تا ۱۲)",
                    desc = "با اعمال الگوریتم Box Blur دو مرحله‌ای، لبه‌های تیز شکل را شیب‌دار می‌کند تا چشم راحت‌تر تغییر عمق را دنبال کند."
                )
                ParameterGuideItem(
                    name = "برعکس‌کردن عمق (فرورفته / برجسته)",
                    desc = "به‌طور پیش‌فرض بخش‌های سفید نقشهٔ عمق به سمت شما برجسته می‌شوند. با فعال‌کردن این گزینه، شکل به داخل صفحه فرو می‌رود (یا برای دید ضربدری Cross-Eyed مناسب می‌شود)."
                )
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParameterGuideItem(name: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
