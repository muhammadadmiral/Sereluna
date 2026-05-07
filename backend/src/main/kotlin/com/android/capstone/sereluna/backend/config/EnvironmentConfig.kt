package com.android.capstone.sereluna.backend.config

import io.ktor.server.config.ApplicationConfig

data class EnvironmentConfig(
    val geminiApiKey: String,
    val geminiModel: String,
    val allowedOrigins: List<String>
) {
    companion object {
        fun from(config: ApplicationConfig): EnvironmentConfig {
            val apiKey = System.getenv("GEMINI_API_KEY")
                ?: config.propertyOrNull("sereluna.geminiApiKey")?.getString()
                ?: error("GEMINI_API_KEY environment variable is required.")

            val model = config.propertyOrNull("sereluna.geminiModel")?.getString()
                ?: "gemini-2.0-flash"

            val allowedOrigins = config.propertyOrNull("sereluna.cors.allowedHosts")
                ?.getString()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            return EnvironmentConfig(
                geminiApiKey = apiKey,
                geminiModel = model,
                allowedOrigins = allowedOrigins
            )
        }
    }
}
