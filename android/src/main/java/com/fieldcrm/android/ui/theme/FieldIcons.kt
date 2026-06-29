package com.fieldcrm.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FieldIcons {
    // Custom Home/Dashboard Icon
    val HomeOutlined: ImageVector = ImageVector.Builder(
        name = "HomeOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(12f, 3f)
        lineTo(4f, 10f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(10f)
        close()
    }.build()

    val HomeFilled: ImageVector = ImageVector.Builder(
        name = "HomeFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(12f, 3f)
        lineTo(3f, 10f)
        verticalLineTo(21f)
        horizontalLineTo(21f)
        verticalLineTo(10f)
        close()
    }.build()

    // Custom Queue/List Icon
    val QueueOutlined: ImageVector = ImageVector.Builder(
        name = "QueueOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(4f, 6f)
        lineTo(20f, 6f)
        moveTo(4f, 12f)
        lineTo(20f, 12f)
        moveTo(4f, 18f)
        lineTo(20f, 18f)
    }.build()

    val QueueFilled: ImageVector = ImageVector.Builder(
        name = "QueueFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(3f, 5f)
        horizontalLineTo(21f)
        verticalLineTo(8f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 11f)
        horizontalLineTo(21f)
        verticalLineTo(14f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 17f)
        horizontalLineTo(21f)
        verticalLineTo(20f)
        horizontalLineTo(3f)
        close()
    }.build()

    // Custom Sync/Send Icon
    val SyncOutlined: ImageVector = ImageVector.Builder(
        name = "SyncOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(12f, 4f)
        arcTo(8f, 8f, 0f, true, true, 4f, 12f)
    }.build()

    val SyncFilled: ImageVector = ImageVector.Builder(
        name = "SyncFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
        horizontalLineTo(18f)
        arcTo(6f, 6f, 0f, true, true, 12f, 6f)
        close()
    }.build()

    // Custom Settings/Gear Icon
    val SettingsOutlined: ImageVector = ImageVector.Builder(
        name = "SettingsOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(12f, 8f)
        arcTo(4f, 4f, 0f, true, true, 8f, 12f)
    }.build()

    val SettingsFilled: ImageVector = ImageVector.Builder(
        name = "SettingsFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(12f, 2f)
        lineTo(14f, 5f)
        lineTo(17f, 5f)
        lineTo(18f, 8f)
        lineTo(21f, 9f)
        lineTo(21f, 12f)
        lineTo(18f, 13f)
        lineTo(17f, 16f)
        lineTo(14f, 17f)
        lineTo(12f, 20f)
        close()
    }.build()

    // Custom Camera Icon (OCR)
    val CameraOutlined: ImageVector = ImageVector.Builder(
        name = "CameraOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(4f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
    }.build()

    val CameraFilled: ImageVector = ImageVector.Builder(
        name = "CameraFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(4f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
    }.build()

    // Custom Search Icon
    val SearchOutlined: ImageVector = ImageVector.Builder(
        name = "SearchOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(10f, 10f)
        moveTo(5f, 10f)
        arcTo(5f, 5f, 0f, true, true, 15f, 10f)
        lineTo(20f, 20f)
    }.build()

    val SearchFilled: ImageVector = ImageVector.Builder(
        name = "SearchFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(10f, 2f)
        arcTo(8f, 8f, 0f, true, false, 18f, 10f)
        lineTo(22f, 22f)
        close()
    }.build()

    // Custom Pen/Signature Icon
    val PenOutlined: ImageVector = ImageVector.Builder(
        name = "PenOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2f
    ) {
        moveTo(3f, 17f)
        lineTo(14f, 6f)
        lineTo(18f, 10f)
        lineTo(7f, 21f)
        close()
    }.build()

    val PenFilled: ImageVector = ImageVector.Builder(
        name = "PenFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(3f, 17f)
        lineTo(14f, 6f)
        lineTo(18f, 10f)
        lineTo(7f, 21f)
        close()
    }.build()

    // Custom Check Icon
    val CheckOutlined: ImageVector = ImageVector.Builder(
        name = "CheckOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2.5f
    ) {
        moveTo(4f, 12f)
        lineTo(9f, 17f)
        lineTo(20f, 6f)
    }.build()

    val CheckFilled: ImageVector = ImageVector.Builder(
        name = "CheckFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
    }.build()

    // Custom Close Icon
    val CloseOutlined: ImageVector = ImageVector.Builder(
        name = "CloseOutlined",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeWidth = 2.5f
    ) {
        moveTo(4f, 4f)
        lineTo(20f, 20f)
        moveTo(20f, 4f)
        lineTo(4f, 20f)
    }.build()

    val CloseFilled: ImageVector = ImageVector.Builder(
        name = "CloseFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White)
    ) {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, true, false, 22f, 12f)
    }.build()
}
