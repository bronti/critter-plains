package ai

import critters.world.WorldStats
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicRequest(val model: String, val max_tokens: Int, val messages: List<AnthropicMessage>)

@Serializable
private data class AnthropicContentBlock(val type: String, val text: String = "")

@Serializable
private data class AnthropicResponse(val content: List<AnthropicContentBlock>)

/**
 * Periodically narrates the simulation's state via the Anthropic Messages API.
 * Calls are throttled and run off the render thread; any failure (missing key,
 * network error, bad response) degrades to a fallback line instead of crashing the app.
 */
class Chronicler(
    private val model: String = "claude-haiku-4-5",
    private val minTicksBetweenCalls: Int = 100,
) {
    private val apiKey = System.getenv("ANTHROPIC_API_KEY")
    private val enabled = !apiKey.isNullOrBlank()

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var latestChronicle: String = if (enabled) {
        "The plains awaken..."
    } else {
        "(set ANTHROPIC_API_KEY to enable the AI chronicler)"
    }
        private set

    private var lastCallTick = -minTicksBetweenCalls
    private var lastPopulation = -1
    private var inFlight = false

    fun maybeUpdate(stats: WorldStats) {
        if (!enabled || inFlight) return

        val populationDropped = lastPopulation != -1 && stats.population < lastPopulation
        val dueForUpdate = stats.time - lastCallTick >= minTicksBetweenCalls
        lastPopulation = stats.population
        if (!populationDropped && !dueForUpdate) return

        lastCallTick = stats.time
        inFlight = true
        scope.launch {
            latestChronicle = try {
                fetchChronicle(stats)
            } catch (e: Exception) {
                logger.warn(e) { "Chronicler call failed" }
                "(the plains fall briefly silent — AI narrator unavailable)"
            } finally {
                inFlight = false
            }
        }
    }

    private suspend fun fetchChronicle(stats: WorldStats): String {
        val prompt = """
            You narrate a small ecosystem simulation called Critter Plains.
            Tick: ${stats.time}, population: ${stats.population}, average hunger: ${
            "%.1f".format(stats.avgHunger)
        } (scale 0-40, death at 40).
            In one short vivid sentence (max 20 words), describe the current state of the plains. No preamble, no quotes.
        """.trimIndent()

        val requestBody = json.encodeToString(
            AnthropicRequest(
                model = model,
                max_tokens = 60,
                messages = listOf(AnthropicMessage(role = "user", content = prompt)),
            )
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        check(response.statusCode() == 200) {
            "Anthropic API returned ${response.statusCode()}: ${response.body().take(200)}"
        }

        val parsed = json.decodeFromString<AnthropicResponse>(response.body())
        return parsed.content.firstOrNull { it.type == "text" }?.text?.trim() ?: "(empty response)"
    }
}
