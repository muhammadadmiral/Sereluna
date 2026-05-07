package com.android.capstone.sereluna.backend.routes

import com.android.capstone.sereluna.backend.auth.FirebaseAuthVerifier
import com.android.capstone.sereluna.backend.model.ChatRequest
import com.android.capstone.sereluna.backend.model.ChatResponse
import com.android.capstone.sereluna.backend.model.ErrorResponse
import com.android.capstone.sereluna.backend.model.UserSession
import com.android.capstone.sereluna.backend.service.GeminiClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext

fun Route.chatRoutes(
    firebaseAuthVerifier: FirebaseAuthVerifier,
    geminiClient: GeminiClient
) {
    post("/journal") {
        val session = requireUserSession(firebaseAuthVerifier) ?: return@post

        val request = runCatching { call.receive<ChatRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body."))
            return@post
        }

        if (request.text.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Text cannot be empty."))
            return@post
        }

        val journal = geminiClient.generateJournalEntry(request.text.trim(), session)
        call.respond(HttpStatusCode.OK, ChatResponse(journal))
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.requireUserSession(
    firebaseAuthVerifier: FirebaseAuthVerifier
): UserSession? {
    val authorization = call.request.header(HttpHeaders.Authorization)
    if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ")) {
        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing bearer token."))
        return null
    }
    val idToken = authorization.removePrefix("Bearer ").trim()
    if (idToken.isEmpty()) {
        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing bearer token."))
        return null
    }

    return runCatching {
        val decoded = firebaseAuthVerifier.verifyIdToken(idToken)
        UserSession(
            uid = decoded.uid,
            email = decoded.email,
            name = decoded.name
        )
    }.getOrElse { throwable ->
        call.application.log.warn("Token verification failed", throwable)
        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid authentication token."))
        null
    }
}
