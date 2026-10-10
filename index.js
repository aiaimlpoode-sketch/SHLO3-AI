import express from "express";
import cors from "cors";
import dotenv from "dotenv";

dotenv.config();
const app = express();
app.use(cors());
app.use(express.json({ limit: "2mb" }));

const API_KEY = process.env.OPENAI_API_KEY;
const CHAT_MODEL = process.env.CHAT_MODEL || "gpt-4o-mini";
const IMAGE_MODEL = process.env.IMAGE_MODEL || "gpt-image-1";

app.get("/", (_req, res) => res.json({ name: "SHLO3 AI API", status: "running" }));

app.post("/chat", async (req, res) => {
  try {
    if (!API_KEY) return res.status(500).json({ error: "OPENAI_API_KEY is not configured" });
    const messages = Array.isArray(req.body.messages) ? req.body.messages : [];
    const safeMessages = messages.slice(-20).map(m => ({
      role: m.role === "assistant" ? "assistant" : "user",
      content: String(m.content || "").slice(0, 12000)
    }));
    const response = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: { "Authorization": `Bearer ${API_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: CHAT_MODEL,
        messages: [
          { role: "system", content: "أنت SHLO3 AI، مساعد مفيد يجيب بالعربية واللهجة العراقية عندما يناسب ذلك، ويساعد في البرمجة بوضوح. لا تدّعِ تنفيذ شيء لم تنفذه." },
          ...safeMessages
        ]
      })
    });
    const data = await response.json();
    if (!response.ok) return res.status(response.status).json({ error: data.error?.message || "AI provider error" });
    return res.json({ answer: data.choices?.[0]?.message?.content || "ما حصلت جواب." });
  } catch (e) {
    return res.status(500).json({ error: e.message || "Server error" });
  }
});

app.post("/image", async (req, res) => {
  try {
    if (!API_KEY) return res.status(500).json({ error: "OPENAI_API_KEY is not configured" });
    const prompt = String(req.body.prompt || "").trim().slice(0, 4000);
    if (!prompt) return res.status(400).json({ error: "prompt is required" });
    const response = await fetch("https://api.openai.com/v1/images/generations", {
      method: "POST",
      headers: { "Authorization": `Bearer ${API_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify({ model: IMAGE_MODEL, prompt, size: "1024x1024" })
    });
    const data = await response.json();
    if (!response.ok) return res.status(response.status).json({ error: data.error?.message || "Image provider error" });
    const item = data.data?.[0];
    // Some API responses return a URL; others return base64 image data.
    if (item?.url) return res.json({ imageUrl: item.url });
    if (item?.b64_json) return res.json({ imageUrl: "data:image/png;base64," + item.b64_json });
    return res.status(502).json({ error: "No image URL or image data returned" });
  } catch (e) {
    return res.status(500).json({ error: e.message || "Server error" });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, "0.0.0.0", () => console.log(`SHLO3 AI API listening on ${port}`));
