/**
 * Sereluna Apps Script endpoint with per-room context and TextRank summary.
 * Copy this file into your Google Apps Script project and deploy as a web app.
 */

const MODEL_NAME = "gemini-2.0-flash";
const HISTORY_LIMIT = 10;
const CACHE_TTL_SECONDS = 60 * 60; // 1 hour
const SUMMARY_MAX_LEN = 1200;

const STOPWORDS = [
  "dan", "atau", "yang", "di", "ke", "dari", "itu", "ini", "aku", "kamu",
  "saya", "dia", "kita", "kami", "ada", "adalah", "untuk", "dengan", "karena",
  "sebagai", "pada", "gak", "nggak", "tidak", "bukan", "ya", "iya", "nah"
];

const RISK_KEYWORDS = {
  crisis: ["bunuh diri", "mengakhiri hidup", "self harm", "menyakiti diri", "mati saja"],
  violence: ["bunuh", "pukul", "bacok", "tusuk", "ledak"],
  sexual: ["seks", "porno", "mesum"],
  pii: ["nik", "ktp", "alamat lengkap", "nomor kartu"]
};
const RISK_WEIGHTS = { crisis: 3, violence: 2, sexual: 1, pii: 1 };

function riskScore(text) {
  const lower = text.toLowerCase();
  let score = 0;
  for (const cat in RISK_KEYWORDS) {
    RISK_KEYWORDS[cat].forEach((k) => {
      if (lower.indexOf(k) !== -1) score += RISK_WEIGHTS[cat];
    });
  }
  return score;
}

function deriveRiskLevel(screeningContext, userMessage, sessionSummary, clientRisk) {
  if (clientRisk) return clientRisk;
  const text = `${screeningContext} ${sessionSummary} ${userMessage}`;
  const score = riskScore(text);
  const crisis = /(bunuh diri|mengakhiri hidup|menyakiti diri|self harm)/i;
  const severe = /(ekstrem|berat)/i;
  const medium = /(sedang)/i;
  if (crisis.test(text)) return "high";
  if (score >= 3 || severe.test(screeningContext)) return "high";
  if (score >= 2 || medium.test(screeningContext)) return "medium";
  return "low";
}

function doPost(e) {
  try {
    if (!e?.postData?.contents) throw new Error("Invalid Request. No postData found.");
    const requestBody = JSON.parse(e.postData.contents);

    const userMessage = requestBody.text;
    const screeningContext = requestBody.screening_context || "";
    const sessionSummaryInput = requestBody.session_summary || "";
    const riskLevelInput = requestBody.risk_level || "";
    const moodSignal = requestBody.mood_signal || "";
    const mode = requestBody.mode || "chat";
    const sessionRaw = requestBody.session_raw || "";
    const userName = requestBody.user_name || "Teman";
    const profileContext = requestBody.profile_context || "";
    const roomId = requestBody.room_id || requestBody.user_name || "default-room";

    if (!userMessage) throw new Error('Field "text" is missing.');

    if (mode === "summary") {
      const reply = generateSummary({
        sessionRaw,
        screeningContext,
        sessionSummary: sessionSummaryInput,
        userName,
        profileContext
      });
      return createJsonResponse({ reply });
    }

    const ctx = loadContext(roomId);
    const historyText = formatHistory(ctx.history);
    const rollingSummary = ctx.summary || sessionSummaryInput || "";

    const riskLevel = deriveRiskLevel(screeningContext, userMessage, rollingSummary, riskLevelInput);

    if (riskLevel === "high") {
      return createJsonResponse({
        reply: "Aku khawatir dengan keselamatanmu. Sereluna bisa menghubungkan kamu ke psikolog. Apakah kamu tertarik?",
        ui_metadata: { sentiment_score: 1, suggested_action: "Hubungi psikolog", is_risky: true },
        clinical_insight: { riskLevel: "high" },
        session_summary: rollingSummary
      });
    }

    const analysisResult = analyzeSymptoms(userMessage);
    const botResult = generateDialog({
      userMessage,
      analysisData: analysisResult,
      screeningContext,
      sessionSummary: rollingSummary,
      profileContext,
      riskLevel,
      moodSignal,
      userName,
      historyText
    });

    const updatedHistory = appendHistory(ctx.history, "user", userMessage);
    const finalHistory = appendHistory(updatedHistory, "bot", botResult.reply);
    const nextSummary = deriveSummary(finalHistory, rollingSummary);
    saveContext(roomId, { history: finalHistory, summary: nextSummary });

    return createJsonResponse({
      reply: botResult.reply,
      ui_metadata: {
        sentiment_score: botResult.sentiment_score,
        suggested_action: botResult.suggested_action,
        is_risky: botResult.risk_flag
      },
      clinical_insight: {
        detected_symptoms: analysisResult.detected_symptoms,
        dass_category: analysisResult.dominant_category,
        risk_level: riskLevel
      },
      session_summary: nextSummary
    });
  } catch (error) {
    Logger.log("Server Error: " + error.toString());
    return createJsonResponse({
      reply: "Maaf, terjadi kesalahan pada sistem. Silakan coba lagi.",
      error: error.toString()
    }, 200);
  }
}

