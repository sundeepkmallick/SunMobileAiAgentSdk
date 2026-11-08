package com.sunmobile.aiagent.firebase

import com.google.adk.firebase.models.Firebase
import com.google.adk.kt.models.Model
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.sunmobile.aiagent.adk.AdkModelFactory

/**
 * Creates an ADK Firebase AI Logic model from the host application's initialized FirebaseApp.
 * The host owns Firebase initialization, App Check, auth, project configuration and billing.
 */
class FirebaseAiModelFactory(
    private val modelName: String,
    private val firebaseAppProvider: () -> FirebaseApp = { FirebaseApp.getInstance() },
) : AdkModelFactory {
    init { require(modelName.isNotBlank()) { "modelName must not be blank" } }

    override suspend fun createModel(): Model {
        val app = firebaseAppProvider()
        return Firebase.create(modelName, FirebaseAI.getInstance(app))
    }
}
