package com.sunmobile.aiagent.mlkit

/**
 * Host-neutral configuration for the ML Kit Agent API backend.
 *
 * AppFunctions are declared and implemented by the host application. The SDK only asks the
 * Agent API to make those host declarations available to the model.
 */
data class GeminiNanoConfiguration(
    val agentName: String = "GeminiNanoAgent",
    val instructions: String,
    val maxTurns: Int = 10,
    val minimumAndroidApi: Int = 36,
    val enableAppFunctions: Boolean = true,
) {
    init {
        require(agentName.isNotBlank()) { "agentName must not be blank" }
        require(instructions.isNotBlank()) { "instructions must not be blank" }
        require(maxTurns > 0) { "maxTurns must be greater than zero" }
        require(minimumAndroidApi >= 26) { "minimumAndroidApi cannot be lower than the SDK minSdk (26)" }
    }
}

/** Readiness result; it does not trigger a model download. */
sealed interface GeminiNanoAvailability {
    data object Available : GeminiNanoAvailability
    data class ModelNotReady(val featureStatus: String) : GeminiNanoAvailability
    data class UnsupportedAndroidVersion(val requiredApi: Int, val actualApi: Int) : GeminiNanoAvailability
    data class StatusCheckFailed(val cause: Throwable) : GeminiNanoAvailability
}

enum class GeminiNanoErrorCode {
    UNSUPPORTED_DEVICE,
    MODEL_NOT_READY,
    INITIALIZATION_FAILED,
    INFERENCE_FAILED,
}

/** Typed backend failure that never exposes ML Kit implementation types. */
class GeminiNanoException(
    val code: GeminiNanoErrorCode,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
