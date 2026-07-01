package com.fieldcrm.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

// ==========================================
// NAVIGATION
// ==========================================

@Composable
fun FieldBottomBar(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(FieldTheme.colors.gray900)
            .borderTop(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedItemIndex
                val isNewTab = item.label == "New"
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            onClick = { onItemSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isNewTab) {
                        // Center FAB style (56dp circular icon raised above the bar line)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = (-10).dp)
                                .background(
                                    color = if (isSelected) FieldTheme.colors.purple700 else FieldTheme.colors.purple600,
                                    shape = CircleShape
                                )
                                .border(2.dp, FieldTheme.colors.gray900, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.filledIcon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        val contentColor = if (isSelected) FieldTheme.colors.purple600 else FieldTheme.colors.gray400
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.2f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "tabIconScale"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = FieldTheme.typography.label.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = contentColor
                            )
                            
                            // Active item indicator pill
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(3.dp)
                                        .background(FieldTheme.colors.purple600, RoundedCornerShape(1.5.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldNavigationRail(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(72.dp) // 72pt wide
            .fillMaxHeight()
            .background(FieldTheme.colors.purple900) // Deep Purple side rail background
            .borderRight(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedItemIndex
                val contentColor = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "railIconScale"
                )
                
                Box(
                    modifier = Modifier
                        .size(56.dp) // Icon-only shape
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FieldTheme.colors.purple950 else Color.Transparent)
                        .clickable { onItemSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FieldTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null
) {
    // Outer box fills behind the status bar so the background colour is seamless.
    // The inner Column pushes the actual bar content below the status bar via
    // statusBarsPadding(), making the back button reachable and stopping content bleed.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray900)
            .borderBottom(0.5.dp, FieldTheme.colors.gray700.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.fillMaxWidth().statusBarsPadding())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        style = FieldTheme.typography.display.copy(fontSize = 18.sp),
                        color = FieldTheme.colors.gray100
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    actions()
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
)

// ==========================================
// SURFACES
// ==========================================

@Composable
fun FieldCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .background(FieldTheme.colors.gray850, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun FieldDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(FieldTheme.colors.gray700.copy(alpha = 0.4f))
    )
}

// ==========================================
// INPUTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isRequired: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    errorText: String? = null,
    helperText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (errorText != null) {
        FieldTheme.colors.statusDanger
    } else if (isFocused) {
        FieldTheme.colors.purple500
    } else {
        FieldTheme.colors.gray700
    }
    
    val borderThickness = if (isFocused) 2.dp else 1.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = FieldTheme.typography.label,
                color = if (errorText != null) FieldTheme.colors.statusDanger else FieldTheme.colors.gray400
            )
            if (isRequired) {
                Text(
                    text = "*",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.statusDanger,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray500
                )
            },
            enabled = enabled,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            textStyle = FieldTheme.typography.bodyStrong.copy(color = FieldTheme.colors.gray100),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(borderThickness, borderColor, RoundedCornerShape(FieldTheme.shapes.inputRadius))
                .clip(RoundedCornerShape(FieldTheme.shapes.inputRadius))
        )
        
        if (errorText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = FieldTheme.typography.label.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                color = FieldTheme.colors.statusDanger
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = FieldTheme.typography.label.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                color = FieldTheme.colors.gray500
            )
        }
    }
}

@Composable
fun FieldDropdown(
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    errorText: String? = null,
    helperText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        FieldTextField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            isRequired = isRequired,
            errorText = errorText,
            helperText = helperText,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = FieldIcons.ChevronDownOutlined,
                        contentDescription = "Dropdown Indicator",
                        tint = FieldTheme.colors.gray400
                    )
                }
            },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(FieldTheme.colors.gray800)
                .border(1.dp, FieldTheme.colors.gray700)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray100
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldDatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier
) {
    DatePicker(
        state = state,
        colors = DatePickerDefaults.colors(
            containerColor = FieldTheme.colors.gray850,
            titleContentColor = FieldTheme.colors.gray100,
            headlineContentColor = FieldTheme.colors.gray100,
            weekdayContentColor = FieldTheme.colors.gray400,
            subheadContentColor = FieldTheme.colors.gray300,
            selectedDayContainerColor = FieldTheme.colors.purple600,
            selectedDayContentColor = Color.White,
            todayContentColor = FieldTheme.colors.purple400
        ),
        modifier = modifier
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .background(FieldTheme.colors.gray850, RoundedCornerShape(FieldTheme.shapes.cardRadius))
    )
}

