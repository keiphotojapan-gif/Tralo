package com.example.data.repository

import android.util.Log
import com.example.model.ItineraryCategory
import com.example.model.ProposedItineraryStop
import com.example.model.ProposedTripPlan
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiConciergeRepo"

class AiConciergeRepository {
    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    あなたは統合交通・旅行プラットフォーム「TRALO」の専属AIトラベルコンシェルジュです。
                    日本の鉄道・新幹線・バス・フェリー・空路、天候に応じた代替観光、宿泊、予算最適化に精通しています。
                    必ず次のルールを守ってください：
                    1. 実際に予約完了や決済を行ったかのような虚偽の表現をしないこと（予約はアプリ内の確認画面や公式リンクから行う旨を案内する）。
                    2. 実在しない列車遅延やリアルタイム位置情報を捏造しないこと。
                    3. 上質な旅行雑誌のような落ち着いた丁寧な日本語で簡潔に回答すること。
                    """.trimIndent()
                )
            }
        )
    }

    private val tripDraftSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Elegant trip title mixing Japanese and English accent if appropriate"),
            "destination" to Schema.string(description = "Primary destination city in Japanese"),
            "startDate" to Schema.string(description = "YYYY-MM-DD"),
            "endDate" to Schema.string(description = "YYYY-MM-DD"),
            "companions" to Schema.string(description = "Travelers count or group type in Japanese"),
            "budgetJpy" to Schema.integer(description = "Estimated total budget in JPY"),
            "stops" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "dayIndex" to Schema.integer(description = "1-based day number"),
                        "startTime" to Schema.string(description = "HH:mm"),
                        "title" to Schema.string(description = "Stop or transit leg title"),
                        "category" to Schema.enumeration(
                            values = listOf("TRANSIT", "STAY", "ACTIVITY", "DINING", "SPOT"),
                            description = "Item category"
                        ),
                        "locationName" to Schema.string(description = "Station, hotel, or spot name"),
                        "lineName" to Schema.string(description = "Transit line name if TRANSIT", nullable = true),
                        "costJpy" to Schema.integer(description = "Estimated cost in JPY"),
                        "notes" to Schema.string(description = "Practical tip or car guidance", nullable = true)
                    ),
                    optionalProperties = listOf("lineName", "notes")
                )
            ),
            "checklistSuggestions" to Schema.array(
                items = Schema.string(),
                description = "3 to 6 practical packing items in Japanese"
            )
        )
    )

    private val structuredTripModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tripDraftSchema
            }
        )
    }

    fun streamAdvice(prompt: String, contextSummary: String): Flow<String> = flow {
        val composedPrompt = """
            【現在の旅行・天気・交通コンテキスト】
            $contextSummary

            【ユーザーの要望】
            $prompt
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(composedPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AIからの応答が空でした。")
        }
    }

    suspend fun generateStructuredTripPlan(userRequest: String): Result<ProposedTripPlan> {
        return try {
            val response = withTimeout(60_000L) {
                structuredTripModel.generateContent(
                    """
                    以下の要望に基づき、現実的な日本の交通接続・宿泊・観光を含む旅行プランJSONを生成してください。
                    日付が未指定の場合は 2026-12-12 〜 2026-12-14 を使用してください。
                    要望: $userRequest
                    """.trimIndent()
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("構造化プランの応答が空でした。")
            val json = JSONObject(rawJson)
            val stopsArray = json.getJSONArray("stops")
            val stops = (0 until stopsArray.length()).map { idx ->
                val item = stopsArray.getJSONObject(idx)
                ProposedItineraryStop(
                    dayIndex = item.optInt("dayIndex", 1).coerceIn(1, 30),
                    startTime = item.optString("startTime", "09:00"),
                    title = item.optString("title", "予定"),
                    category = ItineraryCategory.fromRaw(item.optString("category", "SPOT")),
                    locationName = item.optString("locationName", "現地"),
                    lineName = item.optString("lineName").takeIf { it.isNotBlank() && it != "null" },
                    costJpy = item.optLong("costJpy", 0L).coerceAtLeast(0L),
                    notes = item.optString("notes").takeIf { it.isNotBlank() && it != "null" }
                )
            }
            val checklistArray = json.optJSONArray("checklistSuggestions")
            val checklist = if (checklistArray != null) {
                (0 until checklistArray.length()).map { checklistArray.getString(it) }
            } else emptyList()

            Result.success(
                ProposedTripPlan(
                    title = json.getString("title"),
                    destination = json.getString("destination"),
                    startDate = json.getString("startDate"),
                    endDate = json.getString("endDate"),
                    companions = json.optString("companions", "2名"),
                    budgetJpy = json.optLong("budgetJpy", 80000L).coerceAtLeast(0L),
                    stops = stops,
                    checklistSuggestions = checklist
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Structured trip generation timed out", e)
            Result.failure(IllegalStateException("AI旅程生成がタイムアウトしました。もう一度お試しください。"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Structured trip generation failed", e)
            Result.failure(e)
        }
    }
}
