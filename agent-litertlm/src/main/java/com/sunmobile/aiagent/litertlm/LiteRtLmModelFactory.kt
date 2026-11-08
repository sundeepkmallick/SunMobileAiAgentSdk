package com.sunmobile.aiagent.litertlm

import com.google.adk.kt.litertlm.LiteRtLmModel
import com.google.adk.kt.models.Model
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EngineConfig
import com.sunmobile.aiagent.adk.AdkModelFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates an ADK LiteRT-LM model from a host-managed, already downloaded model artifact.
 *
 * The SDK deliberately does not download model weights. The host must verify provenance,
 * license, immutable revision, SHA-256, device admission and free storage before construction.
 */
class LiteRtLmModelFactory(
    private val modelFile: File,
    private val cacheDirectory: File,
    private val modelName: String,
    private val backend: Backend = Backend.CPU(),
) : AdkModelFactory {
    init {
        require(modelName.isNotBlank()) { "modelName must not be blank" }
    }

    override suspend fun createModel(): Model = withContext(Dispatchers.IO) {
        require(modelFile.isFile && modelFile.canRead()) {
            "LiteRT-LM model file is missing or unreadable: ${modelFile.absolutePath}"
        }
        check(cacheDirectory.exists() || cacheDirectory.mkdirs()) {
            "Could not create LiteRT-LM cache directory"
        }
        val model = LiteRtLmModel.create(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = backend,
                cacheDir = cacheDirectory.absolutePath,
            ),
            name = modelName,
        )
        try {
            model.engine.initialize()
            model
        } catch (error: Throwable) {
            runCatching { model.close() }
            throw error
        }
    }
}