@Composable
fun FieldAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    errorText: String? = null,
    helperText: String? = null
) {
    FieldTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isRequired = isRequired,
        errorText = errorText,
        helperText = helperText,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        trailingIcon = {
            Text(
                text = "₦",
                style = FieldTheme.typography.mono.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = FieldTheme.colors.gray400,
                modifier = Modifier.padding(end = 12.dp)
            )
        },
        visualTransformation = VisualTransformation.None,
        modifier = modifier
    )
}

@Composable
fun FieldSignaturePad(
    modifier: Modifier = Modifier,
    onConfirm: (ImageBitmap) -> Unit,
    onClear: () -> Unit
) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(FieldTheme.shapes.inputRadius))
            .background(FieldTheme.colors.gray800, RoundedCornerShape(FieldTheme.shapes.inputRadius))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val path = Path().apply { moveTo(offset.x, offset.y) }
                            currentPath = path
                            paths.add(path)
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            currentPath?.lineTo(offset.x, offset.y)
                            // Re-trigger Canvas draw
                            val temp = currentPath
                            currentPath = null
                            currentPath = temp
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SecondaryButton(
                text = "Clear",
                onClick = {
                    paths.clear()
                    onClear()
                },
                modifier = Modifier.width(90.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PrimaryButton(
                text = "Confirm",
                onClick = {
                    val bitmap = ImageBitmap(300, 150)
                    onConfirm(bitmap)
                },
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

@Composable
fun FieldUploadDropzone(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(1.dp, FieldTheme.colors.purple600.copy(alpha = 0.4f), FieldTheme.shapes.cardRadius)
            .background(FieldTheme.colors.gray850, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = FieldIcons.InfoOutlined,
                contentDescription = "Upload Icon",
                tint = FieldTheme.colors.purple500,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = FieldTheme.typography.title.copy(fontSize = 14.sp),
                color = FieldTheme.colors.gray100
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                color = FieldTheme.colors.gray500
            )
        }
    }
}

// ==========================================
// STATUS PRIMITIVES
// ==========================================

enum class StatusChipVariant {
    Missing, NeedsReview, LowConfidence, Verified, Signed, Approved, Returned
}

@Composable
fun StatusChip(
    variant: StatusChipVariant,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (variant) {
        StatusChipVariant.Missing -> FieldTheme.colors.statusDanger.copy(alpha = 0.2f) to FieldTheme.colors.statusDanger
        StatusChipVariant.Returned -> FieldTheme.colors.statusDanger.copy(alpha = 0.2f) to FieldTheme.colors.statusDanger
        StatusChipVariant.NeedsReview -> FieldTheme.colors.statusWarning.copy(alpha = 0.2f) to FieldTheme.colors.statusWarning
        StatusChipVariant.LowConfidence -> FieldTheme.colors.statusWarning.copy(alpha = 0.2f) to FieldTheme.colors.statusWarning
        StatusChipVariant.Verified -> FieldTheme.colors.statusSuccess.copy(alpha = 0.2f) to FieldTheme.colors.statusSuccess
        StatusChipVariant.Signed -> FieldTheme.colors.statusSuccess.copy(alpha = 0.2f) to FieldTheme.colors.statusSuccess
        StatusChipVariant.Approved -> FieldTheme.colors.statusSuccess.copy(alpha = 0.2f) to FieldTheme.colors.statusSuccess
    }

    Box(
        modifier = modifier
            .height(22.dp)
            .background(bgColor, RoundedCornerShape(11.dp))
            .border(0.5.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = variant.name.uppercase(Locale.getDefault()),
            style = FieldTheme.typography.label.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

@Composable
fun ConfidenceBar(
    percentage: Float, // value between 0.0f and 1.0f
    modifier: Modifier = Modifier
) {
    val semanticColor = if (percentage >= 0.8f) {
        FieldTheme.colors.statusSuccess
    } else if (percentage >= 0.5f) {
        FieldTheme.colors.statusWarning
    } else {
        FieldTheme.colors.statusDanger
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(FieldTheme.colors.gray700, RoundedCornerShape(1.5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .background(semanticColor, RoundedCornerShape(1.5.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = FieldTheme.typography.mono.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = semanticColor
        )
    }
}

@Composable
fun SourceTag(
    source: String, // "manual", "ocr", "corrected", "approved"
    modifier: Modifier = Modifier
) {
    val word = source.uppercase(Locale.getDefault())
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = FieldIcons.CheckCircleFilled,
            contentDescription = "Source Type",
            tint = FieldTheme.colors.gray500,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = word,
            style = FieldTheme.typography.label.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
            color = FieldTheme.colors.gray500
        )
    }
}

// ==========================================
// ACTIONS
// ==========================================

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = FieldTheme.colors.purple600,
            contentColor = Color.White,
            disabledContainerColor = FieldTheme.colors.gray700,
            disabledContentColor = FieldTheme.colors.gray500
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = FieldTheme.typography.bodyStrong,
                fontWeight = FontWeight.W600
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
        border = BorderStroke(1.dp, FieldTheme.colors.purple600), // Shield Purple border (1px)
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = FieldTheme.colors.purple600 // Shield Purple text
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = FieldTheme.typography.bodyStrong
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = FieldTheme.colors.statusDanger.copy(alpha = 0.15f),
            contentColor = FieldTheme.colors.statusDanger
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = text,
            style = FieldTheme.typography.bodyStrong,
            color = FieldTheme.colors.statusDanger
        )
    }
}

// ==========================================
// FEEDBACK
// ==========================================

@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    width: Dp = 100.dp,
    cornerRadius: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val shimmerColors = listOf(
        FieldTheme.colors.gray800,
        FieldTheme.colors.gray700,
        FieldTheme.colors.gray800
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    Box(
        modifier = modifier
            .size(width, height)
            .background(brush, RoundedCornerShape(cornerRadius))
    )
}

@Composable
fun ErrorBanner(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.statusDanger.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(0.5.dp, FieldTheme.colors.statusDanger.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.statusDanger,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "RETRY",
                style = FieldTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = FieldTheme.colors.purple400,
                modifier = Modifier
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400
            )
            if (linkText != null && onLinkClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = linkText,
                    style = FieldTheme.typography.bodyStrong.copy(
                        color = FieldTheme.colors.purple500,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable(onClick = onLinkClick)
                        .padding(4.dp)
                )
            }
        }
    }
}

