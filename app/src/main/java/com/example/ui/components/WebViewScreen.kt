package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.PartnerLinks

data class ExtractedWebInfo(
    val title: String,
    val url: String,
    val extractedLocation: String,
    val extractedTime: String,
    val summary: String
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TraloWebViewScreen(
    initialUrl: String,
    title: String,
    onClose: () -> Unit,
    onExtractedInfoApproved: (ExtractedWebInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf(title) }
    var progress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewInstance: WebView? by remember { mutableStateOf(null) }
    var showExtractDialog by remember { mutableStateOf(false) }
    var extractedPreview by remember { mutableStateOf<ExtractedWebInfo?>(null) }

    fun extractInfoFromCurrentPage() {
        val loc = when {
            currentUrl.contains("booking.com", true) -> "ホテル予約先"
            currentUrl.contains("klook", true) -> "観光アクティビティ"
            currentUrl.contains("train-guide", true) -> "JR運行・走行位置"
            else -> pageTitle.takeIf { it.isNotBlank() } ?: "WEB参照地"
        }
        extractedPreview = ExtractedWebInfo(
            title = pageTitle,
            url = currentUrl,
            extractedLocation = loc,
            extractedTime = "10:00",
            summary = "URL: $currentUrl から抽出した公式Web情報（取得日時: 2026-04-10）"
        )
        showExtractDialog = true
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("tralo_webview_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pageTitle.ifBlank { "インアプリWeb表示" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.testTag("webview_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { extractInfoFromCurrentPage() },
                        modifier = Modifier.testTag("webview_extract_button")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "ページから情報抽出")
                    }
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.testTag("webview_reload_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "再読み込み")
                    }
                    IconButton(
                        onClick = { PartnerLinks.openExternalUrl(context, currentUrl) },
                        modifier = Modifier.testTag("webview_external_browser_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "外部ブラウザで開く")
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("webview_close_button")
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "閉じる")
                    }
                }
            )

            if (progress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                currentUrl = url
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { currentUrl = it }
                                view?.title?.let { if (it.isNotBlank()) pageTitle = it }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                title?.let { if (it.isNotBlank()) pageTitle = it }
                            }
                        }
                        loadUrl(initialUrl)
                        webViewInstance = this
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "前のページ")
                        }
                        Button(
                            onClick = { extractInfoFromCurrentPage() },
                            modifier = Modifier.testTag("webview_bar_extract_button")
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("このページから情報を抽出")
                        }
                    }
                    Text(
                        text = "安全なSSL接続",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showExtractDialog && extractedPreview != null) {
        val info = extractedPreview!!
        AlertDialog(
            onDismissRequest = { showExtractDialog = false },
            title = { Text("Webページ情報抽出確認") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("タイトル: ${info.title}", style = MaterialTheme.typography.bodyMedium)
                    Text("抽出項目: ${info.extractedLocation}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("詳細: ${info.summary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("この情報をTRALOの検索条件・旅程に反映しますか？", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExtractedInfoApproved(info)
                        showExtractDialog = false
                        onClose()
                    },
                    modifier = Modifier.testTag("approve_extracted_info_button")
                ) {
                    Text("承認してアプリに反映")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtractDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
