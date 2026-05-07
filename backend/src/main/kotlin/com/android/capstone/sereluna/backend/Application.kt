package com.android.capstone.sereluna.backend

import com.android.capstone.sereluna.backend.auth.FirebaseAuthVerifier
import com.android.capstone.sereluna.backend.config.EnvironmentConfig
import com.android.capstone.sereluna.backend.model.ErrorResponse
import com.android.capstone.sereluna.backend.routes.chatRoutes
import com.android.capstone.sereluna.backend.service.GeminiClient
import com.android.capstone.sereluna.backend.service.GeminiClientException
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.http.HttpMethod
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.cors.routing.anyHost
import io.ktor.server.plugins.cors.routing.allowHeader
import io.ktor.server.plugins.cors.routing.allowHost
import io.ktor.server.plugins.cors.routing.allowMethod
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val appLogger = log
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    val envConfig = EnvironmentConfig.from(environment.config)

    val httpClient = HttpClient(Java) {
        install(ClientContentNegotiation) {
            json(json)
        }
    }

    val firebaseAuthVerifier = FirebaseAuthVerifier()
    val geminiClient = GeminiClient(
        httpClient = httpClient,
        apiKey = envConfig.geminiApiKey,
        model = envConfig.geminiModel,
        json = json
    )

    install(DefaultHeaders)

    install(CallLogging) {
        level = Level.INFO
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        if (envConfig.allowedOrigins.isEmpty()) {
            anyHost()
        } else {
            envConfig.allowedOrigins.forEach { origin ->
                allowHost(origin, schemes = listOf("http", "https"))
            }
        }
    }

    install(StatusPages) {
        exception<GeminiClientException> { call, cause ->
            appLogger.error("Gemini call failed", cause)
            call.respond(
                io.ktor.http.HttpStatusCode.BadGateway,
                ErrorResponse("Unable to generate journal entry right now.")
            )
        }
        exception<Throwable> { call, cause ->
            appLogger.error("Unhandled server error", cause)
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                ErrorResponse("Unexpected server error.")
            )
        }
    }

    routing {
        chatRoutes(firebaseAuthVerifier, geminiClient)
    }

    environment.monitor.subscribe(ApplicationStopped) {
        httpClient.close()
    }
}