// ==========================================
// DOMAIN-SPECIFIC
// ==========================================

@Composable
fun LoanStageTimeline(
    stages: List<String>,
    currentStageIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, stage ->
            val isCompleted = index < currentStageIndex
            val isCurrent = index == currentStageIndex
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent) FieldTheme.colors.purple400 else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = if (isCompleted) FieldTheme.colors.purple600 else FieldTheme.colors.gray800,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = FieldIcons.CheckOutlined,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stage.uppercase(Locale.getDefault()),
                    style = FieldTheme.typography.label.copy(
                        fontSize = 8.sp,
                        letterSpacing = 0.sp
                    ),
                    color = if (isCurrent) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                )
            }
            
            if (index < stages.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(1.dp)
                        .background(if (isCompleted) FieldTheme.colors.purple600 else FieldTheme.colors.gray700)
                )
            }
        }
    }
}

data class ChecklistGate(
    val label: String,
    val isVerified: Boolean,
    val variant: StatusChipVariant
)

@Composable
fun ReadinessChecklist(
    gates: List<ChecklistGate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        gates.forEachIndexed { index, gate ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (gate.isVerified) FieldIcons.CheckOutlined else FieldIcons.CloseOutlined,
                        contentDescription = if (gate.isVerified) "Verified" else "Failed",
                        tint = if (gate.isVerified) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gate.label,
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray300
                    )
                }
                StatusChip(variant = gate.variant)
            }
            if (index < gates.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(FieldTheme.colors.gray700.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
fun DocumentThumbnail(
    fileName: String,
    fileSize: String,
    fileType: String,
    modifier: Modifier = Modifier
) {
    val pageBgColor = FieldTheme.colors.gray800
    val foldColor = FieldTheme.colors.gray700
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drawing a page with a folded corner in Canvas
        Canvas(
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .background(Color.Transparent)
        ) {
            val foldSize = 15f
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width - foldSize, 0f)
                lineTo(size.width, foldSize)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            
            // Draw page background
            drawPath(
                path = path,
                color = pageBgColor
            )
            
            // Draw folded corner
            val foldPath = Path().apply {
                moveTo(size.width - foldSize, 0f)
                lineTo(size.width - foldSize, foldSize)
                lineTo(size.width, foldSize)
                close()
            }
            drawPath(
                path = foldPath,
                color = foldColor
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = FieldTheme.typography.bodyStrong,
                color = FieldTheme.colors.gray100
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = fileSize,
                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                color = FieldTheme.colors.gray400
            )
        }
        
        Box(
            modifier = Modifier
                .background(FieldTheme.colors.gray700, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = fileType.uppercase(Locale.getDefault()),
                style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                color = FieldTheme.colors.gray300
            )
        }
    }
}

