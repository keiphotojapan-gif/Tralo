package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ConciergeMessage
import com.example.model.ConciergeUiState
import com.example.model.ProposedTripPlan
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloErrorCard
import com.example.ui.components.TraloInfoNotice
import com.example.ui.components.TraloStatusBadge
import com.example.ui.theme.TabularNumericMedium
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ConciergeScreen(
    messages: List<ConciergeMessage>,
    uiState: ConciergeUiState,
    contextSummary: String,
    pendingProposal: Pair<String, ProposedTripPlan>?,
    savedBanner: String?,
    initialPrompt: String?,
    onSendChat: (String, String) -> Unit,
    onGenerateTripDraft: (String) -> Unit,
    onRequestSaveProposal: (String, ProposedTripPlan) -> Unit,
    onConfirmSaveProposal: () -> Unit,
    onDismissSaveProposal: () -> Unit,
    onClearBanner: () -> Unit,
    onClearError: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember(initialPrompt) { mutableStateOf(initialPrompt.orEmpty()) }
    var draftMode by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("AI TRAVEL CONCIERGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text("TRALO コンシェルジュ", style = MaterialTheme.typography.titleLarge)
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("concierge_close_button")
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "閉じる")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick mode & prompt chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = !draftMode,
                        onClick = { draftMode = false },
                        label = { Text("相談チャット") }
                    )
                }
                item {
                    FilterChip(
                        selected = draftMode,
                        onClick = { draftMode = true },
                        label = { Text("旅程JSON生成モード") }
                    )
                }
                val quickPrompts = listOf(
                    "予算8万円で金沢2泊3日の旅程を作成" to true,
                    "京都で雨天時におすすめの屋内観光ルートは？" to false,
                    "遅延時の代替経路と新幹線での注意点" to false
                )
                items(quickPrompts) { (prompt, isDraft) ->
                    OutlinedButton(
                        onClick = {
                            if (isDraft) {
                                onGenerateTripDraft(prompt)
                            } else {
                                onSendChat(prompt, contextSummary)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(prompt, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (!savedBanner.isNullOrBlank()) {
                TraloInfoNotice(
                    title = "保存完了",
                    body = savedBanner,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                TextButton(
                    onClick = onClearBanner,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) { Text("閉じる") }
            }

            if (uiState is ConciergeUiState.Error) {
                TraloErrorCard(
                    message = uiState.message,
                    onRetry = onClearError,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ConciergeMessageBubble(
                        message = msg,
                        onSaveProposal = { proposal ->
                            onRequestSaveProposal(msg.id, proposal)
                        }
                    )
                }

                if (uiState is ConciergeUiState.Loading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Geminiがプランを組み立てています...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (uiState is ConciergeUiState.Streaming) {
                    item {
                        TraloCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                text = uiState.partialText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (draftMode) "例：箱根1泊2日 予算5万円の温泉旅"
                            else "旅程・交通・天気・持ち物を質問..."
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("concierge_input")
                )
                Button(
                    onClick = {
                        val current = inputText
                        inputText = ""
                        if (draftMode) {
                            onGenerateTripDraft(current)
                        } else {
                            onSendChat(current, contextSummary)
                        }
                    },
                    enabled = inputText.isNotBlank() && uiState !is ConciergeUiState.Loading && uiState !is ConciergeUiState.Streaming,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("concierge_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "送信")
                }
            }
        }
    }

    pendingProposal?.let { (_, proposal) ->
        val fmt = NumberFormat.getNumberInstance(Locale.JAPAN)
        AlertDialog(
            onDismissRequest = onDismissSaveProposal,
            title = { Text("旅程の保存確認") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("以下のAI提案プランをあなたのクラウド旅程に保存します。よろしいですか？")
                    Text("タイトル: ${proposal.title}", fontWeight = FontWeight.SemiBold)
                    Text("目的地: ${proposal.destination} (${proposal.startDate} 〜 ${proposal.endDate})")
                    Text("目安予算: ¥${fmt.format(proposal.budgetJpy)}")
                    Text("予定数: ${proposal.stops.size}件 / 持ち物候補: ${proposal.checklistSuggestions.size}件")
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSaveProposal,
                    modifier = Modifier.testTag("concierge_confirm_save_button")
                ) { Text("確認して保存") }
            },
            dismissButton = {
                TextButton(onClick = onDismissSaveProposal) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun ConciergeMessageBubble(
    message: ConciergeMessage,
    onSaveProposal: (ProposedTripPlan) -> Unit
) {
    val container = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    TraloCard(containerColor = container) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (message.isUser) "あなた" else "TRALO AI コンシェルジュ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message.timestampLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = message.text, style = MaterialTheme.typography.bodyLarge)

        message.proposedTrip?.let { proposal ->
            val fmt = NumberFormat.getNumberInstance(Locale.JAPAN)
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(proposal.title, style = MaterialTheme.typography.titleMedium)
                        TraloStatusBadge(
                            label = "¥${fmt.format(proposal.budgetJpy)}",
                            tone = BadgeTone.GOLD
                        )
                    }
                    Text(
                        text = "${proposal.destination} ・ ${proposal.startDate}〜${proposal.endDate} (${proposal.companions})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    proposal.stops.take(5).forEach { stop ->
                        Text(
                            text = "Day${stop.dayIndex} ${stop.startTime} ${stop.title} (${stop.locationName})",
                            style = TabularNumericMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (message.appliedToCloud) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("旅程に保存済み", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { onSaveProposal(proposal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("proposal_save_trigger")
                        ) {
                            Text("内容を確認して旅程に保存")
                        }
                    }
                }
            }
        }
    }
}
