package io.daysleft.app.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import io.daysleft.app.R
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.data.local.RepeatInterval
import io.daysleft.app.ui.theme.DaysLeftTheme
import io.daysleft.app.ui.viewmodel.CountdownViewModel
import io.daysleft.app.util.DateUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    eventId: Long,
    viewModel: CountdownViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var event by remember { mutableStateOf<CountdownEventEntity?>(null) }

    LaunchedEffect(eventId) {
        event = viewModel.getEventById(eventId)
    }

    event?.let { currentEvent ->
        InfoScreenContent(
            event = currentEvent,
            onNavigateBack = onNavigateBack,
            onNavigateToEdit = onNavigateToEdit,
            modifier = modifier
        )
    } ?: run {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.event_details)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                )
            },
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreenContent(
    event: CountdownEventEntity,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    var showSharePreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { showSharePreview = true }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(event.id) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_details))
            }
        },
        modifier = modifier
    ) { paddingValues ->
        // 计算日期差值和显示文案
        val today = LocalDate.now()
        val initialDaysDiff = ChronoUnit.DAYS.between(today, event.targetDate)
        val isPast = initialDaysDiff < 0
        val absDays = abs(initialDaysDiff)

        val nextDate = DateUtils.calculateNextOccurrence(event)
        val nextDaysDiff = ChronoUnit.DAYS.between(today, nextDate)

        val daysText = if (isPast && event.isRepeatEnabled) {
            nextDaysDiff.toString()
        } else {
            if (initialDaysDiff == 0L) stringResource(R.string.today_label) else absDays.toString()
        }

        val labelText = if (isPast && event.isRepeatEnabled) {
            val years = DateUtils.calculateEventYears(event, nextDate)
            if (years > 0) stringResource(R.string.days_until_years, years) else stringResource(R.string.days_until_next)
        } else {
            if (initialDaysDiff == 0L) "" else if (isPast) stringResource(R.string.days_past) else stringResource(R.string.days_remaining)
        }

        // 顶层容器使用微妙的多色氛围背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // 氛围感弥散圆圈 (Ambient Blur Circles)
            Box(modifier = Modifier.fillMaxSize()) {
                
            }

            // 核心详情卡片
            val cardBgBrush = Brush.verticalGradient(
                colors = if (isDarkTheme) {
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                } else {
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                }
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBgBrush)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 农历标签 capsule
                    if (event.isLunar) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lunar_label),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 倒数日标题
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 居中大天数圆环容器
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.01f)
                                    )
                                )
                            )
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daysText,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 68.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (labelText.isNotEmpty()) {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 下方详情网格
                    val dateFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.date_format_full))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailItem(
                            icon = Icons.Filled.DateRange,
                            label = stringResource(R.string.target_date),
                            value = event.targetDate.format(dateFormatter)
                        )

                        if (event.isRepeatEnabled) {
                            val intervalText = when (event.repeatInterval) {
                                RepeatInterval.DAILY -> stringResource(R.string.daily)
                                RepeatInterval.WEEKLY -> stringResource(R.string.weekly)
                                RepeatInterval.MONTHLY -> stringResource(R.string.monthly)
                                RepeatInterval.YEARLY -> stringResource(R.string.yearly)
                                else -> stringResource(R.string.repeat)
                            }
                            DetailItem(
                                icon = Icons.Filled.Refresh,
                                label = stringResource(R.string.repeat_interval),
                                value = intervalText
                            )
                        }

                        if (event.syncToSystemCalendar) {
                            DetailItem(
                                icon = Icons.Filled.CheckCircle,
                                label = stringResource(R.string.sync_to_calendar),
                                value = stringResource(R.string.permission_granted).substring(0, minOf(3, stringResource(R.string.permission_granted).length))
                            )
                        }
                    }
                }
            }
        }

        // 弹出分享预览弹窗
        if (showSharePreview) {
            SharePreviewDialog(
                event = event,
                onDismissRequest = { showSharePreview = false }
            )
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewDialog(
    event: CountdownEventEntity,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val systemDark = isSystemInDarkTheme()
    var isDarkPreview by remember(systemDark) { mutableStateOf(systemDark) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 4.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 弹窗标题
                Text(
                    text = stringResource(R.string.share_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 亮暗色选择 SegmentedButton
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isDarkPreview,
                        onClick = { isDarkPreview = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.theme_light)) }
                    SegmentedButton(
                        selected = isDarkPreview,
                        onClick = { isDarkPreview = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.theme_dark)) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 可抓图卡片容器
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    ShareCardLayout(event = event, isDark = isDarkPreview)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 功能按键区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                                        bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    } else {
                                        bitmap
                                    }
                                    saveBitmapToGallery(context, softwareBitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.save_to_gallery), fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                                        bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    } else {
                                        bitmap
                                    }
                                    shareBitmap(context, softwareBitmap)
                                    onDismissRequest()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(stringResource(R.string.share), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ShareCardLayout(
    event: CountdownEventEntity,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    DaysLeftTheme(darkTheme = isDark) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = modifier
                .width(260.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.surface
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surface
                                )
                            }
                        )
                    )
            ) {
                // 信息区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (event.isLunar) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lunar_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val dateFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.date_format_full))
                    Text(
                        text = event.targetDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 天数显示圆圈
                    val today = LocalDate.now()
                    val initialDaysDiff = ChronoUnit.DAYS.between(today, event.targetDate)
                    val isPast = initialDaysDiff < 0
                    val absDays = abs(initialDaysDiff)

                    val nextDate = DateUtils.calculateNextOccurrence(event)
                    val nextDaysDiff = ChronoUnit.DAYS.between(today, nextDate)

                    val daysText = if (isPast && event.isRepeatEnabled) {
                        nextDaysDiff.toString()
                    } else {
                        if (initialDaysDiff == 0L) stringResource(R.string.today_label) else absDays.toString()
                    }

                    val labelText = if (isPast && event.isRepeatEnabled) {
                        val years = DateUtils.calculateEventYears(event, nextDate)
                        if (years > 0) stringResource(R.string.days_until_years, years) else stringResource(R.string.days_until_next)
                    } else {
                        if (initialDaysDiff == 0L) "" else if (isPast) stringResource(R.string.days_past) else stringResource(R.string.days_remaining)
                    }

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.01f)
                                    )
                                )
                            )
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daysText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (labelText.isNotEmpty()) {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 页脚广告推广区
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val promoTagline = when (Locale.getDefault().language) {
                            "zh" -> {
                                val country = Locale.getDefault().country
                                if (country == "TW" || country == "HK") "記錄每一個值得期待的日子" else "记录每一个值得期待的日子"
                            }
                            else -> "Record every day worth looking forward to"
                        }
                        Text(
                            text = promoTagline,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "countdown_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DaysLeft")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { stream ->
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Save failed: Uri is null", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }

        val file = File(cachePath, "countdown_share_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 预览区域 (Previews)
// ==========================================

@Preview(showBackground = true, name = "详情页 - 浅色模式")
@Composable
fun InfoScreenPreview_Light() {
    DaysLeftTheme(darkTheme = false) {
        InfoScreenContent(
            event = CountdownEventEntity(
                id = 1,
                title = "跨年夜",
                targetDate = LocalDate.now().plusDays(24),
                lunarInfo = io.daysleft.app.data.local.LunarInfo(isLunar = false),
                repeatInterval = RepeatInterval.YEARLY,
                notifyDaysInAdvance = 1,
                syncToSystemCalendar = true,
                calendarEventId = null
            ),
            onNavigateBack = {},
            onNavigateToEdit = {}
        )
    }
}

@Preview(showBackground = true, name = "详情页 - 深色模式")
@Composable
fun InfoScreenPreview_Dark() {
    DaysLeftTheme(darkTheme = true) {
        InfoScreenContent(
            event = CountdownEventEntity(
                id = 2,
                title = "端午节",
                targetDate = LocalDate.now().minusDays(5),
                lunarInfo = io.daysleft.app.data.local.LunarInfo(isLunar = true),
                repeatInterval = RepeatInterval.YEARLY,
                notifyDaysInAdvance = 3,
                syncToSystemCalendar = false,
                calendarEventId = null
            ),
            onNavigateBack = {},
            onNavigateToEdit = {}
        )
    }
}
