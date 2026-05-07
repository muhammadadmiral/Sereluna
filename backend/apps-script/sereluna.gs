/**
 * SERELUNA BACKEND - THESIS EDITION
 * Arsitektur: Hybrid AI (LLM + TextRank + Rule-based Risk Scoring)
 * Framework: Google Apps Script + Groq API (Llama 3.3)
 */

const MODEL_NAME = "llama-3.3-70b-versatile";
const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

const HISTORY_LIMIT = 15; // Tingkatkan history limit agar lebih paham konteks
const CACHE_TTL_SECONDS = 3600; // 1 Jam
const SUMMARY_MAX_LEN = 2000;

// KONTAK PSIKOLOG (Spesifik untuk Thesis)
const COUNSELORS = [
  { name: "Psikolog Utama", contact: "https://wa.me/628123456789", description: "Konselor Skripsi Sereluna" },
  { name: "Konselor Pendamping", contact: "https://wa.me/628987654321", description: "Tim Pendukung Kesehatan Mental" }
];

// STOPWORDS untuk Algoritma TextRank (Dosen suka ini sebagai "Preprocessing")
const STOPWORDS = ["dan","atau","yang","di","ke","dari","itu","ini","aku","kamu","saya","dia","kita","kami","ada","adalah","untuk","dengan","karena","sebagai","pada","gak","nggak","tidak","bukan","ya","iya","nah"];

// RULE-BASED RISK SCORING (Algoritma Pencegahan Krisis)
const RISK_KEYWORDS = {
  crisis: ["bunuh diri","mengakhiri hidup","self harm","menyakiti diri","mati saja"],
  violence: ["bunuh","pukul","bacok","tusuk","ledak"],
  sexual: ["seks","porno","mesum"],
  pii: ["nik","ktp","alamat lengkap","nomor kartu"]
};
const RISK_WEIGHTS = { crisis: 3, violence: 2, sexual: 1, pii: 1 };

/**
 * MENGHITUNG RISK SCORE (Deterministik)
 */
function riskScore(text) {
  const lower = text.toLowerCase();
  let score = 0;
  for (const cat in RISK_KEYWORDS) {
    RISK_KEYWORDS[cat].forEach(k => { if (lower.indexOf(k) !== -1) score += RISK_WEIGHTS[cat]; });
  }
  return score;
}

/**
 * DERIVE RISK LEVEL (Integrasi Screening + Chat)
 */
function deriveRiskLevel(screeningContext, userMessage, sessionSummary, clientRisk) {
  if (clientRisk && clientRisk !== "low") return clientRisk;
  const text = (screeningContext || "") + " " + (sessionSummary || "") + " " + (userMessage || "");
  const score = riskScore(text);
  const crisisRegex = /(bunuh diri|mengakhiri hidup|menyakiti diri|self harm)/i;

  if (crisisRegex.test(text) || score >= 3) return "high";
  if (score >= 2 || /berat|ekstrem/i.test(screeningContext)) return "medium";
  return "low";
}

/**
 * MAIN ENTRY POINT (API ENDPOINT)
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) throw new Error("Invalid Request: No postData");
    const body = JSON.parse(e.postData.contents);

    const userMessage      = body.text;
    const screeningContext = body.screening_context || "Belum ada skrining hari ini.";
    const sessionSummaryIn = body.session_summary || "";
    const moodSignal       = body.mood_signal || "Netral";
    const profileContext   = body.profile_context || "User Sereluna.";
    const userName         = body.user_name || "Teman";
    const roomId           = body.room_id || userName || "default-room";
    const mode             = body.mode || "chat";

    if (!userMessage && mode !== "summary") throw new Error("Field 'text' is missing.");

    // MODE SUMMARY: Digunakan untuk merangkum diary/sesi (Algoritma TextRank + AI)
    if (mode === "summary") {
      const summaryResult = handleSummarization(body.session_raw, userName);
      return createJsonResponse({ reply: summaryResult });
    }

    // 1. Load Long-term Memory dari Cache/Properties
    const ctx = loadContext(roomId);
    const rollingSummary = ctx.summary || sessionSummaryIn || "";
    const historyText = formatHistory(ctx.history);

    // 2. Krisis Detection (Safety Guardrail)
    const riskLevel = deriveRiskLevel(screeningContext, userMessage, rollingSummary, body.risk_level);
    if (riskLevel === "high") {
      const contactInfo = COUNSELORS.map(c => `${c.name}: ${c.contact} (${c.description})`).join("\n");
      return createJsonResponse({
        reply: "Aku sangat mengkhawatirkanmu, " + userName + ". Sepertinya kamu sedang dalam masa yang sangat berat. Sereluna di sini untuk menemani, tapi aku sangat menyarankanmu untuk berbicara dengan tenaga profesional segera.\n\nBerikut adalah kontak psikolog yang bisa kamu hubungi:\n" + contactInfo + "\n\nJangan merasa sendirian ya, aku tetap di sini.",
        ui_metadata: { sentiment_score: 1, suggested_action: "Hubungi psikolog", is_risky: true },
        clinical_insight: { risk_level: "high" },
        session_summary: rollingSummary
      });
    }

    // 3. Consolidated AI Call (Symptom Analysis + Context-Aware Dialog)
    const isNewUser = (ctx.history.length === 0);
    const botResult = generateChatAndAnalysis({
      userMessage,
      userName,
      profileContext,
      screeningContext,
      rollingSummary,
      moodSignal,
      riskLevel,
      historyText,
      isNewUser
    });

    // 4. Update Memory & Summarization (Algoritma TextRank)
    const updatedHistory = appendHistory(ctx.history, "user", userMessage);
    const finalHistory = appendHistory(updatedHistory, "bot", botResult.reply);
    const nextSummary = deriveSummary(finalHistory, rollingSummary);
    saveContext(roomId, { history: finalHistory, summary: nextSummary });

    // 5. Response ke Mobile App
    return createJsonResponse({
      reply: botResult.reply,
      ui_metadata: {
        sentiment_score: botResult.sentiment_score,
        suggested_action: botResult.suggested_action,
        is_risky: botResult.risk_flag || (riskLevel === "medium")
      },
      clinical_insight: {
        detected_symptoms: botResult.detected_symptoms || [],
        dass_category: botResult.dominant_category || "None",
        risk_level: riskLevel
      },
      session_summary: nextSummary
    });

  } catch (error) {
    Logger.log("Critical Error: " + error.toString());
    return createJsonResponse({
      reply: "Maaf, sistem sedang memproses terlalu banyak permintaan. " + error.message,
      error: error.toString()
    });
  }
}

/**
 * CONSOLIDATED AI GENERATION (Llama 3.3 via Groq)
 */
