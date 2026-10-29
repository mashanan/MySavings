package com.hanan.mysavings.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.ReceiptDraft
import com.hanan.mysavings.domain.model.ReceiptField
import com.hanan.mysavings.ui.components.CategoryIcon
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.LocalDate

@Composable
fun ScanRoute(
    onBack: () -> Unit,
    onContinue: (ReceiptDraft) -> Unit,
    onManualEntry: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageCapture = rememberReceiptImageCapture()

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        // After a denial, Android stops showing the dialog once "don't ask again" applies;
        // at that point the only way forward is the app's system settings page.
        permanentlyDenied = !granted &&
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
    }
    // Re-check when returning from system settings, where the user may have granted it.
    LifecycleResumeEffect(Unit) {
        hasPermission = context.hasCameraPermission()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = { MySavingsTopBar(title = stringResource(R.string.scan_title), onBack = onBack) },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            !hasPermission -> PermissionContent(
                permanentlyDenied = permanentlyDenied,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { context.openAppSettings() },
                onManualEntry = onManualEntry,
                modifier = contentModifier,
            )

            else -> ScanContent(
                uiState = uiState,
                camera = { ReceiptCameraPreview(imageCapture, Modifier.fillMaxSize()) },
                onCapture = {
                    viewModel.onCaptureStarted()
                    imageCapture.captureInMemory(context, viewModel::onImageCaptured, viewModel::onCaptureFailed)
                },
                onRetry = viewModel::onRetry,
                onContinue = onContinue,
                onManualEntry = onManualEntry,
                modifier = contentModifier,
            )
        }
    }
}

/** Stateless scan body. [camera] is a slot so previews can render without a real camera. */
@Composable
fun ScanContent(
    uiState: ScanUiState,
    camera: @Composable () -> Unit,
    onCapture: () -> Unit,
    onRetry: () -> Unit,
    onContinue: (ReceiptDraft) -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        ScanUiState.Camera, ScanUiState.Processing -> CameraContent(
            isProcessing = uiState == ScanUiState.Processing,
            camera = camera,
            onCapture = onCapture,
            modifier = modifier,
        )

        is ScanUiState.Result -> ResultContent(uiState.draft, onRetry, { onContinue(uiState.draft) }, modifier)

        ScanUiState.Failed -> MessageContent(
            title = stringResource(R.string.scan_failed_title),
            body = stringResource(R.string.scan_failed_body),
            primaryLabel = stringResource(R.string.scan_retry),
            onPrimary = onRetry,
            secondaryLabel = stringResource(R.string.scan_fill_manually),
            onSecondary = onManualEntry,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    isProcessing: Boolean,
    camera: @Composable () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        camera()

        // Guide frame: helps the user fill the photo with the receipt, which improves OCR.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .aspectRatio(0.6f)
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        )
        Text(
            text = stringResource(R.string.scan_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        val captureDescription = stringResource(R.string.action_capture)
        FilledIconButton(
            onClick = onCapture,
            enabled = !isProcessing,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .semantics { contentDescription = captureDescription },
        ) {}

        if (isProcessing) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.scan_processing), color = Color.White)
            }
        }
    }
}

/** Summary of what was read; "Lanjut" opens the Confirm form pre-filled with this draft. */
@Composable
private fun ResultContent(
    draft: ReceiptDraft,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.scan_result_title), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultRow(stringResource(R.string.confirm_merchant), draft.merchant, ReceiptField.MERCHANT in draft.missingFields)
                ResultRow(stringResource(R.string.confirm_amount), draft.total?.let(::formatRupiah), ReceiptField.TOTAL in draft.missingFields)
                ResultRow(stringResource(R.string.confirm_date), draft.date?.let(::formatShortDate), ReceiptField.DATE in draft.missingFields)
                ResultRow(
                    label = stringResource(R.string.confirm_category),
                    value = draft.category?.name,
                    needsCheck = ReceiptField.CATEGORY in draft.missingFields,
                    leading = draft.category?.let { { CategoryIcon(it, size = 24.dp) } },
                )
            }
        }
        Text(
            text = stringResource(R.string.scan_result_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.scan_retake)) }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.scan_continue)) }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String?,
    needsCheck: Boolean,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = value ?: stringResource(R.string.scan_not_detected),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (value != null) FontWeight.Medium else FontWeight.Normal,
            )
            if (needsCheck) {
                Text(
                    text = stringResource(R.string.scan_needs_check),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PermissionContent(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageContent(
        title = stringResource(R.string.scan_permission_title),
        body = stringResource(if (permanentlyDenied) R.string.scan_permission_denied else R.string.scan_permission_body),
        primaryLabel = stringResource(if (permanentlyDenied) R.string.scan_open_settings else R.string.scan_allow_camera),
        onPrimary = if (permanentlyDenied) onOpenSettings else onRequest,
        secondaryLabel = stringResource(R.string.scan_fill_manually),
        onSecondary = onManualEntry,
        modifier = modifier,
    )
}

@Composable
private fun MessageContent(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPrimary) { Text(primaryLabel) }
        OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Preview(showBackground = true)
@Composable
private fun ScanResultPreview() {
    val draft = ReceiptDraft(
        merchant = "Indomaret",
        total = 87_500,
        date = LocalDate.of(2026, 10, 29),
        category = Category(2, "Belanja Harian", "groceries", "#4DB6AC", emptyList(), true),
        missingFields = emptySet(),
    )
    MySavingsTheme {
        ScanContent(ScanUiState.Result(draft), camera = {}, onCapture = {}, onRetry = {}, onContinue = {}, onManualEntry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanFailedPreview() {
    MySavingsTheme {
        ScanContent(ScanUiState.Failed, camera = {}, onCapture = {}, onRetry = {}, onContinue = {}, onManualEntry = {})
    }
}
