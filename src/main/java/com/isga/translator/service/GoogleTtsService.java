package com.isga.translator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

public class GoogleTtsService {

    private static final String VERTEX_TTS_URL =
            "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1"
            + "/publishers/google/models/gemini-2.5-flash-preview-tts:generateContent";

    private static final int SAMPLE_RATE = 24000;
    private static final int CHANNELS = 1;
    private static final int BITS = 16;

    private String keyFilePath;
    private HttpClient httpClient;

    public GoogleTtsService(String keyFilePath) {
        this.keyFilePath = keyFilePath;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String synthesize(String darijaText) throws Exception {
        String accessToken = GoogleAuthService.getAccessToken(keyFilePath);
        String projectId = GoogleAuthService.getProjectId(keyFilePath);

        String prompt = "You are a young native Moroccan speaker from Casablanca. "
                + "Read the following Moroccan Darija text out loud exactly as a real Moroccan would say it in casual conversation — "
                + "with a natural Casablanca street accent, relaxed rhythm, and authentic Moroccan intonation. "
                + "Do NOT use a Modern Standard Arabic or Egyptian accent. "
                + "Pronounce Darija-specific sounds correctly: the 'gh', the guttural 'q' often pronounced as 'g' in Darija, "
                + "the 'kh', and the French-borrowed words with their French pronunciation. "
                + "Sound natural, warm, and conversational — like talking to a friend, not reading a formal text:\n"
                + darijaText;

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject prebuiltVoice = new JsonObject();
        prebuiltVoice.addProperty("voiceName", "Kore");

        JsonObject voiceConfig = new JsonObject();
        voiceConfig.add("prebuiltVoiceConfig", prebuiltVoice);

        JsonObject speechConfig = new JsonObject();
        speechConfig.add("voiceConfig", voiceConfig);

        JsonArray modalities = new JsonArray();
        modalities.add("AUDIO");

        JsonObject generationConfig = new JsonObject();
        generationConfig.add("response_modalities", modalities);
        generationConfig.add("speech_config", speechConfig);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        String url = String.format(VERTEX_TTS_URL, projectId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Gemini TTS error (" + resp.statusCode() + "): " + resp.body());
        }

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonObject inlineData = json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .getAsJsonObject("inlineData");

        String pcmBase64 = inlineData.get("data").getAsString();
        byte[] pcmBytes = Base64.getDecoder().decode(pcmBase64);

        byte[] wavBytes = convertPcmToWav(pcmBytes);
        return Base64.getEncoder().encodeToString(wavBytes);
    }

    private byte[] convertPcmToWav(byte[] pcmData) throws Exception {
        int dataSize = pcmData.length;
        int byteRate = SAMPLE_RATE * CHANNELS * BITS / 8;
        int blockAlign = CHANNELS * BITS / 8;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);

        header.put("RIFF".getBytes());
        header.putInt(36 + dataSize);
        header.put("WAVE".getBytes());

        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) CHANNELS);
        header.putInt(SAMPLE_RATE);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) BITS);

        header.put("data".getBytes());
        header.putInt(dataSize);

        out.write(header.array());
        out.write(pcmData);
        return out.toByteArray();
    }
}