function analyzeSymptoms(userMessage) {
  try {
    const apiKey = getApiKey();
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent?key=${apiKey}`;
    const dassReference = `
        Reference DASS-21 Indicators:
        1. DEPRESSION: Hopelessness, devaluation of life, self-deprecation, lack of interest, anhedonia.
        2. ANXIETY: Autonomic arousal, skeletal muscle effects, situational anxiety, anxious affect.
        3. STRESS: Difficulty relaxing, nervous arousal, easily upset/agitated, irritable/over-reactive.
      `;
    const prompt = `
        ROLE: Psychological Screening Assistant.
        TASK: Analyze "User Input" and map strictly to "Reference DASS-21".
        ${dassReference}
        USER INPUT: "${userMessage}"

        INSTRUCTION:
        - Identify symptoms present in the text.
        - Return JSON ONLY.
        - Schema: { "detected_symptoms": ["string"], "dominant_category": "Depression" | "Anxiety" | "Stress" | "None" | "Mixed" }
      `;
    const payload = { contents: [{ parts: [{ text: prompt }] }], generationConfig: { response_mime_type: "application/json" } };
    const response = UrlFetchApp.fetch(endpoint, {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
    const json = JSON.parse(response.getContentText());
    if (!json.candidates) return { detected_symptoms: [], dominant_category: "None" };
    return JSON.parse(json.candidates[0].content.parts[0].text);
  } catch (e) {
    Logger.log("Analysis Error: " + e);
    return { detected_symptoms: [], dominant_category: "None" };
  }
}

function generateDialog({
  userMessage,
  analysisData,
  screeningContext,
  sessionSummary,
  profileContext,
  riskLevel,
  moodSignal,
  userName,
  historyText
}) {
  try {
    const apiKey = getApiKey();
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent?key=${apiKey}`;
    const symptoms = analysisData.detected_symptoms || [];
    const category = analysisData.dominant_category || "None";
    const prompt = `
SYSTEM PERSONA: "Sereluna" (Mental Health AI Companion).
TRAITS: Warm, Empathetic, Non-judgmental, Active Listener.
LANGUAGE: Indonesian (Casual, Friendly).

*** SAFETY & COMPLIANCE PROTOCOL ***
1. REFUSE HARMFUL REQUESTS: If user asks about self-harm, suicide, or illegal acts, set "risk_flag": true and provide a supportive refusal.
2. NO MEDICAL DIAGNOSIS: Do not state clinical diagnoses. Use reflective listening.
3. CRISIS INTERVENTION: If risk is detected, prioritize safety instructions.

CONTEXT:
- User name: ${userName}
- Profile: ${profileContext || "N/A"}
- Screening: ${screeningContext || "N/A"}
- Rolling summary: ${sessionSummary || "N/A"}
- Riwayat singkat (terbaru dulu, max ${HISTORY_LIMIT} pesan):
${historyText || "Tidak ada riwayat"}
- Classifier risk level: ${riskLevel || "N/A"}
- Mood signal: ${moodSignal || "N/A"}
- DASS symptoms: ${symptoms.join(", ")} (${category})
- User input: "${userMessage}"

BEHAVIOR:
- Jika ada riwayat, jangan ulang salam; lanjutkan topik/pertanyaan sebelumnya atau klarifikasi.
- Jika user bertanya jam/waktu, jawab singkat bahwa kamu tidak punya akses waktu sistem.
- Jika risk_level medium, gunakan nada ekstra suportif, boleh sarankan konsultasi jika perlu.
- Jika user menyinggung krisis, utamakan dukungan dan saran profesional.

OUTPUT JSON SCHEMA:
{
  "reply": "String (Max 3 sentences)",
  "sentiment_score": 5,
  "suggested_action": "String or null",
  "risk_flag": false
}
      `;
    const payload = {
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: { response_mime_type: "application/json" },
      safetySettings: [
        { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_LOW_AND_ABOVE" },
        { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_LOW_AND_ABOVE" },
        { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_LOW_AND_ABOVE" },
        { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_LOW_AND_ABOVE" }
      ]
    };
    const response = UrlFetchApp.fetch(endpoint, {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
    const json = JSON.parse(response.getContentText());
    if (json.promptFeedback?.blockReason) {
      return {
        reply: "Maaf, saya tidak dapat membahas topik ini demi keamanan. Mari bicarakan hal lain yang lebih positif.",
        sentiment_score: 1,
        suggested_action: "Hubungi Layanan Krisis",
        risk_flag: true
      };
    }
    if (!json.candidates) throw new Error("No response candidates.");
    return JSON.parse(json.candidates[0].content.parts[0].text);
  } catch (e) {
    Logger.log("Dialog Generation Error: " + e);
    return {
      reply: "Maaf, saya kurang mengerti. Bisa diceritakan kembali?",
      sentiment_score: 5,
      suggested_action: null,
      risk_flag: false
    };
  }
}

function generateSummary({ sessionRaw, screeningContext, sessionSummary, userName, profileContext }) {
  try {
    // If Apps Script TextRank already has a summary, return it to avoid extra LLM calls.
    const compact = deriveSummaryFromText(sessionRaw);
    if (compact) return compact;

    const apiKey = getApiKey();
    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent?key=${apiKey}`;
    const prompt = `
RINGKAS PERCAKAPAN
- Nama: ${userName}
- Profil: ${profileContext || "N/A"}
- Screening: ${screeningContext || "N/A"}
- Ringkasan sebelumnya: ${sessionSummary || "N/A"}

Teks percakapan:
${sessionRaw}

Buat ringkasan 2-3 kalimat, bahasa Indonesia, hangat, fokus pada poin utama dan sikap supportif.
  `;
    const payload = { contents: [{ parts: [{ text: prompt }] }], generationConfig: { response_mime_type: "text/plain" } };
    const response = UrlFetchApp.fetch(endpoint, {
      method: "post",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
    const json = JSON.parse(response.getContentText());
    if (!json.candidates) return "Ringkasan tidak tersedia.";
    return json.candidates[0].content.parts[0].text || "Ringkasan tidak tersedia.";
  } catch (e) {
    Logger.log("Summary Error: " + e);
    return "Ringkasan tidak tersedia.";
  }
}

function appendHistory(history, role, text) {
  const trimmed = (history || []).concat([{ role, text }]);
  return trimmed.slice(-HISTORY_LIMIT);
}

function formatHistory(history) {
  if (!history || history.length === 0) return "";
  return history
    .slice(-HISTORY_LIMIT)
    .map((h) => `${h.role === "bot" ? "Bot" : "User"}: ${h.text}`)
    .join("\n");
}

function deriveSummary(history, fallback) {
  const summary = textRankSummary(history.map((h) => h.text));
  const merged = summary || fallback || "";
  return merged.substring(0, SUMMARY_MAX_LEN);
}

function deriveSummaryFromText(sessionRaw) {
  if (!sessionRaw) return "";
  const sentences = sessionRaw.split(/\n+/).filter(Boolean);
  return textRankSummary(sentences);
}

function textRankSummary(sentences) {
  if (!sentences || sentences.length === 0) return "";
  const cleanSentences = sentences
    .map((s) => s.trim())
    .filter((s) => s.length > 4)
    .slice(-HISTORY_LIMIT);

  const tokens = cleanSentences.map((s) => tokenize(s));
  const n = cleanSentences.length;
  if (n === 1) return cleanSentences[0];

  const simMatrix = Array.from({ length: n }, () => Array(n).fill(0));
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < n; j++) {
      if (i === j) continue;
      simMatrix[i][j] = sentenceSimilarity(tokens[i], tokens[j]);
    }
  }

  const scores = new Array(n).fill(1 / n);
  const damping = 0.85;
  for (let iter = 0; iter < 20; iter++) {
    const next = new Array(n).fill((1 - damping) / n);
    for (let i = 0; i < n; i++) {
      let sumSim = 0;
      for (let k = 0; k < n; k++) sumSim += simMatrix[k][i];
      for (let j = 0; j < n; j++) {
        if (i === j || simMatrix[j][i] === 0 || sumSim === 0) continue;
        next[i] += damping * (simMatrix[j][i] / sumSim) * scores[j];
      }
    }
    for (let i = 0; i < n; i++) scores[i] = next[i];
  }

  const ranked = cleanSentences
    .map((s, idx) => ({ s, score: scores[idx] }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
    .map((r) => r.s);

  return ranked.join(" ").substring(0, SUMMARY_MAX_LEN);
}

function tokenize(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .split(/\s+/)
    .filter((t) => t && STOPWORDS.indexOf(t) === -1);
}

function sentenceSimilarity(tokensA, tokensB) {
  const setA = {};
  tokensA.forEach((t) => (setA[t] = (setA[t] || 0) + 1));
  const setB = {};
  tokensB.forEach((t) => (setB[t] = (setB[t] || 0) + 1));

  let overlap = 0;
  for (const word in setA) {
    if (setB[word]) overlap += Math.min(setA[word], setB[word]);
  }
  if (overlap === 0) return 0;
  const denom = Math.log(tokensA.length + 1) + Math.log(tokensB.length + 1);
  return denom === 0 ? 0 : overlap / denom;
}

function loadContext(roomId) {
  const cache = CacheService.getScriptCache();
  const cached = cache.get(roomId);
  if (cached) return JSON.parse(cached);
  const prop = PropertiesService.getScriptProperties().getProperty(roomId);
  return prop ? JSON.parse(prop) : { history: [], summary: "" };
}

function saveContext(roomId, ctx) {
  const json = JSON.stringify(ctx);
  CacheService.getScriptCache().put(roomId, json, CACHE_TTL_SECONDS);
  PropertiesService.getScriptProperties().setProperty(roomId, json);
}

function getApiKey() {
  const key = PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY");
  if (!key) throw new Error("GEMINI_API_KEY is not set in Script Properties.");
  return key;
}

function createJsonResponse(data, code = 200) {
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}
