# Sereluna Chatbot Backend (Ktor)

This lightweight Ktor server keeps the Gemini API key on the backend, validates Firebase ID tokens, and returns data that matches the Android client's `ChatResponse`.

## Prerequisites

- JDK 17+
- Firebase service account JSON (used for ID token verification). Point `GOOGLE_APPLICATION_CREDENTIALS` at the file.
- Gemini API key stored in `GEMINI_API_KEY` (use Secret Manager or Cloud Run env vars in production).

## Running locally

### Quick start (PowerShell)

1. Download your Firebase service-account JSON for project **melar** and put it at `backend/firebase-service-account.json`. (The filename is git-ignored so it stays private.)
2. Run:

   ```powershell
   .\backend\runLocal.ps1
   ```

   The script exports `GEMINI_API_KEY` with the key you provided and points `GOOGLE_APPLICATION_CREDENTIALS` at the JSON before calling `./gradlew :backend:run`.

### Manual setup

```bash
# from repo root
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
export GEMINI_API_KEY=your-key
./gradlew :backend:run
```

The server listens on `http://localhost:8080` by default (`backend/src/main/resources/application.conf`).

## Request contract

- `POST /journal`
- Headers:
  - `Authorization: Bearer <Firebase ID token>`
  - `Content-Type: application/json`
- Body:

```json
{ "text": "Your diary entry..." }
```

## Response

On success, the endpoint returns:

```json
{
  "journal": {
    "text": "Empathetic summary ...",
    "feeling": "hopeful",
    "suggestion": "Try a short breathing exercise before bed.",
    "_id": "uuid",
    "createdAt": "2024-06-01T12:00:00Z",
    "updatedAt": "2024-06-01T12:00:00Z",
    "__v": 1
  }
}
```

Errors use `{ "message": "..." }` along with appropriate HTTP status codes (`400`, `401`, `502`, `500`).

## Deploying

1. **Build container**: create a Dockerfile (Cloud Run ready) that runs `./gradlew :backend:shadowJar` or `:backend:run`.
2. **Configure secrets**: set `GEMINI_API_KEY` and `GOOGLE_APPLICATION_CREDENTIALS` (or mount Workload Identity) in Cloud Run/Functions.
3. **Networking**: restrict ingress to the Android app/Cloud Armor if needed.
4. **Android integration**: point `ChatbotApiService.BASE_URL` to the deployed HTTPS endpoint (e.g., `https://sereluna-chatbot-xyz.a.run.app/`).

The heavy Gemini prompt logic stays server-side, so you can evolve prompts or add rate limiting/analytics without touching the app.
