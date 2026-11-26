package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiConciergeRepository
import com.example.data.repository.TripRepository
import com.example.model.ConciergeMessage
import com.example.model.ConciergeUiState
import com.example.model.ProposedTripPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "ConciergeVM"

class ConciergeViewModel(
    private val aiRepository: AiConciergeRepository,
    private val tripRepository: TripRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ConciergeMessage(
                id = "welcome",
                isUser = false,
                text = "TRALO AIコンシェルジュへようこそ。ご予算や日程に合わせた旅程プランの作成、雨天時の屋内プラン変更、乗車車両や乗り換えの相談までお気軽にお尋ねください。",
                timestampLabel = nowLabel()
            )
        )
    )
    val messages: StateFlow<List<ConciergeMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ConciergeUiState>(ConciergeUiState.Idle)
    val uiState: StateFlow<ConciergeUiState> = _uiState.asStateFlow()

    private val _pendingConfirmProposal = MutableStateFlow<Pair<String, ProposedTripPlan>?>(null)
    val pendingConfirmProposal: StateFlow<Pair<String, ProposedTripPlan>?> = _pendingConfirmProposal.asStateFlow()

    private val _savedConfirmationBanner = MutableStateFlow<String?>(null)
    val savedConfirmationBanner: StateFlow<String?> = _savedConfirmationBanner.asStateFlow()

    fun sendChatMessage(prompt: String, contextSummary: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return

        _messages.update {
            it + ConciergeMessage(
                id = UUID.randomUUID().toString(),
                isUser = true,
                text = clean,
                timestampLabel = nowLabel()
            )
        }

        viewModelScope.launch {
            _uiState.value = ConciergeUiState.Loading
            var finalText = ""
            try {
                aiRepository.streamAdvice(clean, contextSummary).collect { partial ->
                    finalText = partial
                    _uiState.value = ConciergeUiState.Streaming(partial)
                }
                _messages.update {
                    it + ConciergeMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = finalText,
                        timestampLabel = nowLabel()
                    )
                }
                _uiState.value = ConciergeUiState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI streaming timed out", e)
                _uiState.value = ConciergeUiState.Error("AIの応答が60秒でタイムアウトしました。再度お試しください。")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI streaming failed", e)
                _uiState.value = ConciergeUiState.Error(
                    e.localizedMessage ?: "AIコンシェルジュの呼び出しに失敗しました。"
                )
            }
        }
    }

    fun generateTripDraft(requestText: String) {
        val clean = requestText.trim()
        if (clean.isBlank()) return

        _messages.update {
            it + ConciergeMessage(
                id = UUID.randomUUID().toString(),
                isUser = true,
                text = "【旅程プラン作成】$clean",
                timestampLabel = nowLabel()
            )
        }

        viewModelScope.launch {
            _uiState.value = ConciergeUiState.Loading
            aiRepository.generateStructuredTripPlan(clean)
                .onSuccess { proposal ->
                    _messages.update {
                        it + ConciergeMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "「${proposal.title}」の旅程ドラフトを作成しました。内容を確認し、よろしければ「旅程に保存」を押してクラウドへ登録してください。",
                            timestampLabel = nowLabel(),
                            proposedTrip = proposal
                        )
                    }
                    _uiState.value = ConciergeUiState.Idle
                }
                .onFailure { err ->
                    _uiState.value = ConciergeUiState.Error(
                        err.localizedMessage ?: "構造化旅程の生成に失敗しました。"
                    )
                }
        }
    }

    fun requestSaveConfirmation(messageId: String, proposal: ProposedTripPlan) {
        _pendingConfirmProposal.value = messageId to proposal
    }

    fun dismissSaveConfirmation() {
        _pendingConfirmProposal.value = null
    }

    fun confirmAndSaveProposal() {
        val pair = _pendingConfirmProposal.value ?: return
        val (messageId, proposal) = pair
        _pendingConfirmProposal.value = null

        viewModelScope.launch {
            tripRepository.createTripFromProposalBatch(currentUserId, proposal)
                .onSuccess {
                    _messages.update { list ->
                        list.map { msg ->
                            if (msg.id == messageId) msg.copy(appliedToCloud = true) else msg
                        }
                    }
                    _savedConfirmationBanner.value = "「${proposal.title}」を旅程タブへ保存しました。"
                }
                .onFailure { err ->
                    _uiState.value = ConciergeUiState.Error(
                        err.localizedMessage ?: "Firestoreへの旅程保存に失敗しました。"
                    )
                }
        }
    }

    fun clearBanner() {
        _savedConfirmationBanner.value = null
    }

    fun clearError() {
        if (_uiState.value is ConciergeUiState.Error) {
            _uiState.value = ConciergeUiState.Idle
        }
    }

    private fun nowLabel(): String =
        SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
}
