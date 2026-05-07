const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Inisialisasi Firebase Admin SDK
admin.initializeApp();

// Ambil Gemini API Key dari environment variables
// PENTING: Ganti nilai di bawah dengan API Key Anda atau konfigurasikan di Firebase
const GEMINI_API_KEY = "AIzaSyCw7pHCl__CDR9ZHSo-nUOWno0wKpZg1lM";

// Inisialisasi Gemini Generative AI SDK
const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

exports.chatBot = functions.https.onCall(async (data, context) => {
  // Memastikan pengguna terautentikasi jika diperlukan (opsional, tapi best practice)
  // if (!context.auth) {
  //   throw new functions.https.HttpsError(
  //     "unauthenticated",
  //     "The function must be called while authenticated."
  //   );
  // }

  const userPrompt = data.text;
  if (!userPrompt) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      'The function must be called with one argument "text" containing the message to send.'
    );
  }

  try {
    // Dapatkan model generatif Gemini
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    // Prompt Engineering: Memberi instruksi dan konteks pada AI
    const chatPrompt = `
      Anda adalah Sereluna, seorang asisten virtual AI untuk aplikasi kesehatan mental dan jurnal pribadi bernama Sereluna.
      Peran Anda adalah menjadi pendengar yang empatik, suportif, dan non-judgmental.
      Gunakan gaya bahasa yang tenang, positif, dan menenangkan.
      JANGAN PERNAH memberikan diagnosa medis, resep obat, atau anjuran profesional yang bersifat klinis.
      Fokus pada validasi perasaan pengguna dan berikan dukungan emosional yang umum.

      Berikut adalah pesan dari pengguna:
      "${userPrompt}"
    `;

    // Hasilkan konten berdasarkan prompt
    const result = await model.generateContent(chatPrompt);
    const response = await result.response;
    const botReply = response.text();

    return { reply: botReply };

  } catch (error) {
    console.error("Error calling Gemini API:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Failed to communicate with the chatbot AI."
    );
  }
});
