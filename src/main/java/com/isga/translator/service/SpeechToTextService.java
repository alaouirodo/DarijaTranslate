package com.isga.translator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SpeechToTextService {

    private static final String STT_URL = "https://speech.googleapis.com/v1/speech:recognize";

    private String keyFilePath;
    private HttpClient httpClient;

    public SpeechToTextService(String keyFilePath) {
        this.keyFilePath = keyFilePath;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String transcribe(String audioBase64) throws Exception {
        String accessToken = GoogleAuthService.getAccessToken(keyFilePath);

        JsonObject config = new JsonObject();
        config.addProperty("encoding", "WEBM_OPUS");
        config.addProperty("sampleRateHertz", 48000);
        config.addProperty("languageCode", "en-US");
        config.addProperty("model", "latest_short");

        JsonObject audio = new JsonObject();
        audio.addProperty("content", audioBase64);

        JsonObject body = new JsonObject();
        body.add("config", config);
        body.add("audio", audio);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Speech-to-Text error (" + resp.statusCode() + "): " + resp.body());
        }

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray results = json.getAsJsonArray("results");

        if (results == null || results.isEmpty()) {
            return "";
        }

        return results.get(0).getAsJsonObject()
                .getAsJsonArray("alternatives")
                .get(0).getAsJsonObject()
                .get("transcript").getAsString()
                .trim();
    }
}