@Composable
fun AuditTrailEntry(
    timestamp: String,
    actorName: String,
    actorRole: String,
    action: String,
    diff: String?,
    isCurrentUserAction: Boolean,
    modifier: Modifier = Modifier
) {
    val highlightColor = FieldTheme.colors.purple600
    val leftBorderModifier = if (isCurrentUserAction) {
        Modifier.drawBehind {
            drawRect(
                color = highlightColor,
                topLeft = Offset(0f, 0f),
                size = Size(4.dp.toPx(), size.height)
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
            .background(FieldTheme.colors.gray850, RoundedCornerShape(8.dp))
            .then(leftBorderModifier)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actorName,
                        style = FieldTheme.typography.bodyStrong,
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RoleBadge(role = actorRole)
                }
                Text(
                    text = timestamp,
                    style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                    color = FieldTheme.colors.gray400
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action,
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray300
            )
            if (diff != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                        .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = diff,
                        style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                        color = FieldTheme.colors.purple200
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(
    role: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (role.lowercase(Locale.getDefault())) {
        "loan officer" -> FieldTheme.colors.purple600.copy(alpha = 0.15f) to FieldTheme.colors.purple400
        "branch manager" -> FieldTheme.colors.statusInfo.copy(alpha = 0.15f) to FieldTheme.colors.statusInfo
        "credit officer" -> FieldTheme.colors.statusWarning.copy(alpha = 0.15f) to FieldTheme.colors.statusWarning
        "auditor" -> FieldTheme.colors.gray400.copy(alpha = 0.15f) to FieldTheme.colors.gray400
        "system admin", "mcr" -> FieldTheme.colors.statusSuccess.copy(alpha = 0.15f) to FieldTheme.colors.statusSuccess
        else -> FieldTheme.colors.gray500.copy(alpha = 0.15f) to FieldTheme.colors.gray500
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(0.5.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = role.uppercase(Locale.getDefault()),
            style = FieldTheme.typography.label.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = textColor
        )
    }
}

// ==========================================
// UTILITIES
// ==========================================

fun Modifier.borderBottom(width: Dp, color: Color) = this.drawBehind {
    val strokeWidth = width.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}

fun Modifier.borderTop(width: Dp, color: Color) = this.drawBehind {
    val strokeWidth = width.toPx()
    val y = strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}

fun Modifier.borderRight(width: Dp, color: Color) = this.drawBehind {
    val strokeWidth = width.toPx()
    val x = size.width - strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = strokeWidth
    )
}

fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp
) = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )
    )
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}

@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier,
    color: Color = FieldTheme.colors.statusSuccess,
    onAnimationEnd: () -> Unit = {}
) {
    var animProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Celebratory checkmark animation: 400ms draw + 600ms pop
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
    }
    
    Canvas(modifier = modifier.size(64.dp)) {
        val width = size.width
        val height = size.height
        
        // Circle background
        drawCircle(color = color, radius = width / 2f)
        
        val p = animProgress.value
        
        val startX1 = width * 0.28f
        val startY1 = height * 0.5f
        val endX1 = width * 0.45f
        val endY1 = height * 0.67f
        
        val startX2 = endX1
        val startY2 = endY1
        val endX2 = width * 0.72f
        val endY2 = height * 0.35f
        
        if (p > 0f) {
            val p1 = (p / 0.4f).coerceAtMost(1f)
            val currentEndX1 = startX1 + (endX1 - startX1) * p1
            val currentEndY1 = startY1 + (endY1 - startY1) * p1
            
            drawLine(
                color = Color.White,
                start = Offset(startX1, startY1),
                end = Offset(currentEndX1, currentEndY1),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            if (p > 0.4f) {
                val p2 = ((p - 0.4f) / 0.6f).coerceAtMost(1f)
                val currentEndX2 = startX2 + (endX2 - startX2) * p2
                val currentEndY2 = startY2 + (endY2 - startY2) * p2
                
                drawLine(
                    color = Color.White,
                    start = Offset(startX2, startY2),
                    end = Offset(currentEndX2, currentEndY2),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
