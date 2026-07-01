package com.fieldcrm.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FieldIcons {

    // ── HOME / DASHBOARD ──────────────────────────────────────
    // House silhouette with peaked roof, arched door, and two windows.
    // Outlined = stroke only; Filled = EvenOdd so door and windows become holes.

    val HomeOutlined: ImageVector = ImageVector.Builder(
        name = "FieldHome_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Outer house: roof peak → left eave → left wall → base → right wall → right eave → close
        moveTo(12f, 2.5f)
        lineTo(2.5f, 11f)
        horizontalLineTo(4.5f)
        verticalLineTo(21.5f)
        horizontalLineTo(19.5f)
        verticalLineTo(11f)
        horizontalLineTo(21.5f)
        close()
        // Arched door (open path — shows as stroke detail inside house)
        moveTo(9.5f, 21.5f)
        verticalLineTo(16f)
        quadTo(9.5f, 13.8f, 12f, 13.8f)
        quadTo(14.5f, 13.8f, 14.5f, 16f)
        verticalLineTo(21.5f)
        // Left window
        moveTo(6.5f, 12.5f)
        horizontalLineTo(9.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(6.5f)
        close()
        // Right window
        moveTo(14.5f, 12.5f)
        horizontalLineTo(17.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(14.5f)
        close()
    }.build()

    val HomeFilled: ImageVector = ImageVector.Builder(
        name = "FieldHome_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        // Outer house (primary fill region)
        moveTo(12f, 2.5f)
        lineTo(2.5f, 11f)
        horizontalLineTo(4.5f)
        verticalLineTo(21.5f)
        horizontalLineTo(19.5f)
        verticalLineTo(11f)
        horizontalLineTo(21.5f)
        close()
        // Door hole (EvenOdd subtracts this from the fill)
        moveTo(9.5f, 21.5f)
        verticalLineTo(16f)
        quadTo(9.5f, 13.8f, 12f, 13.8f)
        quadTo(14.5f, 13.8f, 14.5f, 16f)
        verticalLineTo(21.5f)
        close()
        // Left window hole
        moveTo(6.5f, 12.5f)
        horizontalLineTo(9.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(6.5f)
        close()
        // Right window hole
        moveTo(14.5f, 12.5f)
        horizontalLineTo(17.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(14.5f)
        close()
    }.build()

    // ── QUEUE / LOAN PIPELINE ─────────────────────────────────
    // Three task rows each with a small square status bullet on the left.
    // Filled = three solid bars (first two full-width, third shorter = "in progress").

    val QueueOutlined: ImageVector = ImageVector.Builder(
        name = "FieldQueue_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Row 1 — bullet square
        moveTo(3.5f, 7f)
        horizontalLineTo(6.5f)
        verticalLineTo(10f)
        horizontalLineTo(3.5f)
        close()
        // Row 1 — content line
        moveTo(9.5f, 8.5f)
        horizontalLineTo(20.5f)
        // Row 2 — bullet square
        moveTo(3.5f, 12.5f)
        horizontalLineTo(6.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(3.5f)
        close()
        // Row 2 — content line
        moveTo(9.5f, 14f)
        horizontalLineTo(20.5f)
        // Row 3 — bullet square
        moveTo(3.5f, 18f)
        horizontalLineTo(6.5f)
        verticalLineTo(21f)
        horizontalLineTo(3.5f)
        close()
        // Row 3 — shorter content line (pending/partial)
        moveTo(9.5f, 19.5f)
        horizontalLineTo(16f)
    }.build()

    val QueueFilled: ImageVector = ImageVector.Builder(
        name = "FieldQueue_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        // Full-width bar 1
        moveTo(3.5f, 7f)
        horizontalLineTo(20.5f)
        verticalLineTo(10f)
        horizontalLineTo(3.5f)
        close()
        // Full-width bar 2
        moveTo(3.5f, 12.5f)
        horizontalLineTo(20.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(3.5f)
        close()
        // Shorter bar 3 (in progress)
        moveTo(3.5f, 18f)
        horizontalLineTo(16f)
        verticalLineTo(21f)
        horizontalLineTo(3.5f)
        close()
    }.build()

    // ── SYNC / CLOUD SYNC ────────────────────────────────────
    // Cloud at top with two arrows below — left arrow points UP (upload device→cloud),
    // right arrow points DOWN (download cloud→device). Bidirectional sync.

    val SyncOutlined: ImageVector = ImageVector.Builder(
        name = "FieldSync_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Cloud outline (closed shape)
        moveTo(7f, 12f)
        curveTo(5f, 12f, 3f, 10.5f, 3f, 8.5f)
        curveTo(3f, 6.5f, 4.5f, 5f, 6.5f, 5f)
        curveTo(7f, 3f, 9f, 2f, 11.5f, 2f)
        curveTo(14f, 2f, 15.5f, 3.5f, 16.5f, 5f)
        curveTo(17f, 4.8f, 17.5f, 4.7f, 18f, 4.7f)
        curveTo(19.5f, 4.7f, 21f, 6f, 21f, 8f)
        curveTo(21f, 10f, 19.5f, 12f, 17f, 12f)
        close()
        // Upload arrow (left, x=8): shaft going up, arrowhead at top pointing into cloud
        moveTo(8f, 22f)
        verticalLineTo(12f)
        moveTo(5f, 15f)
        lineTo(8f, 12.5f)
        lineTo(11f, 15f)
        // Download arrow (right, x=16): shaft going down, arrowhead at bottom pointing away from cloud
        moveTo(16f, 12f)
        verticalLineTo(22f)
        moveTo(13f, 19f)
        lineTo(16f, 21.5f)
        lineTo(19f, 19f)
    }.build()

    val SyncFilled: ImageVector = ImageVector.Builder(
        name = "FieldSync_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        // Solid cloud
        moveTo(7f, 12f)
        curveTo(5f, 12f, 3f, 10.5f, 3f, 8.5f)
        curveTo(3f, 6.5f, 4.5f, 5f, 6.5f, 5f)
        curveTo(7f, 3f, 9f, 2f, 11.5f, 2f)
        curveTo(14f, 2f, 15.5f, 3.5f, 16.5f, 5f)
        curveTo(17f, 4.8f, 17.5f, 4.7f, 18f, 4.7f)
        curveTo(19.5f, 4.7f, 21f, 6f, 21f, 8f)
        curveTo(21f, 10f, 19.5f, 12f, 17f, 12f)
        close()
        // Solid upload arrow (left, pointing UP): arrowhead tip → left wing → shaft → right wing → close
        moveTo(8f, 12f)
        lineTo(5f, 15.5f)
        horizontalLineTo(6.5f)
        verticalLineTo(22f)
        horizontalLineTo(9.5f)
        verticalLineTo(15.5f)
        horizontalLineTo(11f)
        close()
        // Solid download arrow (right, pointing DOWN): shaft top → right side → arrowhead tip → left side → close
        moveTo(14.5f, 12f)
        horizontalLineTo(17.5f)
        verticalLineTo(18.5f)
        horizontalLineTo(19.5f)
        lineTo(16f, 22f)
        lineTo(12.5f, 18.5f)
        horizontalLineTo(14.5f)
        close()
    }.build()

    // ── SETTINGS / GEAR ─────────────────────────────────────
    // 6-tooth gear polygon with center hub circle.
    // Tooth tips at r=9.5, valleys at r=7, hub at r=3.
    // Filled = EvenOdd so hub becomes a transparent hole in the solid gear.
    //
    // Vertex coordinates (center 12,12; angles CW from top; sin/cos values pre-computed):
    //  Tip  0°: (12.00, 2.50)    Val  30°: (15.50, 5.94)
    //  Tip 60°: (20.23, 7.25)    Val  90°: (19.00, 12.00)
    //  Tip120°: (20.23, 16.75)   Val 150°: (15.50, 18.06)
    //  Tip180°: (12.00, 21.50)   Val 210°: ( 8.50, 18.06)
    //  Tip240°: ( 3.77, 16.75)   Val 270°: ( 5.00, 12.00)
    //  Tip300°: ( 3.77,  7.25)   Val 330°: ( 8.50,  5.94)

    val SettingsOutlined: ImageVector = ImageVector.Builder(
        name = "FieldSettings_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Gear outline
        moveTo(12f, 2.5f)
        lineTo(15.5f, 5.94f)
        lineTo(20.23f, 7.25f)
        lineTo(19f, 12f)
        lineTo(20.23f, 16.75f)
        lineTo(15.5f, 18.06f)
        lineTo(12f, 21.5f)
        lineTo(8.5f, 18.06f)
        lineTo(3.77f, 16.75f)
        lineTo(5f, 12f)
        lineTo(3.77f, 7.25f)
        lineTo(8.5f, 5.94f)
        close()
        // Hub circle: two CW semi-arcs at r=3, center (12,12)
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, false, true, 9f, 12f)
        arcTo(3f, 3f, 0f, false, true, 15f, 12f)
    }.build()

    val SettingsFilled: ImageVector = ImageVector.Builder(
        name = "FieldSettings_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        // Solid gear (outer region)
        moveTo(12f, 2.5f)
        lineTo(15.5f, 5.94f)
        lineTo(20.23f, 7.25f)
        lineTo(19f, 12f)
        lineTo(20.23f, 16.75f)
        lineTo(15.5f, 18.06f)
        lineTo(12f, 21.5f)
        lineTo(8.5f, 18.06f)
        lineTo(3.77f, 16.75f)
        lineTo(5f, 12f)
        lineTo(3.77f, 7.25f)
        lineTo(8.5f, 5.94f)
        close()
        // Hub hole (EvenOdd subtracts this from the solid gear)
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, false, true, 9f, 12f)
        arcTo(3f, 3f, 0f, false, true, 15f, 12f)
    }.build()

    // ── UTILITY ICONS (used in screens, not nav) ─────────────

    val CameraOutlined: ImageVector = ImageVector.Builder(
        name = "CameraOutlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Camera body
        moveTo(3f, 8f)
        horizontalLineTo(21f)
        verticalLineTo(20f)
        horizontalLineTo(3f)
        close()
        // Viewfinder notch
        moveTo(8f, 8f)
        verticalLineTo(6f)
        horizontalLineTo(10f)
        lineTo(11f, 4f)
        horizontalLineTo(13f)
        lineTo(14f, 6f)
        horizontalLineTo(16f)
        verticalLineTo(8f)
        // Lens circle
        moveTo(15f, 14f)
        arcTo(3f, 3f, 0f, false, true, 9f, 14f)
        arcTo(3f, 3f, 0f, false, true, 15f, 14f)
    }.build()

    val CameraFilled: ImageVector = ImageVector.Builder(
        name = "CameraFilled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        // Camera body
        moveTo(3f, 8f)
        horizontalLineTo(8f)
        verticalLineTo(6f)
        horizontalLineTo(10f)
        lineTo(11f, 4f)
        horizontalLineTo(13f)
        lineTo(14f, 6f)
        horizontalLineTo(16f)
        verticalLineTo(8f)
        horizontalLineTo(21f)
        verticalLineTo(20f)
        horizontalLineTo(3f)
        close()
        // Lens hole
        moveTo(15f, 14f)
        arcTo(3f, 3f, 0f, false, true, 9f, 14f)
        arcTo(3f, 3f, 0f, false, true, 15f, 14f)
    }.build()

    val SearchOutlined: ImageVector = ImageVector.Builder(
        name = "SearchOutlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Magnifier glass circle
        moveTo(17f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, false, true, 4f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, false, true, 17f, 10.5f)
        // Handle
        moveTo(15.5f, 15.5f)
        lineTo(21f, 21f)
    }.build()

    val SearchFilled: ImageVector = ImageVector.Builder(
        name = "SearchFilled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        // Magnifier lens fill
        moveTo(17f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, false, true, 4f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, false, true, 17f, 10.5f)
        // Handle as a thick stroke (filled rectangle at angle)
        moveTo(14.5f, 14.5f)
        lineTo(16f, 13f)
        lineTo(21.5f, 18.5f)
        lineTo(20f, 20f)
        close()
    }.build()

    val PenOutlined: ImageVector = ImageVector.Builder(
        name = "PenOutlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Pen body diagonal
        moveTo(17f, 3f)
        lineTo(21f, 7f)
        lineTo(8f, 20f)
        lineTo(3f, 21f)
        lineTo(4f, 16f)
        close()
        // Pen tip crease line
        moveTo(15f, 5f)
        lineTo(19f, 9f)
    }.build()

    val PenFilled: ImageVector = ImageVector.Builder(
        name = "PenFilled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(17f, 3f)
        lineTo(21f, 7f)
        lineTo(8f, 20f)
        lineTo(3f, 21f)
        lineTo(4f, 16f)
        close()
    }.build()

    val CheckOutlined: ImageVector = ImageVector.Builder(
        name = "CheckOutlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 12f)
        lineTo(9.5f, 17.5f)
        lineTo(20f, 6f)
    }.build()

    val CheckFilled: ImageVector = ImageVector.Builder(
        name = "CheckFilled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        // Filled checkmark as a solid polygon
        moveTo(9.5f, 19f)
        lineTo(2.5f, 12f)
        lineTo(4.5f, 10f)
        lineTo(9.5f, 15f)
        lineTo(19.5f, 5f)
        lineTo(21.5f, 7f)
        close()
    }.build()

    // ── SYNC-STATE STATUS INDICATORS ────────────────────────────
    // Used in the sync queue list to show per-item status.
    // Follows the same stroke/fill pattern as the nav icons.

    // Circle with checkmark — SYNCED
    val CheckCircleOutlined: ImageVector = ImageVector.Builder(
        name = "FieldCheckCircle_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 2.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 21.5f, 12f)
        moveTo(7.5f, 12f)
        lineTo(10.5f, 15f)
        lineTo(17f, 8.5f)
    }.build()

    val CheckCircleFilled: ImageVector = ImageVector.Builder(
        name = "FieldCheckCircle_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        // Solid circle
        moveTo(21.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 2.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 21.5f, 12f)
        // Checkmark as filled polygon hole (EvenOdd punch-through)
        moveTo(9.5f, 17f)
        lineTo(7f, 14.5f)
        lineTo(8.5f, 13f)
        lineTo(9.5f, 14f)
        lineTo(15.5f, 8f)
        lineTo(17f, 9.5f)
        close()
    }.build()

    // Circle with clock hands — PENDING
    val ClockOutlined: ImageVector = ImageVector.Builder(
        name = "FieldClock_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Clock face
        moveTo(21.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 2.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 21.5f, 12f)
        // Hour hand (12→2 o'clock direction)
        moveTo(12f, 7f)
        verticalLineTo(12f)
        lineTo(15.5f, 14f)
    }.build()

    // Warning triangle with exclamation — FAILED
    val AlertOutlined: ImageVector = ImageVector.Builder(
        name = "FieldAlert_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Triangle
        moveTo(12f, 3.5f)
        lineTo(21.5f, 20.5f)
        horizontalLineTo(2.5f)
        close()
        // Exclamation stem
        moveTo(12f, 9f)
        verticalLineTo(14.5f)
        // Exclamation dot (short stub = round cap gives dot)
        moveTo(12f, 17.2f)
        lineTo(12f, 17.3f)
    }.build()

    val AlertFilled: ImageVector = ImageVector.Builder(
        name = "FieldAlert_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        // Solid triangle
        moveTo(12f, 3.5f)
        lineTo(21.5f, 20.5f)
        horizontalLineTo(2.5f)
        close()
        // Exclamation slot (EvenOdd hole)
        moveTo(11f, 9f)
        horizontalLineTo(13f)
        verticalLineTo(15f)
        horizontalLineTo(11f)
        close()
        // Dot hole
        moveTo(10.5f, 17f)
        horizontalLineTo(13.5f)
        verticalLineTo(19f)
        horizontalLineTo(10.5f)
        close()
    }.build()

    val CloseOutlined: ImageVector = ImageVector.Builder(
        name = "CloseOutlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(5f, 5f)
        lineTo(19f, 19f)
        moveTo(19f, 5f)
        lineTo(5f, 19f)
    }.build()

    val CloseFilled: ImageVector = ImageVector.Builder(
        name = "CloseFilled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(5f, 3.5f)
        lineTo(20.5f, 19f)
        lineTo(19f, 20.5f)
        lineTo(3.5f, 5f)
        close()
        moveTo(19f, 3.5f)
        lineTo(20.5f, 5f)
        lineTo(5f, 20.5f)
        lineTo(3.5f, 19f)
        close()
    }.build()

    // ── NAVIGATION ───────────────────────────────────────────────

    val ArrowBackOutlined: ImageVector = ImageVector.Builder(
        name = "FieldArrowBack_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 12f)
        horizontalLineTo(5f)
        moveTo(11f, 6f)
        lineTo(5f, 12f)
        lineTo(11f, 18f)
    }.build()

    val ChevronUpOutlined: ImageVector = ImageVector.Builder(
        name = "FieldChevronUp_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 15f)
        lineTo(12f, 9f)
        lineTo(18f, 15f)
    }.build()

    val ChevronRightOutlined: ImageVector = ImageVector.Builder(
        name = "FieldChevronRight_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(9f, 6f)
        lineTo(15f, 12f)
        lineTo(9f, 18f)
    }.build()

    val ChevronDownOutlined: ImageVector = ImageVector.Builder(
        name = "FieldChevronDown_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 9f)
        lineTo(12f, 15f)
        lineTo(18f, 9f)
    }.build()

    val MoreVertOutlined: ImageVector = ImageVector.Builder(
        name = "FieldMoreVert_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(13.25f, 5f)
        arcTo(1.25f, 1.25f, 0f, false, true, 10.75f, 5f)
        arcTo(1.25f, 1.25f, 0f, false, true, 13.25f, 5f)
        moveTo(13.25f, 12f)
        arcTo(1.25f, 1.25f, 0f, false, true, 10.75f, 12f)
        arcTo(1.25f, 1.25f, 0f, false, true, 13.25f, 12f)
        moveTo(13.25f, 19f)
        arcTo(1.25f, 1.25f, 0f, false, true, 10.75f, 19f)
        arcTo(1.25f, 1.25f, 0f, false, true, 13.25f, 19f)
    }.build()

    // ── AUTH & SECURITY ──────────────────────────────────────────
    // Shield: flat top, straight sides, curves to bottom point.

    val ShieldOutlined: ImageVector = ImageVector.Builder(
        name = "FieldShield_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        verticalLineTo(13f)
        curveTo(20f, 18f, 16.5f, 21.5f, 12f, 23f)
        curveTo(7.5f, 21.5f, 4f, 18f, 4f, 13f)
        close()
    }.build()

    val ShieldFilled: ImageVector = ImageVector.Builder(
        name = "FieldShield_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        verticalLineTo(13f)
        curveTo(20f, 18f, 16.5f, 21.5f, 12f, 23f)
        curveTo(7.5f, 21.5f, 4f, 18f, 4f, 13f)
        close()
    }.build()

    // Padlock: rectangular body + U shackle + keyhole circle + slot.
    // Filled uses EvenOdd to punch the shackle opening and keyhole as holes.

    val LockOutlined: ImageVector = ImageVector.Builder(
        name = "FieldLock_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4.5f, 11f)
        horizontalLineTo(19.5f)
        verticalLineTo(21f)
        horizontalLineTo(4.5f)
        close()
        moveTo(8.5f, 11f)
        verticalLineTo(7.5f)
        curveTo(8.5f, 4.5f, 10f, 3f, 12f, 3f)
        curveTo(14f, 3f, 15.5f, 4.5f, 15.5f, 7.5f)
        verticalLineTo(11f)
        moveTo(13f, 15f)
        arcTo(1f, 1f, 0f, false, true, 11f, 15f)
        arcTo(1f, 1f, 0f, false, true, 13f, 15f)
        moveTo(12f, 16f)
        verticalLineTo(18f)
    }.build()

    val LockFilled: ImageVector = ImageVector.Builder(
        name = "FieldLock_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(8f, 11f)
        verticalLineTo(7.5f)
        curveTo(8f, 3.5f, 9.8f, 2.5f, 12f, 2.5f)
        curveTo(14.2f, 2.5f, 16f, 3.5f, 16f, 7.5f)
        verticalLineTo(11f)
        horizontalLineTo(19.5f)
        verticalLineTo(21f)
        horizontalLineTo(4.5f)
        verticalLineTo(11f)
        close()
        moveTo(10.5f, 11f)
        verticalLineTo(7.5f)
        curveTo(10.5f, 5.5f, 11f, 5f, 12f, 5f)
        curveTo(13f, 5f, 13.5f, 5.5f, 13.5f, 7.5f)
        verticalLineTo(11f)
        close()
        moveTo(13f, 15f)
        arcTo(1f, 1f, 0f, false, true, 11f, 15f)
        arcTo(1f, 1f, 0f, false, true, 13f, 15f)
        moveTo(11.5f, 16f)
        horizontalLineTo(12.5f)
        verticalLineTo(18.5f)
        horizontalLineTo(11.5f)
        close()
    }.build()

    // Eye open: almond outline + pupil. Eye off: outline (partial) + diagonal slash.

    val EyeOutlined: ImageVector = ImageVector.Builder(
        name = "FieldEye_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(1.5f, 12f)
        curveTo(3.5f, 7.5f, 7.5f, 5f, 12f, 5f)
        curveTo(16.5f, 5f, 20.5f, 7.5f, 22.5f, 12f)
        curveTo(20.5f, 16.5f, 16.5f, 19f, 12f, 19f)
        curveTo(7.5f, 19f, 3.5f, 16.5f, 1.5f, 12f)
        close()
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, false, true, 9f, 12f)
        arcTo(3f, 3f, 0f, false, true, 15f, 12f)
    }.build()

    val EyeOffOutlined: ImageVector = ImageVector.Builder(
        name = "FieldEyeOff_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(2.5f, 9f)
        curveTo(4.5f, 7f, 7.5f, 5.5f, 12f, 5.5f)
        curveTo(15f, 5.5f, 17.5f, 6.5f, 19.5f, 8f)
        moveTo(21.5f, 11.5f)
        curveTo(20.5f, 14f, 19f, 15.5f, 17f, 16.5f)
        moveTo(14f, 18f)
        curveTo(13.5f, 18.5f, 12.5f, 19f, 12f, 19f)
        curveTo(9f, 19f, 6f, 17.5f, 3f, 14.5f)
        curveTo(2.5f, 13.5f, 2f, 13f, 1.5f, 12f)
        moveTo(3.5f, 3.5f)
        lineTo(20.5f, 20.5f)
    }.build()

    // Fingerprint: four concentric scan-arcs + top whorl origin.

    val FingerprintOutlined: ImageVector = ImageVector.Builder(
        name = "FieldFingerprint_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(2f, 15f)
        curveTo(2f, 9.5f, 6.5f, 5.5f, 12f, 5.5f)
        curveTo(17.5f, 5.5f, 22f, 9.5f, 22f, 15f)
        moveTo(5f, 17.5f)
        curveTo(5f, 13.5f, 8f, 10f, 12f, 10f)
        curveTo(16f, 10f, 19f, 13.5f, 19f, 17.5f)
        moveTo(8f, 19.5f)
        curveTo(8f, 17f, 9.8f, 14f, 12f, 14f)
        curveTo(14.2f, 14f, 16f, 17f, 16f, 19.5f)
        moveTo(9f, 5.5f)
        curveTo(9f, 3.5f, 10.3f, 2f, 12f, 2f)
        curveTo(13.7f, 2f, 15f, 3f, 15f, 5f)
    }.build()

    // ── NOTIFICATIONS ────────────────────────────────────────────
    // Bell: dome + rim bar + dangling clapper.

    val BellOutlined: ImageVector = ImageVector.Builder(
        name = "FieldBell_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        verticalLineTo(4f)
        moveTo(5.5f, 17f)
        horizontalLineTo(18.5f)
        verticalLineTo(14.5f)
        curveTo(18.5f, 10f, 15.5f, 6f, 12f, 6f)
        curveTo(8.5f, 6f, 5.5f, 10f, 5.5f, 14.5f)
        close()
        moveTo(14.5f, 19.5f)
        curveTo(14.5f, 20.88f, 13.38f, 22f, 12f, 22f)
        curveTo(10.62f, 22f, 9.5f, 20.88f, 9.5f, 19.5f)
    }.build()

    val BellFilled: ImageVector = ImageVector.Builder(
        name = "FieldBell_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(5.5f, 17.5f)
        horizontalLineTo(18.5f)
        verticalLineTo(14.5f)
        curveTo(18.5f, 10f, 15.5f, 6f, 12f, 6f)
        curveTo(8.5f, 6f, 5.5f, 10f, 5.5f, 14.5f)
        close()
        moveTo(14.5f, 19.5f)
        curveTo(14.5f, 20.88f, 13.38f, 22f, 12f, 22f)
        curveTo(10.62f, 22f, 9.5f, 20.88f, 9.5f, 19.5f)
        close()
    }.build()

    // ── LOCATION ─────────────────────────────────────────────────
    // Teardrop pin. Filled = EvenOdd so inner circle is a transparent hole.

    val LocationOutlined: ImageVector = ImageVector.Builder(
        name = "FieldLocation_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 22f)
        curveTo(12f, 22f, 5f, 14.25f, 5f, 9f)
        curveTo(5f, 5.13f, 8.13f, 2f, 12f, 2f)
        curveTo(15.87f, 2f, 19f, 5.13f, 19f, 9f)
        curveTo(19f, 14.25f, 12f, 22f, 12f, 22f)
        moveTo(14.5f, 9f)
        arcTo(2.5f, 2.5f, 0f, false, true, 9.5f, 9f)
        arcTo(2.5f, 2.5f, 0f, false, true, 14.5f, 9f)
    }.build()

    val LocationFilled: ImageVector = ImageVector.Builder(
        name = "FieldLocation_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(12f, 22f)
        curveTo(12f, 22f, 5f, 14.25f, 5f, 9f)
        curveTo(5f, 5.13f, 8.13f, 2f, 12f, 2f)
        curveTo(15.87f, 2f, 19f, 5.13f, 19f, 9f)
        curveTo(19f, 14.25f, 12f, 22f, 12f, 22f)
        moveTo(14.5f, 9f)
        arcTo(2.5f, 2.5f, 0f, false, true, 9.5f, 9f)
        arcTo(2.5f, 2.5f, 0f, false, true, 14.5f, 9f)
    }.build()

    // ── ACTIONS ──────────────────────────────────────────────────

    val AddOutlined: ImageVector = ImageVector.Builder(
        name = "FieldAdd_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(12f, 5f)
        verticalLineTo(19f)
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }.build()

    val AddFilled: ImageVector = ImageVector.Builder(
        name = "FieldAdd_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(10.5f, 3.5f)
        horizontalLineTo(13.5f)
        verticalLineTo(10.5f)
        horizontalLineTo(20.5f)
        verticalLineTo(13.5f)
        horizontalLineTo(13.5f)
        verticalLineTo(20.5f)
        horizontalLineTo(10.5f)
        verticalLineTo(13.5f)
        horizontalLineTo(3.5f)
        verticalLineTo(10.5f)
        horizontalLineTo(10.5f)
        close()
    }.build()

    // Trash can: lid line, handle arch, body trapezoid, two interior lines.

    val DeleteOutlined: ImageVector = ImageVector.Builder(
        name = "FieldDelete_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(9f, 7f)
        verticalLineTo(5f)
        curveTo(9f, 4f, 9.9f, 3f, 11f, 3f)
        horizontalLineTo(13f)
        curveTo(14.1f, 3f, 15f, 4f, 15f, 5f)
        verticalLineTo(7f)
        moveTo(6f, 7f)
        lineTo(7f, 21f)
        horizontalLineTo(17f)
        lineTo(18f, 7f)
        moveTo(10f, 11f)
        verticalLineTo(17f)
        moveTo(14f, 11f)
        verticalLineTo(17f)
    }.build()

    // Two half-circle arrows (bidirectional reload).

    val RefreshOutlined: ImageVector = ImageVector.Builder(
        name = "FieldRefresh_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4.5f, 8.5f)
        curveTo(6f, 5.5f, 9f, 3f, 12f, 3f)
        curveTo(17.5f, 3f, 22f, 7.5f, 22f, 13f)
        moveTo(19.5f, 3f)
        lineTo(22f, 3f)
        verticalLineTo(5.5f)
        moveTo(19.5f, 15.5f)
        curveTo(18f, 18.5f, 15f, 21f, 12f, 21f)
        curveTo(6.5f, 21f, 2f, 16.5f, 2f, 11f)
        moveTo(4.5f, 21f)
        lineTo(2f, 21f)
        verticalLineTo(18.5f)
    }.build()

    // ── INFO / STATUS ─────────────────────────────────────────────

    val InfoOutlined: ImageVector = ImageVector.Builder(
        name = "FieldInfo_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 2.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 21.5f, 12f)
        moveTo(12f, 8f)
        lineTo(12f, 8.01f)
        moveTo(12f, 11f)
        verticalLineTo(16f)
    }.build()

    // Cloud crossed out — full-screen offline / no-network error.

    val CloudOffOutlined: ImageVector = ImageVector.Builder(
        name = "FieldCloudOff_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 7f)
        curveTo(4f, 7.5f, 2.5f, 9f, 2.5f, 11f)
        curveTo(2.5f, 13f, 4f, 14.5f, 6.5f, 15f)
        moveTo(15f, 4.5f)
        curveTo(17f, 5f, 18.5f, 6.5f, 19.5f, 8f)
        curveTo(20f, 7.8f, 20.5f, 7.7f, 21f, 7.7f)
        curveTo(22.3f, 7.7f, 22.5f, 9f, 22.5f, 10f)
        curveTo(22.5f, 12f, 21f, 13.5f, 19f, 14.5f)
        moveTo(3f, 3f)
        lineTo(21f, 21f)
    }.build()

    // Empty circle — unchecked radio / requirement not yet met.

    val RadioUncheckedOutlined: ImageVector = ImageVector.Builder(
        name = "FieldRadioUnchecked_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(21.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 2.5f, 12f)
        arcTo(9.5f, 9.5f, 0f, false, true, 21.5f, 12f)
    }.build()

    // ── PEOPLE & IDENTITY ────────────────────────────────────────

    val PersonOutlined: ImageVector = ImageVector.Builder(
        name = "FieldPerson_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 8f)
        arcTo(4f, 4f, 0f, false, true, 8f, 8f)
        arcTo(4f, 4f, 0f, false, true, 16f, 8f)
        moveTo(2.5f, 21.5f)
        curveTo(2.5f, 16.5f, 6.8f, 14f, 12f, 14f)
        curveTo(17.2f, 14f, 21.5f, 16.5f, 21.5f, 21.5f)
    }.build()

    val PersonFilled: ImageVector = ImageVector.Builder(
        name = "FieldPerson_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(16f, 8f)
        arcTo(4f, 4f, 0f, false, true, 8f, 8f)
        arcTo(4f, 4f, 0f, false, true, 16f, 8f)
        moveTo(2.5f, 22f)
        horizontalLineTo(21.5f)
        curveTo(21.5f, 17f, 17.2f, 14f, 12f, 14f)
        curveTo(6.8f, 14f, 2.5f, 17f, 2.5f, 22f)
        close()
    }.build()

    val PersonAddOutlined: ImageVector = ImageVector.Builder(
        name = "FieldPersonAdd_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(14f, 8f)
        arcTo(4f, 4f, 0f, false, true, 6f, 8f)
        arcTo(4f, 4f, 0f, false, true, 14f, 8f)
        moveTo(1.5f, 21.5f)
        curveTo(1.5f, 16.5f, 5.5f, 14f, 10f, 14f)
        curveTo(13f, 14f, 15.5f, 15f, 17f, 17f)
        moveTo(20f, 9f)
        verticalLineTo(15f)
        moveTo(17f, 12f)
        horizontalLineTo(23f)
    }.build()

    val GroupOutlined: ImageVector = ImageVector.Builder(
        name = "FieldGroup_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(17f, 8f)
        arcTo(4f, 4f, 0f, false, true, 9f, 8f)
        arcTo(4f, 4f, 0f, false, true, 17f, 8f)
        moveTo(3f, 21.5f)
        curveTo(3f, 16.5f, 7.5f, 14f, 13f, 14f)
        curveTo(18.5f, 14f, 23f, 16.5f, 23f, 21.5f)
        moveTo(8f, 9f)
        arcTo(3f, 3f, 0f, false, true, 2f, 9f)
        arcTo(3f, 3f, 0f, false, true, 8f, 9f)
        moveTo(1f, 21.5f)
        curveTo(1f, 17.5f, 4f, 15.5f, 7.5f, 15.5f)
    }.build()

    val GroupFilled: ImageVector = ImageVector.Builder(
        name = "FieldGroup_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(17f, 8f)
        arcTo(4f, 4f, 0f, false, true, 9f, 8f)
        arcTo(4f, 4f, 0f, false, true, 17f, 8f)
        moveTo(3f, 22f)
        horizontalLineTo(23f)
        curveTo(23f, 17f, 18.5f, 14f, 13f, 14f)
        curveTo(7.5f, 14f, 3f, 17f, 3f, 22f)
        close()
        moveTo(8f, 9f)
        arcTo(3f, 3f, 0f, false, true, 2f, 9f)
        arcTo(3f, 3f, 0f, false, true, 8f, 9f)
    }.build()

    // Phone handset: curved L shape tracing earpiece → shaft → mouthpiece.

    val PhoneOutlined: ImageVector = ImageVector.Builder(
        name = "FieldPhone_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6.5f, 2.5f)
        curveTo(5.5f, 2.5f, 4.5f, 3f, 4f, 4f)
        curveTo(3.5f, 5f, 3.5f, 6f, 3.5f, 7f)
        curveTo(3.5f, 14f, 10f, 20.5f, 17f, 20.5f)
        curveTo(18f, 20.5f, 19f, 20.5f, 20f, 20f)
        curveTo(21f, 19.5f, 21.5f, 18.5f, 21.5f, 17.5f)
        lineTo(18f, 14f)
        curveTo(17.5f, 13.5f, 16.5f, 13.5f, 16f, 14f)
        lineTo(14.5f, 15.5f)
        curveTo(13.5f, 15f, 11.5f, 13.5f, 10f, 12f)
        curveTo(8.5f, 10.5f, 7f, 8.5f, 6.5f, 7.5f)
        lineTo(8f, 6f)
        curveTo(8.5f, 5.5f, 8.5f, 4.5f, 8f, 4f)
        close()
    }.build()

    // ID card: card outline + photo circle (left) + text lines (right).

    val BadgeOutlined: ImageVector = ImageVector.Builder(
        name = "FieldBadge_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 5f)
        horizontalLineTo(21f)
        verticalLineTo(19f)
        horizontalLineTo(3f)
        close()
        moveTo(11f, 12f)
        arcTo(3f, 3f, 0f, false, true, 5f, 12f)
        arcTo(3f, 3f, 0f, false, true, 11f, 12f)
        moveTo(13f, 10f)
        horizontalLineTo(19f)
        moveTo(13f, 13f)
        horizontalLineTo(19f)
        moveTo(13f, 16f)
        horizontalLineTo(17f)
    }.build()

    // Document page with folded top-right corner + three content lines.
    // Filled = EvenOdd: fold triangle becomes a transparent notch.

    val DocumentOutlined: ImageVector = ImageVector.Builder(
        name = "FieldDocument_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(5f, 2.5f)
        horizontalLineTo(14.5f)
        lineTo(19.5f, 7.5f)
        verticalLineTo(21.5f)
        horizontalLineTo(5f)
        close()
        moveTo(14.5f, 2.5f)
        verticalLineTo(7.5f)
        horizontalLineTo(19.5f)
        moveTo(8f, 11f)
        horizontalLineTo(16f)
        moveTo(8f, 14f)
        horizontalLineTo(16f)
        moveTo(8f, 17f)
        horizontalLineTo(13f)
    }.build()

    val DocumentFilled: ImageVector = ImageVector.Builder(
        name = "FieldDocument_Filled",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.EvenOdd
    ) {
        moveTo(5f, 2.5f)
        horizontalLineTo(14.5f)
        lineTo(19.5f, 7.5f)
        verticalLineTo(21.5f)
        horizontalLineTo(5f)
        close()
        moveTo(14.5f, 2.5f)
        lineTo(19.5f, 7.5f)
        horizontalLineTo(14.5f)
        close()
    }.build()

    // ── FINANCE & MAP ─────────────────────────────────────────────
    // Banknote: rectangular outline + center coin + corner denomination dots.

    val PaymentsOutlined: ImageVector = ImageVector.Builder(
        name = "FieldPayments_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 7f)
        horizontalLineTo(21f)
        verticalLineTo(17f)
        horizontalLineTo(3f)
        close()
        moveTo(14.5f, 12f)
        arcTo(2.5f, 2.5f, 0f, false, true, 9.5f, 12f)
        arcTo(2.5f, 2.5f, 0f, false, true, 14.5f, 12f)
        moveTo(5.75f, 10.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 4.25f, 10.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 5.75f, 10.5f)
        moveTo(5.75f, 13.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 4.25f, 13.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 5.75f, 13.5f)
        moveTo(19.75f, 10.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 18.25f, 10.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 19.75f, 10.5f)
        moveTo(19.75f, 13.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 18.25f, 13.5f)
        arcTo(0.75f, 0.75f, 0f, false, true, 19.75f, 13.5f)
    }.build()

    // Folded map: zigzag outline + two vertical crease lines.

    val MapOutlined: ImageVector = ImageVector.Builder(
        name = "FieldMap_Outlined",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.75f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 5.5f)
        lineTo(9f, 3f)
        lineTo(15f, 5.5f)
        lineTo(21f, 3f)
        verticalLineTo(18.5f)
        lineTo(15f, 21f)
        lineTo(9f, 18.5f)
        lineTo(3f, 21f)
        close()
        moveTo(9f, 3f)
        verticalLineTo(18.5f)
        moveTo(15f, 5.5f)
        verticalLineTo(21f)
    }.build()
}