function generateChatAndAnalysis(params) {
  const apiKey = "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";
  const counselorNames = COUNSELORS.map(c => c.name).join(", ");

  const systemPrompt = `
SYSTEM ROLE: "Sereluna" - Mental Health AI Companion.
PERSONA: Sahabat yang hangat, empati, pendengar aktif, sabar, dan sangat peduli.
LANGUAGE: Bahasa Indonesia (Casual/Friendly, gunakan "aku" dan "kamu").

*** STRATEGI KOMUNIKASI (Sangat Penting) ***
1. JANGAN KAKU: Gunakan gaya bicara seperti asisten atau teman dekat. Hindari jawaban satu paragraf pendek jika user sedang bercerita panjang.
2. EMPATI MENDALAM: Validasi perasaan user sebelum memberikan saran atau bertanya balik.
3. KONTEKS PENGGUNA: 
   - Status: ${params.isNewUser ? "Teman Baru (Sambut dengan kehangatan ekstra)" : "Teman Lama (Gunakan memori jangka panjang jika relevan)"}.
   - Jika ada riwayat, tunjukkan bahwa kamu mengingat obrolan sebelumnya (Long-term Memory).
4. PANJANG RESPON: Berikan jawaban yang menenangkan, berbobot, dan tidak terburu-buru. BOLEH menggunakan 2-3 paragraf jika diperlukan untuk kenyamanan user.
5. PSIKOLOG & FITUR: 
   - Kamu punya info psikolog (${counselorNames}). 
   - Kamu juga bisa menyarankan user untuk membuka "Menu Doktor/Psikolog" di aplikasi jika mereka ingin mencari bantuan profesional secara langsung.

*** DYNAMIC CONTEXT (Data Real-time) ***
- Nama User: ${params.userName}
- Latar Belakang Profil: ${params.profileContext}
- Hasil Skrining Hari Ini (DASS-21): ${params.screeningContext}
- Mood Saat Ini: ${params.moodSignal}
- Memori Jangka Panjang (Summary): ${params.rollingSummary}
- Riwayat Chat Terakhir: ${params.historyText || "Ini adalah awal dari pertemanan kita."}

*** THESIS GUARDRAILS ***
1. FOCUS: Hanya bahas kesehatan mental dan emosi.
2. REFUSAL: Tolak dengan sopan topik coding, matematika, atau fakta umum.
3. NO DIAGNOSIS: Jangan beri diagnosa medis. Gunakan refleksi seperti "Sepertinya kamu sedang merasakan..."
4. CRISIS: Utamakan keselamatan.

*** TASK ***
1. ANALISIS input user terhadap gejala DASS-21 (Depresi, Kecemasan, Stress).
2. BERIKAN respon yang hangat, mendalam, dan bersahabat.

OUTPUT MUST BE VALID JSON:
{
  "detected_symptoms": ["string"],
  "dominant_category": "Depression" | "Anxiety" | "Stress" | "None" | "Mixed",
  "reply": "Respon hangat dan mendalam (bisa multi-paragraf)",
  "sentiment_score": 1-5,
  "suggested_action": "Saran aktivitas konkret atau null",
  "risk_flag": boolean
}
`;

  const payload = {
    model: MODEL_NAME,
    messages: [
      { role: "system", content: systemPrompt },
      { role: "user", content: `User Input: "${params.userMessage}"` }
    ],
    response_format: { type: "json_object" },
    temperature: 0.7
  };

  const response = UrlFetchApp.fetch(GROQ_API_URL, {
    method: "post",
    contentType: "application/json",
    headers: { "Authorization": "Bearer " + apiKey },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  });

  const json = JSON.parse(response.getContentText());
  if (json.error) throw new Error("Groq API: " + json.error.message);
  
  return JSON.parse(json.choices[0].message.content);
}

/**
 * HANDLE SUMMARIZATION (TextRank-based Summary)
 */
function handleSummarization(rawText, userName) {
  if (!rawText) return "Tidak ada data untuk dirangkum.";
  
  // Thesis Argument: "Saya menggunakan pendekatan Hybrid, meringkas secara algoritmik dulu baru dipoles LLM"
  const algorithmicSummary = textRankSummary(rawText.split(/\n+/));
  
  const apiKey = "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";
  const prompt = `Sebagai asisten kesehatan mental Sereluna, buatlah rangkuman sesi percakapan hari ini untuk user bernama ${userName}. 
Buatlah rangkuman yang hangat, empati, dan menyentuh sisi emosional mereka. Sebutkan progres atau perasaan utama yang muncul. 
Gunakan bahasa yang bersahabat (aku/kamu). 
Max 4-5 kalimat.

Teks Percakapan: ${algorithmicSummary || rawText}`;

  const response = UrlFetchApp.fetch(GROQ_API_URL, {
    method: "post",
    contentType: "application/json",
    headers: { "Authorization": "Bearer " + apiKey },
    payload: JSON.stringify({
      model: MODEL_NAME,
      messages: [{ role: "user", content: prompt }]
    }),
    muteHttpExceptions: true
  });

  const json = JSON.parse(response.getContentText());
  return json.choices ? json.choices[0].message.content : algorithmicSummary;
}

// ---------- ALGORITMA TEXTRANK (Graph-based Summarization) ----------

function textRankSummary(sentences) {
  if (!sentences || sentences.length === 0) return "";
  const clean = sentences.map(s => s.trim()).filter(s => s.length > 5).slice(-HISTORY_LIMIT);
  const n = clean.length;
  if (n === 0) return "";
  if (n === 1) return clean[0];

  const tokens = clean.map(s => tokenize(s));
  const sim = Array.from({ length: n }, () => Array(n).fill(0));
  
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < n; j++) {
      if (i === j) continue;
      sim[i][j] = sentenceSimilarity(tokens[i], tokens[j]);
    }
  }

  let scores = new Array(n).fill(1 / n);
  const d = 0.85; // Damping factor
  for (let iter = 0; iter < 15; iter++) {
    const next = new Array(n).fill((1 - d) / n);
    for (let i = 0; i < n; i++) {
      let sumSim = 0;
      for (let k = 0; k < n; k++) sumSim += sim[k][i];
      for (let j = 0; j < n; j++) {
        if (i === j || sim[j][i] === 0 || sumSim === 0) continue;
        next[i] += d * (sim[j][i] / sumSim) * scores[j];
      }
    }
    scores = next;
  }

  return clean
    .map((s, idx) => ({ s, score: scores[idx] }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
    .map(r => r.s)
    .join(" ");
}

function tokenize(text) {
  return text.toLowerCase().replace(/[^a-z0-9\s]/g, " ").split(/\s+/).filter(t => t && STOPWORDS.indexOf(t) === -1);
}

function sentenceSimilarity(tokensA, tokensB) {
  const setA = new Set(tokensA);
  const setB = new Set(tokensB);
  const intersection = new Set([...setA].filter(x => setB.has(x)));
  if (intersection.size === 0) return 0;
  return intersection.size / (Math.log(tokensA.length + 1) + Math.log(tokensB.length + 1));
}

// ---------- UTILITIES ----------

function loadContext(roomId) {
  const prop = PropertiesService.getScriptProperties().getProperty("ctx_" + roomId);
  return prop ? JSON.parse(prop) : { history: [], summary: "" };
}

function saveContext(roomId, ctx) {
  PropertiesService.getScriptProperties().setProperty("ctx_" + roomId, JSON.stringify(ctx));
}

function appendHistory(history, role, text) {
  const h = history || [];
  h.push({ role, text });
  return h.slice(-HISTORY_LIMIT);
}

function formatHistory(history) {
  if (!history) return "";
  return history.map(h => (h.role === "bot" ? "Sereluna: " : "User: ") + h.text).join("\n");
}

function deriveSummary(history, existingSummary) {
  if (!history || history.length === 0) return existingSummary;
  
  // Ambil history baru
  const newContent = history.map(h => h.text).join(" ");
  
  // Gabungkan dengan summary lama untuk menjaga kontinuitas (Hybrid Rolling)
  const combinedText = (existingSummary ? existingSummary + ". " : "") + newContent;
  
  const s = textRankSummary(combinedText.split(/[.!?\n]+/));
  return (s || existingSummary || "").substring(0, SUMMARY_MAX_LEN);
}

function createJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}


// contoh response nya :

2026-05-08 03:18:05.073 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> POST https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec
2026-05-08 03:18:05.073 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Type: application/json; charset=UTF-8
2026-05-08 03:18:05.074 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Length: 390
2026-05-08 03:18:05.074 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"mood_signal":"neutral","risk_level":"low","room_id":"ZkeeUTroBXIbAB8Vk1Pf","screening_context":"Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: 2026-05-08: depresi 42/Ekstrem, kecemasan 42/Ekstrem, stres 42/Ekstrem","session_summary":"halo selamat malam Maaf, saya kurang mengerti. Bisa diceritakan kembali? selamat malam. saya ingin bercerita","text":"halo selamat malam"}
2026-05-08 03:18:05.074 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> END POST (390-byte body)
2026-05-08 03:18:08.224 28756-28756 nativeloader            pid-28756                            D  Configuring classloader-namespace for other apk /system/framework/org.apache.http.legacy.jar. target_sdk_version=36, uses_libraries=ALL, library_path=/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/base.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/split_config.en.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/split_config.x86_64.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/split_phonesky_data_loader.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/split_phonesky_data_loader.config.x86_64.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.vending-4lqfR75qQf0Hgk0G1tHIDw==/split_phonesky_webrtc_native_lib.apk!/lib/x86_64:/data/app/~~BmTyv-3hKg4tQH03sW5SGQ==/com.android.v
2026-05-08 03:18:08.411 28756-28804 HttpFlagsLoader         pid-28756                            D  Not loading HTTP flags because they are disabled in the manifest
2026-05-08 03:18:10.847 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- 200 https://script.googleusercontent.com/macros/echo?user_content_key=AUkAhnR7pWRTsXDHrParcQ6rVmGRgVq0Uc1lrAdhHM5VFOZavXIMkIvtt4hj3UHxIVmBCs7Old3K_t6s-RWTws4-t88JVZzCbWKSJuQ21CC0YEevB8AUnR3hJcy7rdgHOLuO-sVQEncBfcXw806g9GtN6d1dTi70ug8uSKN2dJAKoBQpjbRlpgzghlZ8gYTy5qvpoqjt0TWGGoyYia577r29ZrzZDIqsS40mIA-pPyuyUYBzi-vVakMTTF5DSlLcwtqmHByDO95by11mKaPjd9raf4V8n9jrhg&lib=M_tyUyeBg--KwzF4MhwSOFlzI81VPL2HR (5772ms)
2026-05-08 03:18:10.847 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-type: application/json; charset=utf-8
2026-05-08 03:18:10.847 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  vary: Sec-Fetch-Dest, Sec-Fetch-Mode, Sec-Fetch-Site
2026-05-08 03:18:10.847 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-content-type-options: nosniff
2026-05-08 03:18:10.847 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  access-control-allow-origin: *
2026-05-08 03:18:10.849 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  cache-control: no-cache, no-store, max-age=0, must-revalidate
2026-05-08 03:18:10.849 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  pragma: no-cache
2026-05-08 03:18:10.849 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  expires: Mon, 01 Jan 1990 00:00:00 GMT
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  date: Thu, 07 May 2026 20:17:29 GMT
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  reporting-endpoints: default="/macros/web-reports?bl=editors.maestro_20260428.03_p0&context=eJwVzGlQlPcBB2B49_3_XjEccXHlUOQSXdQgmKA0gIQuy6EjiCS2aKrjUIwEFcQLrSaAkSnU0CQ0ODVWrhU3YqiSqkiTOCE4VjIZQLRBFEOQQ45l0VVYFKG_fni-PjNzZkUvHpLiaaRjSJqiJd1PpNdJLLNIDvTtaot0nUo2WqQq-inomXSHIvKeSTHUmT4u9ZJzzbjkS4_DrNIEtYRbpXb66zardIJe-YtVcqYdw1bpMH26ZUIqp4VNE1IA5bZMSAV0Q_1caqb6sudSAy07MilFUMfGKamb6qenpAay95iWZtO8g9PSAvr8gI3qDGUabFXZZC6TVONU2yip6ikjSaU6QEm5KtUmepqvUk1Sh0GlevB_vSpVH3XYyapuKjO_KRvpm7BQOTExVC5uDJUr6b9FYXIf1Z4Ik-vJPiRcnk1XPVbJN2nl-Co5hi7ERsp1FLkmUo4jl8O_lT0pN1MnF9CB_Tr5Q6qt0Mk_0Ol_6uQq2n1JJx-k4JIoWU-Lk_VyKD1K08ujtHa3Xn6HMr_Ty9kU26eX15ExOFq-QG3jMfI9KkqPlUuoWR0n_0zZymo5h4bWlwoL6dNKxVpSHy0VvrSIQkhbVyoCKamuTKRQ8NMyEU7eR8tFEMUmVIh11Hq9Utyli_crxVX6yVwp7lBMmkEkUN4Ogygk1yKD0NKGHoPYTPLFM8Ke3OKrhDf9mlslHpPLt1XCk4KPnRXh1Dl8VpjoMdmazgp3WkwNLkbRRGc8jeIrGl9rFEq8UYwmG4WVss4bReHAOfEZVQ6eE9Xk8lq18CR9fLVYS37Z1WIFtRypFu10MqVGVFD8rhqxgfr3Dwoz9amHxBjt6RoSH9HfIobFKapuGBZf07Hfm8THdOKkSZTSsTMmUUKH3zWL4-RktIcLaV91QCBNXXEA6hxw60cHPKQ4V0ckU9RlR7xNy6854k1Kue6IdPpNqRMi6WHtLAxRcaAaX1BNvBrf0YYaNTZToo8zkun-Smf00I2tzmimmZ2zoabvbTVopdR4DXZSzkEN_kwfHNIgn2LyNUighzc1sJBo0kBDWgqhfCqha7c1aCbtmjkIoV_K56CfUu7PQTp1p7tgkB5Mu6CPCra54lPaXuOKTPJqcuXpirfL3fAuTVa4Qa50g6hxg4YcXN3hQaGF7tBRotUdyTTvk7lYQLaN82BHc8_Ph415PmZQzb88cZlWKl6IoIT3vXC6zgtV9GOlN9oo_Fdv6OlpgA8myX-fD4JoT74P_kSeBT5YRJZSH7wgzZQPPOirMF9coslVvpAjfNF_2xdm2l29AC1kpnGavucH5b4fuhYsxCPK_91CFNEGWYvN9BG0OE7vO2qRRSuWarGKgou1CKeUCi3SabBPiyfU5eSPR9Qyyx_tlDrbHzvJV-OPJaScD4UTTS8Ng9NrYTgRH4ZS8ioJh5Y2FaxCCm2xi8B71FgZgdtUaIjASXoxHQGVzVv4ovEtVFKuLhIFtPofkVhP2U2RyKGtrZFII4_BSPjRxzt0-Jz8s3UIon2XdThCL4Z0UA3r8N6YDhlk0kThGd0ciUIrpR3WYzflmfUopix1NA7RLznR6KdThmgYaPJGNOT_ROPi3hhcpc3nY7CNvumKQSP9OyAWP5BzVyzm0hKvOITSocJu5JFl5CFeUMO6HjRR0voebKJdEz3YT-Vf9uFLqqruQw31Gh7BRBULB3COrnQN4BrdPTqIAdo5PIh9dDdiGAPUe2UYJpoINMEuyIS8VBMKKbHdhGSyDR-BHX3_tRk36dYfRtFBbVmjuEfGsCe4QP2JFpjpVrEF3XRaP4YqeiduDKl0PWEMd-j4GitOkfjQCgcaaLFigq48t-Ia1UZNoJ682iegpQdLJzFCSbWT2EQHt75ELu269BL7KXr6JeJpyGUaFlrUPI1llK-zUYros0wbpYKkjSplJg06y8oTcosTijf9PRRKOa35AEoSRf8MJZ5kCxR7WlqiKG_QopQZSgg5Dtspc6jV7xVle5u9kknyS3vFnjrbHJReUtvPuNtnacaro513PpHcZX3K9gyf-al_TNubkbUnYOfW1D17szK2LA9cviLwjeUhAYGvb8kM_B88TzyN&build-label=editors.maestro_20260428.03_p0&is-cached-offline=false"
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  document-policy: include-js-call-stacks-in-crash-reports
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-frame-options: SAMEORIGIN
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-security-policy: frame-ancestors 'self'
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-xss-protection: 1; mode=block
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  server: GSE
2026-05-08 03:18:10.850 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  alt-svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
2026-05-08 03:18:10.885 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"reply":"Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini?","ui_metadata":{"sentiment_score":5,"suggested_action":null,"is_risky":true},"clinical_insight":{"detected_symptoms":[],"dass_category":"None","risk_level":"medium"},"session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini?"}
2026-05-08 03:18:10.885 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- END HTTP (349-byte body)
2026-05-08 03:18:15.367 28756-28784 Finsky:background       pid-28756                            I  [118] playLoggingServerUrl: https://play.googleapis.com/play/log
2026-05-08 03:18:15.367 28756-28784 Finsky:background       pid-28756                            I  [118] playLoggingServerTimestampUrl: https://play.googleapis.com/play/log/timestamp
2026-05-08 03:18:38.560 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> POST https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec
2026-05-08 03:18:38.561 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Type: application/json; charset=UTF-8
2026-05-08 03:18:38.561 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Length: 796
2026-05-08 03:18:38.561 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"mood_signal":"neutral","profile_context":"Nama: Muhammad Admiral Ganteng; Email: admelar15@gmail.com; Provider: password; Bergabung: Fri May 08 00:54:57 GMT+07:00 2026; Konteks personal dari diary: Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali?","risk_level":"high","room_id":"ZkeeUTroBXIbAB8Vk1Pf","screening_context":"Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: 2026-05-08: depresi 42/Ekstrem, kecemasan 42/Ekstrem, stres 42/Ekstrem","session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini?","text":"saya sedang stress, saya skripsian belum kelar kelar udah semester 10","user_name":"Muhammad Admiral Ganteng"}
2026-05-08 03:18:38.561 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> END POST (796-byte body)
2026-05-08 03:18:40.277 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- 200 https://script.googleusercontent.com/macros/echo?user_content_key=AUkAhnQulCSLqAxwj_EDzs-mc3x7scUsojt9NNVpVc7iMnwkX3vS539D7OEcDy39_gj5E43MQaYkkngoAAgdk1CuVSij9XFsJc-q1ptQlxo774Xhay4mm2wW8KP4taVsjoTBOBjqiwvPtA9WC-vLqW3zKD-drLlT3PlDmqb9RT26abKFB6fh9ZXG6sK5AFS90d633UUWfw8iQKjZV0ptGldaJZXlkls6NP1SnDoorMrxubLqIVjlDzqgKOibKw7KqkGHtQtgH7RhvTAlDo27HIFW1Eo1BN87hQ&lib=M_tyUyeBg--KwzF4MhwSOFlzI81VPL2HR (1716ms)
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-type: application/json; charset=utf-8
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  vary: Sec-Fetch-Dest, Sec-Fetch-Mode, Sec-Fetch-Site
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-content-type-options: nosniff
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  access-control-allow-origin: *
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  cache-control: no-cache, no-store, max-age=0, must-revalidate
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  pragma: no-cache
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  expires: Mon, 01 Jan 1990 00:00:00 GMT
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  date: Thu, 07 May 2026 20:17:59 GMT
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  reporting-endpoints: default="/macros/web-reports?bl=editors.maestro_20260428.03_p0&context=eJwVzGtUlHUCB2B45_3_XlEuCY5cFLmJDmoIFsoGOLHDcNHjIFK2ZKvHw2ISKog3dLUAk7OwxlYUnsyVqziJsUqpyFaeCI8rnQ4guiFeIq5yGUYdhUER9rcfnq_P9OyZ0YuGJAONdAxJk7S467H0ComlFsmBvl9lka7SL8FPpVukzX0qxdC9tDGpl1xqxiQ_ehRulcapJcIqtdM_tlilYzTj71bJhbYNW6WD9MmmcamMFjSNS4GU0zIu5dM152dSM9WXPpMaaOmhCUlLHW9PSl1UPzUpNZC955Q0i-bun5Lm0-f7bFSnKKPSVpVF5lJJNUa1jZKqntITVap9lJijUm2gJ3kq1QR1VKpU9_-vV6Xqow47WdVFpebXZCN9Fx4mJySEyUWNYXIF_bcwXO6j2mPhcj3Zh0bIs-iy50r5Oq0YWynH0LnYSLmOIldHynHkevCPshflZOjkfNq3Vyd_QLXlOvknOvkvnVxFOy_o5P0UUhwl62lRkl4Oowepevkhrdmpl9-kjB_0chbF9unltWQMiZbPUdtYjHyHCtNi5WJqdo6Tf6UsZZWcTUPrSoSF9KklYg05Hy4RfrSQQklTVyKCKLGuVCRTyJNSEUE-h8tEMMXGl4u11Hq1Qtym83crxGX6xVwhblFMaqWIp9xtlaKA3AorhYbW91SKjSSfPyXsyd1QJXzo95wq8Yhcv68SXhRy5LSIoHvDp4WJHpGt6bTwoEXU4GoUTXTKyyi-prE1RqEYjOJhklFYKfOsURQMnBGf0heDZ0QZub5cLbxIb6gWa8g_q1osp5ZD1aKdjifXiHIy7KgR66l_76AwU5_zkBilXZ1D4kP6TDssTlB1w7D4ho78ySQ-omPHTaKEjpwyiWI6-I5ZHCUnoz1cSfOSA4Jo8pIDUOeAGz87oJvi3ByRRFEXHfEGLbviiNco-aoj0ugPJU6IpO7amRiioiBnfEk1Bmf8QOtrnLGREnxdkETxK1zwFt2lHrq22QXNNP3eLDjTj7ZqtFKKQY3tlL1fjb_R-wfUyKOYPDXiqfu6GhYSTWqoSUOhlEfFdOWmGs2kWT0bofRb2Wz0U_Ld2UijrjRXDNL9KVf0Uf4WN3xCW2vckEHeTW483fBGmTveoYlyd8gV7hA17lCTg5sHPCmswAM6SrB6IInmfjwH88m2cS7saM7ZebAxz8M0qvnWCxdpheINLcW_542Tdd6oop8rfNBGEb_7QE9PAn0xQQF7fBFMu_J88VfyyvfFQrKU-OI5qSd94Ulfh_vhAk2s9IOs9UP_TT-YaWf1fLSQmcZo6o4_lLv-6Jy_AA8o760FKKT1sgYb6UNocJTec9Qgk5Yv0WAlhRRpEEHJ5Rqk0WCfBo-p0ykAD6hlZgDaKWVWALaTnzoAi0k5GwYnmloSDqeXw3HMEI4S8i6OgIY25K9EMm2y0-JdaqzQ4iYVVGpxnJ5PaaGyeR1fNr6OCsrRRSKfVv0zEusoqykS2bS5NRKp5DkYCX_6aJsOn1NAlg7BtOeiDofo-ZAOqmEd3h3VIZ1M6ig8pesjUWil1IN67KRcsx5FlOkcjQP0W3Y0-ulEZTQqaeJaNOT_ROP87hhcpo1nY7CFvuuMQSP9OzAWP5FLZyzm0GLvOITRgYIu5NKLkW4Iczca1vagiRLX9WAD7RjvwV4q-6oPX1FVdR9qqLfyAUxUvmAAZ-hS5wCu0O3Dgxig7cOD2EO3tcMYoN5LwzDReJAJdsEm5KaYUEAJ7SYkkW3ECOzox2_MuE43_vwQHdSW-RB3yBj-GOeoP8ECM90osqCLTupHUUVvxo0iha7Gj-IWHV1txQkSH1jhQAMtVozTpWdWXKHaqHHUk3f7ODR0f8kERiixdgIbaP_mF8ihHRdeYC9FT72AgYZcp2Chhc1TWEp5OhulkD7NsFHKSXpbpUynQRdZeUzucULxoS_CoJTR6vehJFL0r1AMJFug2NOSYkV5lRYmT1NCyXHYTplNrf4zlK1t9koG3WtzUHppQMxUHpHzjGljfZZmvGT8NlvykPXJW9N956X8JXV3euauwO2bU3btzkzftCxo2fKgV5eFBga9sikj6H9Mpjel&build-label=editors.maestro_20260428.03_p0&is-cached-offline=false"
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  document-policy: include-js-call-stacks-in-crash-reports
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-frame-options: SAMEORIGIN
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-security-policy: frame-ancestors 'self'
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-xss-protection: 1; mode=block
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  server: GSE
2026-05-08 03:18:40.278 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  alt-svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
2026-05-08 03:18:40.309 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"reply":"Aku sangat mengkhawatirkanmu. Sepertinya kamu sedang dalam masa yang sangat sulit. Sereluna di sini untuk menemani, tapi aku sangat menyarankanmu untuk berbicara dengan tenaga profesional. Apakah kamu ingin aku hubungkan ke bantuan psikolog?","ui_metadata":{"sentiment_score":1,"suggested_action":"Hubungi psikolog","is_risky":true},"clinical_insight":{"risk_level":"high"},"session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini?"}
2026-05-08 03:18:40.309 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- END HTTP (490-byte body)
2026-05-08 03:18:52.980 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> POST https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec
2026-05-08 03:18:52.980 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Type: application/json; charset=UTF-8
2026-05-08 03:18:52.980 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Length: 771
2026-05-08 03:18:52.980 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"mood_signal":"neutral","profile_context":"Nama: Muhammad Admiral Ganteng; Email: admelar15@gmail.com; Provider: password; Bergabung: Fri May 08 00:54:57 GMT+07:00 2026; Konteks personal dari diary: Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali?","risk_level":"low","room_id":"ZkeeUTroBXIbAB8Vk1Pf","screening_context":"Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: 2026-05-08: depresi 42/Ekstrem, kecemasan 42/Ekstrem, stres 42/Ekstrem","session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini?","text":"wah apakah bisa saya dihubungkan ke psikolog?","user_name":"Muhammad Admiral Ganteng"}
2026-05-08 03:18:52.980 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> END POST (771-byte body)
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- 200 https://script.googleusercontent.com/macros/echo?user_content_key=AUkAhnRDUIL0-xdQj5hI4EEzbInZlIGrAkbkxVWgCykucPMHixocdrg4LvOirF0i-NLFWpUfBqFdDJ29wqZUUO9U-2tPj-CUmiM8hVHeRk741kgKzyzMqxtruD3AYsO3AAIWgxKjhugLuRQWI6UeSugI-65AAZSNwffH6mGcnX5PEAPfaYj9AoyichiGYxAeef2zDx0F2IMUPu3kFROypMkOVhHRXKCllXszii2cbAEZuJ_GKnbme6Uv1bfBlmyOiNlpXYGY0Yzp2K5y76ELZ3J153N9h3IbwA&lib=M_tyUyeBg--KwzF4MhwSOFlzI81VPL2HR (3135ms)
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-type: application/json; charset=utf-8
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  vary: Sec-Fetch-Dest, Sec-Fetch-Mode, Sec-Fetch-Site
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-content-type-options: nosniff
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  access-control-allow-origin: *
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  cache-control: no-cache, no-store, max-age=0, must-revalidate
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  pragma: no-cache
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  expires: Mon, 01 Jan 1990 00:00:00 GMT
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  date: Thu, 07 May 2026 20:18:14 GMT
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  reporting-endpoints: default="/macros/web-reports?bl=editors.maestro_20260428.03_p0&context=eJwVzGtU1GUCBnD4z_s-fzFAHRy5KHLzMqgpWCgbN9lhuMgRRMqWbPV4WExCBfGGrpZgsgtrlFF4NFfkIpIQq7QqspUnw2PS6QCiG-IlQgG5DKOOwqAo--yH39ff-E8nRc7pV-JosL1feUlzOx8rr5FcYFEc6LulFuUy_eL_VLlBYTlPlSi6kzasdJFTzbDiQ4-CrcoINYdYlTb6ZJ1VOUSv_MOqONGGAauymw6uGVFKaFbjiDKfsptHlDy6on2mNFH98WfKJVqwZ1QJo_Z3XiqdVD_2UrlE9u5jymSatnNMmUFf7LDRnKCMcltNFpmPK5phqm1QNPWUnqjR7KDEbI1mFT3J1WhGqb1co7n7f10aTTe12wlNJx03vyEq6dvgIJGQECQKG4JEGf23IFh0U-2hYFFPB6uDxWGyDwwRk2nZkhARGhEiLriHiqu0eDhURNHp6HBRR-Gx4SKGnHf_UXhQdoZB5NGO7QbxIdWWGsSPdOxfBlFBm88axE4KKIoQRpqTZBRB9CDVKB7Sss1G8RZlfG8UWRTdbRTLqTIgUpym1uEocYsK0qJFETVpY8SvlKUuFXupf0WxtJB2X7H0odkUSPq6YulHiXXHZTIFPDkuQ8hrX4n0p-j4UrmcWi6XyZt05naZvEC_mMvkDYpKLZfxlLOhXOaTS0G51NPK--VyNYkzJ6Q9ucZVSC_6PbtCPiLn7yqkBwXsPylD6M7ASWmiR2RrOindaA5dcq6UjXTCo1J-TcPLKqUaVykfJlVKK2VWV8r83lPyMzrcd0qWkPOrVdKD4mOr5NtkjKuSy2hmVpVcRM17qmQbHUmukaUUt6lGrqSe7X3STN3afjlEWzr65Uf0ediAPEpVlwbkN7T_Tyb5MR06YpLFtP-ESRbR7nfN8gBNqLSHM-knOsCPXp53AOoccO1nB9yjGBdHJFHEOUe8SQsvOuINSr7siDT6Q_EEhNO92knop0I_Lb6kmjgtvqeVNVqspgRvJyTR7cVOuE9X1jqhicbfmQwt_WCrQwulxOmwkfbu1OHv9MEuHXIpKleHeLp3VQcLyUYddKSnQMqlIrp4XYcm0sdOQSD9VjIFPZR8ewrSqDPNGX10d8wZ3ZS3zgUHaX2NCzLIs9GFpwveLHHFuzRa6gpR5gpZ4wodObi4wZ2C8t1goASrG5Jo2qdTMYNsG6bBjqZWT4eNeTrGUc2_PXCOFqueCKP49z1xrM4TFfRzmRdaKeR3LxjpyXxvjJLvNm_405Zcb_yVPPK8MZssxd54TrqX3nCnr4N9cJZGQ30gwnzQc90HZtpcNQPNZKZhGrs1E-rtmeiYMQsPKPftWSiglUKP1fQR9DhA7zvqkUmL5ukRSgGFeoRQcqkeadTXrcdj6pjgiwfUPMkXbZQy2RcbyUfni7mkVgdhAtm8GoyJdCguGMXkWRQCPa3KC0UyrbELw3vUUBaG65RfHoYj9HwsDBqbJfiyYQnKKNsQjjxa-s9wrKCsxnDspbUt4Ugl975wzKSPNxjwBflmGeBP284ZsIee9xugGTDgvSED0smki8BTujoYgRZK3W3EZsoxG1FImdpI7KLf9kaih46WR6KcRq9EQvwUiTNbo3CBVldHYR192xGFBvrP_Gj8SE4d0ZhKcz1jEES78juRQ5bBe3hOl5bfRyMlrriPVbRp5D62U8lX3fiKKqq6UUNd5Q9gotJZvThF5zt6cZFu7utDL20c6MM2WqTtRyjdDBtAL3WdH4CJRvxMsPM3ISfFhHxKaDMhiWxDBmFHP3xjxlW69ueHaKfWzIe4RZXBj3GaehIsMNO1Qgs66ZhxCBX0VswQUuhy_BBu0IFYK46S_NAKB-pttmKEzj-z4iLVRoygnjzbRqCnu_NGMUiJtaNYRTvXvkA2bTr7AtspcuwF4qjfeQwWmt00hgWUa7BRC-izDBu1lJR3NOp46nMS6mNyjZGqFx0OglpCsR9ATaTIX6HGkbBAtad5Rar6Os1OHqcGkuOAnTqFWma-oq5vtVczSLywV-3pTquD2kVa-3G5PZYmTNz709m_adyEMXl9uvf0lL-kbk3P3DJ_49qULVsz09cs9Fu4yO_1hYHz_V5bk-H3P27FPbA&build-label=editors.maestro_20260428.03_p0&is-cached-offline=false"
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  document-policy: include-js-call-stacks-in-crash-reports
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-frame-options: SAMEORIGIN
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-security-policy: frame-ancestors 'self'
2026-05-08 03:18:56.116 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-xss-protection: 1; mode=block
2026-05-08 03:18:56.117 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  server: GSE
2026-05-08 03:18:56.117 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  alt-svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
2026-05-08 03:18:56.125 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"reply":"Hai, aku bisa membantu kamu menemukan sumber dukungan. Apakah kamu memiliki psikolog yang biasa kamu kontak? Atau aku bisa membantu mencari informasi tentang psikolog di sekitarmu.","ui_metadata":{"sentiment_score":3,"suggested_action":null,"is_risky":true},"clinical_insight":{"detected_symptoms":["Depresi","Kecemasan"],"dass_category":"Mixed","risk_level":"medium"},"session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini? wah apakah bisa saya dihubungkan ke psikolog?"}
2026-05-08 03:18:56.125 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- END HTTP (531-byte body)
2026-05-08 03:19:03.700 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> POST https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec
2026-05-08 03:19:03.700 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Type: application/json; charset=UTF-8
2026-05-08 03:19:03.700 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Length: 784
2026-05-08 03:19:03.701 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"mood_signal":"neutral","profile_context":"Nama: Muhammad Admiral Ganteng; Email: admelar15@gmail.com; Provider: password; Bergabung: Fri May 08 00:54:57 GMT+07:00 2026; Konteks personal dari diary: Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali?","risk_level":"low","room_id":"ZkeeUTroBXIbAB8Vk1Pf","screening_context":"Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: 2026-05-08: depresi 42/Ekstrem, kecemasan 42/Ekstrem, stres 42/Ekstrem","session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini? wah apakah bisa saya dihubungkan ke psikolog?","text":"ah kontol lu","user_name":"Muhammad Admiral Ganteng"}
2026-05-08 03:19:03.704 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> END POST (784-byte body)
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- 200 https://script.googleusercontent.com/macros/echo?user_content_key=AUkAhnQMSeicVqS6FTCOipd5zNXqZDrnIU4MEmOWiiv4WnvojRG2VJEmGlctXSJx-qDafcokWfbwcryQU2B50_8IsmKabreBGg8-jPv_yte9HOenx8lozWTdS8SCIgjIFosARqSGa4XHEck5kGb6aY7O5OQNjouY9U-4d3wuYpYKwRqeGclYH90sy4RPI3dPz6dE0NOyDxeHpgHaBFaC88HYaUDabBdet1Sf3Opg5GX7s-axJsVkqZv1qKZB65o6FuGAnhhrKsZt92Xv_pXgxSaSG27B2otuVQ&lib=M_tyUyeBg--KwzF4MhwSOFlzI81VPL2HR (2521ms)
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-type: application/json; charset=utf-8
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  vary: Sec-Fetch-Dest, Sec-Fetch-Mode, Sec-Fetch-Site
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-content-type-options: nosniff
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  access-control-allow-origin: *
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  cache-control: no-cache, no-store, max-age=0, must-revalidate
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  pragma: no-cache
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  expires: Mon, 01 Jan 1990 00:00:00 GMT
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  date: Thu, 07 May 2026 20:18:24 GMT
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  reporting-endpoints: default="/macros/web-reports?bl=editors.maestro_20260428.03_p0&context=eJwVzGlQlPcBB2B49_3_XjGABlw5FLlEFzUIJigNV-iyHDouItGWkOg4FCvBA_FCq4lgZAoxNJEGp8bIjZtgtkqqIo1xQnCsZDKAaIN4hCCHHMuiq7AoQn_98Hx9po_MjFk0KOlpuGNQmqTFXU-k10kstUgOdGWlRbpGPwc9k25TZO4zKZbuZ4xJPeRsHJN86XGYVRqnlnCr1E5_22yVTtArH1slZ9o6ZJUO0mcbx6UyWtA0LgVQTsu4lE_XnZ5LzVRf-lxqoKWHJqRI6nhnUuqi-qlJqYHsPaakWTR3_5Q0nz7fZ6OqosxKW1U2mUsl1RjVNkqqetqRpFLto6QclSqFnuapVBPUUalSPfi_HpWqlzrsZFUXlZrflA30XVionJgYKhc1hsoV9N_CMLmXak-EyfVkHxIuz6LLHhHyDVoxFiHH0rm4KLmOolZFyfHkcvD3siflZGrlfNq3Vyt_SLXlWvlHOv1PrVxNOy9o5f0UXBwt62hRsk4OpUfpOnmEVu_Uyeso83udnE1xvTp5DRmCY-Rz1DYWK9-lwow4uZianeLlXyhbWSkfpsG1JcJCTkdKhC8tpBDS1JWIQEqqKxWpFPy0VIST95EyEURxCeViDbVeqxB36Py9CnGZfjZXiNsUm14pEih3a6UoINfCSqGh9d2VYgPJ56uEPbnpq4U3_ZZTLR6Ty5Vq4UnBR8-IcLo_dEaY6DHZms4Id1pEDS4G0URVngbxDY2tNghFbxAjyQZhpayzBlHQ_7U4Ti6v1QhP0ulrxGryy64Ry6nlUI1op5OpRlFO-u1GsZ769g4IM_U6DYpR2tU5KD6iv0cOiVNU0zAkvqWjfzSJT-jESZMooaNVJlFMB981i2M0w2APF9LMdEAgTV5yAOoccPMnBzykeFdHJFP0RUe8TcuuOuJNSr3miAz6XckMRNHD2lcxSEWBTviCjHonfE_rjU7YQIk-zkimeyuc0U3XNzmjmabfnwUn-sFWjVZK06uxjQ7vV-Ov9MEBNfIoNk-NBHp4Qw0LiSY11KShEMqjYrp6S41m0qyajRD6tWw2-ij13mxkUFeGCwbowZQLeil_sys-oy1GV2SSV5MrT1e8XeaGd2mi3A1yhRuE0Q1qcnB1hweFFrhDS4lWdyTT3E_nYD7ZNs6FHc05Ow825nmYRsZ_eeIirVC8EEkJ73vhdJ0XqumnCm-0Ufhv3tDR0wAfTJD_Hh8E0a48H_yFPPN9sJAsJT54QepJH3jQN2G-uEATEb6QI33Rd8sXZtpZMx8tZKYxmrrrB-WeHzrnL8AjyvvDAhTSelmDDfQRNDhG7ztqkEXLl2gQQcFFGoRTarkGGTTQq8ET6pzhj0fU8qo_2iltlj-2ka_aH4tJORuKGWTzWhhm0gl9GErIqzgcGkrJj0AqrbOLxHvUWBGJW1RQGYmT9GIqEiqbt_BF41uooBxtFPJp5ZdRWEvZTVE4TJtao5BOHgNR8KNPtmrxOflnaxFEey5qcYheDGqhGtLiz6Na7CCTOhrP6MZwNFop_aAOOynXrEMRZTnF4AD9ejgGfXSqMgaVNHE9BvJ_YnB-dywu04azsdhM33XGopH-HRCHH8m5Mw5z6Eu7eFTRYq94hNKBgi7kkmX4IV5Qw5puNFHS2m6k0Pbxbuylsq968RVV1_TCSD2Vj2Ci8gX9-JoudfbjKt05MoB-2jY0gD10J3II_dRzaQgmGg80wS7IhNw0Ewoosd2EZLINH4Yd_fCtGTfo5nsj6KC2rBHcJUPYE5yjvkQLzHSzyIIuOq0bRTWtix9FGl1LGMVtOrbKilMkPrTCgfpbrBinS8-tuEq10eOoJ6_2cWgoxfgcqfRgyQSGKal2Aim0f9NL5ND2Cy-xl2KmXkJPgy5TsNDC5ikspTytjVJIxzNtlHKS3lEp02nAWVaekFu8ULzpH6FQymjVB1CSKOYXKHqSLVDsaUmxorxBC1OnKSHkOGSnzKZWv1eULW32Sibdb3NQesjJftqnfZZmzDR8fPyKyl3WpW7Z4TMv7U_pu3dk7QrYtilt1-6sHRuXBS5bHvjGspCAwNc3Zgb-D_RxM8o&build-label=editors.maestro_20260428.03_p0&is-cached-offline=false"
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  document-policy: include-js-call-stacks-in-crash-reports
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-frame-options: SAMEORIGIN
2026-05-08 03:19:06.226 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-security-policy: frame-ancestors 'self'
2026-05-08 03:19:06.227 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-xss-protection: 1; mode=block
2026-05-08 03:19:06.227 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  server: GSE
2026-05-08 03:19:06.227 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  alt-svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
2026-05-08 03:19:06.248 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"reply":"Aku melihat kamu sedang sangat frustrasi. Apakah kamu ingin berbicara tentang apa yang terjadi?","ui_metadata":{"sentiment_score":1,"suggested_action":"Coba ambil napas dalam-dalam dan berbicara tentang perasaanmu","is_risky":true},"clinical_insight":{"detected_symptoms":["Agresif"],"dass_category":"Stress","risk_level":"medium"},"session_summary":"wah apakah bisa saya dihubungkan ke psikolog? Hai, aku bisa membantu kamu menemukan sumber dukungan. Apakah kamu memiliki psikolog yang biasa kamu kontak? Atau aku bisa membantu mencari informasi tentang psikolog di sekitarmu. Aku melihat kamu sedang sangat frustrasi. Apakah kamu ingin berbicara tentang apa yang terjadi?"}
2026-05-08 03:19:06.248 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- END HTTP (685-byte body)
2026-05-08 03:19:10.254 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> POST https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec
2026-05-08 03:19:10.255 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Type: application/json; charset=UTF-8
2026-05-08 03:19:10.255 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  Content-Length: 979
2026-05-08 03:19:10.256 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"mood_signal":"neutral","profile_context":"Nama: Muhammad Admiral Ganteng; Email: admelar15@gmail.com; Provider: password; Bergabung: Fri May 08 00:54:57 GMT+07:00 2026; Konteks personal dari diary: Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali? Maaf, saya kurang mengerti. Bisa diceritakan kembali?","risk_level":"low","room_id":"ZkeeUTroBXIbAB8Vk1Pf","screening_context":"Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: 2026-05-08: depresi 42/Ekstrem, kecemasan 42/Ekstrem, stres 42/Ekstrem","session_summary":"wah apakah bisa saya dihubungkan ke psikolog? Hai, aku bisa membantu kamu menemukan sumber dukungan. Apakah kamu memiliki psikolog yang biasa kamu kontak? Atau aku bisa membantu mencari informasi tentang psikolog di sekitarmu. Aku melihat kamu sedang sangat frustrasi. Apakah kamu ingin berbicara tentang apa yang terjadi?","text":"ngentot lu memek","user_name":"Muhammad Admiral Ganteng"}
2026-05-08 03:19:10.256 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  --> END POST (979-byte body)
2026-05-08 03:19:15.483 28756-28856 PlayCommon              pid-28756                            I  [156] Connecting to server for timestamp: https://play.googleapis.com/play/log/timestamp
2026-05-08 03:19:15.609 28756-28856 PlayCommon              pid-28756                            I  [156] Connecting to server: https://play.googleapis.com/play/log?format=raw&proto_v2=true
2026-05-08 03:19:17.443 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- 200 https://script.googleusercontent.com/macros/echo?user_content_key=AUkAhnQF9bFeF1jU2YNo70Nu0r4AI27o8Ss7kpuyynIJ2MTc_DTCVeLZtKnl0wDbB0t-27WjF1PR_oxEcqcvhMplNvWba4LSse3EatxPXDLKve0jLc_hxVXtCFgoKJKr1ZtrCVWNaAsuUfXZuHE-1gNnW_nrVTHpZf_7LqhDEvtoPU8RpGm2KwG9Gajb6rZNAxsA6zoUhxt37iYiHUDi5Z81UDJMW_C2JN7UqRe4H6jtD8mpGwXLVrcRw6ZhxCnuVmcx-D2OhbQkrvh8ZrjL2OdSxCJg1rh1Vw&lib=M_tyUyeBg--KwzF4MhwSOFlzI81VPL2HR (7185ms)
2026-05-08 03:19:17.444 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-type: application/json; charset=utf-8
2026-05-08 03:19:17.444 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  vary: Sec-Fetch-Dest, Sec-Fetch-Mode, Sec-Fetch-Site
2026-05-08 03:19:17.444 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-content-type-options: nosniff
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  access-control-allow-origin: *
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  cache-control: no-cache, no-store, max-age=0, must-revalidate
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  pragma: no-cache
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  expires: Mon, 01 Jan 1990 00:00:00 GMT
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  date: Thu, 07 May 2026 20:18:36 GMT
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  reporting-endpoints: default="/macros/web-reports?bl=editors.maestro_20260428.03_p0&context=eJwVzGtUlHUCB2B45_3_XlFAHRy5KHITHdQQLJQNkNhhuOhxECl3yVaPh8UkVBBN0bUETArW2DU2PJrJbUASYpVWRbb0RHhc6XRA0Y3wEiEXuQyjjsKgCPvbD8_XZ2rWzKhFg5KBhjsGpQla3PVEepXEUovkQN-tskhX6afAZ9JtCs95JkXTvdRRqYecakclH3ocapXGqDXMKrXT37ZYpWM07a9WyYm2DVmlA3R005hUSguaxyR_ym4dk_Lomvq51EINJc-lRlr6wbgUTh1vT0hd1DA5ITWSvfukNIvm7puU5tPne21UFZRutFVlkrlEUo1SXZOkaqC0BJVqLyVkq1Qb6GmuSjVOHUaV6v7_9ahUvdRhJ6u6qMT8ulxF34aGyPHxIXJhU4hcTv8tCJV7qe5YqNxAR2tC5eNkHxwmz6JL7ivl67RidKUcTWdjIuR6ilgdIceS84Hfyx6Una6T82jvHp18kOrKdPIPdOqfOrmSdp7XyfsoqChS1tOiRL0cQg9T9PIjWrNTL79F6Zf1cibF9OrltVQVFCWfpbbRaPkOFaTGyEXUoo6Vf6ZMZZWcRYPrioWF1IeKhQ8tpGDS1heLAEqoLxFJFPS0RISR16FSEUgxcWViLV27Wi5a6NzdcnGJfjKXi9sUnWIUcZSzzSjyyaXAKLS0vtsoNpJ8rkLYk6uhUnjRb9mV4jE5f1cpPCjo8GkRRveGTgsTPSZb02nhRouo0blKNFOFR5X4mkbXVAnFUCUeJVYJK2XUVIn8_jPiMzo-cEaUkvMr1cKD9IZqsYZ8M6vFcmr9oFq004mkWlFGhh21Yj317RkQZupVD4oR2tU5KD6if4QPiZNU3TgkvqHDfzSJT-nYCZMopsMVJlFEB94xiyM0vcoezqSd4YAAmrjoANQ74OaPDnhAsS6OSKTIC454k5ZdccTrlHTVEan0u-LpiKAHdTMxSIUBanxBtQY1LtP6WjU2Ury3ExLp7gondNO1zU5ooan3ZkFN39tqcIOSDRpsp6x9GnxCH-7XIJeiczWIowfXNbCQaNZAQ1oKplwqoiu3NGgh7erZCKZfS2ejj5LuzkYqdaU6Y4DuTzqjl_K2uOAoba11QTp5NrvwdMGbpa54h8bLXCGXu0LUukJDDi5ucKeQfDfoKN7qhkSa-_c5mE-2TXNhR3Nq5sHGPA9TqPZfHrhAKxRPhFPce544Ve-JSvqx3AttFPabF_T01N8b4-T3vjcCaVeuN_5CHnneWEiWYm-8IM2EN9zp61AfnKfxlT6Qw33Qd8sHZtpZPR-tZKZRmrzjC-WuLzrnL8BDyv3DAhTQelmLjfQRtDhC7zlqkUHLl2ixkoIKtQijpDItUmmgV4sn1DndDw-pdaYf2il5lh-2k4_GD4tJqQnBdLJ5JRQz6JghFMXkWRQGLW3IW4kk2mQXjnepqTwctyjfGI4T9GIyHCqbN_BF0xsop2xdBPJo1ZcRWEeZzRHIos03IpBC7gMR8KVPt-nwOfll6hBIBy_o8DG9GNRBNaTDuyM6pJFJE4lndH04Ejco5YAeOynHrEchZaijsJ9-zYpCH500RsFI49eiIP8nCud2R-MSbayJxhb6tjMaTfRv_xj8QE6dMZhDX9rFooIWe8YihPbndyGHLMMP8IIa13ajmRLWdWMD7Rjrxh4q_aoXX1FldS9qqcf4ECYqW9CPM3Sxsx9X6JdDA-in7UMDeJ9-CR9CP_VcHIKJxgJMsAs0ISfZhHyKbzchkWzDhmFH339jxnW6-adH6KC2jEe4Q1WhT3CW-uItMNPNQgu66JR-BJX0VuwIkulq3Ahu05HVVpwkcdAKB-pvtWKMLj634grVRY6hgTzbx6Cl-NrnSKT7S8YxTAl149hA-za_RDbtOP8Seyhq8iUMNOg8CQstbJnEUsrV2SgF9Fm6jVJG0tsqZSoNOMnKE3KNFYoXHQ-BUkqrP4SSQFE_QzGQbIFiT0uKFOU1Wpg0RQkmxyE7ZTbd8J2mbG2zV9LpXpuD0kPqaVNO9llaMOOTwct6N1mftDXNe17yn1N2p2Xs8t--OXnX7oy0TcsCli0PeG1ZsH_Aq5vSA_4Hetg6xQ&build-label=editors.maestro_20260428.03_p0&is-cached-offline=false"
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  document-policy: include-js-call-stacks-in-crash-reports
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-frame-options: SAMEORIGIN
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  content-security-policy: frame-ancestors 'self'
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  x-xss-protection: 1; mode=block
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  server: GSE
2026-05-08 03:19:17.445 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  alt-svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000
2026-05-08 03:19:17.469 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  {"reply":"Aku mendengar kamu sedang sangat frustasi. Apakah kamu ingin berbicara tentang apa yang terjadi?","ui_metadata":{"sentiment_score":1,"suggested_action":null,"is_risky":true},"clinical_insight":{"detected_symptoms":["Stress"],"dass_category":"Stress","risk_level":"medium"},"session_summary":"halo selamat malam Halo, Teman! Selamat malam untukmu. Bagaimana perasaanmu hari ini? wah apakah bisa saya dihubungkan ke psikolog?"}
2026-05-08 03:19:17.469 28020-28731 okhttp.OkHttpClient     com.android.capstone.sereluna        I  <-- END HTTP (435-byte body)
---------------------------- PROCESS ENDED (28020) for package com.android.capstone.sereluna ----------------------------
2026-05-08 03:21:48.047 28956-28991 HttpFlagsLoader         pid-28956                            I  Unable to resolve the HTTP flags file provider package. This is expected if the host system is not set up to provide HTTP flags.
2026-05-08 03:21:48.087 28956-28984 SpeechPackManager       pid-28956                            I  SpeechPackManager.registerManifest():509 registerManifest() : https://dl.google.com/android/voice/gboard/terse/superpacks-manifest-20210519.json
2026-05-08 03:21:48.169 28956-28994 SP                      pid-28956                            I  Registering emoticon_content_description.2022081613, url: https://www.gstatic.com/android/keyboard/emoticon_content_desc/202208161305/superpacks_manifest.json, constraints: W:*:*:*, flags: bg, requested: 2022081613, current: 2022081613
2026-05-08 03:21:48.177 28956-28994 SP                      pid-28956                            I  Registering emoticon_content_description.2022081613, url: https://www.gstatic.com/android/keyboard/emoticon_content_desc/202208161305/superpacks_manifest.json, constraints: W:*:*:*, flags: bg, requested: 2022081613, current: 2022081613
2026-05-08 03:21:48.312 28956-28956 HandwritingSuperpacks   pid-28956                            I  HandwritingSuperpacks.register():265 register(): version '86', url 'https://dl.google.com/handwriting/models/handwriting_release.superpack_manifest.20231127.json' [ONLINE]
2026-05-08 03:21:48.323 28956-28994 SP                      pid-28956                            I  Registering emoticon_content_description.2022081613, url: https://www.gstatic.com/android/keyboard/emoticon_content_desc/202208161305/superpacks_manifest.json, constraints: W:*:*:*, flags: bg, requested: 2022081613, current: 2022081613
2026-05-08 03:21:48.323 28956-28994 SP                      pid-28956                            I  Registering emoticon_content_description.2022081613, url: https://www.gstatic.com/android/keyboard/emoticon_content_desc/202208161305/superpacks_manifest.json, constraints: W:*:*:*, flags: bg, requested: 2022081613, current: 2022081613
2026-05-08 03:21:48.324 28956-28994 SP                      pid-28956                            I  Registering handwriting_recognition.86, url: https://dl.google.com/handwriting/models/handwriting_release.superpack_manifest.20231127.json, constraints: m:*:*:*, flags: fg, requested: 86, current: 86
2026-05-08 03:21:48.362 28956-28985 Manifested...Downloader pid-28956                            I  ManifestedDataDownloader.download():67 downloading manifest https://www.gstatic.com/android/keyboard/dictionarypack/2025121000/metadata.json-perlang/en-US.json?v=2026012600
2026-05-08 03:21:48.405 28956-28994 SP                      pid-28956                            I  Registering emoticon_content_description.2022081613, url: https://www.gstatic.com/android/keyboard/emoticon_content_desc/202208161305/superpacks_manifest.json, constraints: W:*:*:*, flags: bg, requested: 2022081613, current: 2022081613


