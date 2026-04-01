package com.isga.translator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiTranslationService {

    private static final String VERTEX_URL =
            "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1"
            + "/publishers/google/models/gemini-2.0-flash:generateContent";

    private String keyFilePath;
    private HttpClient httpClient;

    public GeminiTranslationService(String keyFilePath) {
        this.keyFilePath = keyFilePath;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String[] translate(String englishText) throws Exception {
        String accessToken = GoogleAuthService.getAccessToken(keyFilePath);
        String projectId = GoogleAuthService.getProjectId(keyFilePath);

        String prompt = "You are a native Moroccan who speaks everyday street Darija. "
                + "Translate the following English text into natural, casual Moroccan Darija exactly how a young Moroccan would say it in real life — "
                + "not formal Modern Standard Arabic, not Classical Arabic. "
                + "Use real Darija vocabulary and contractions: "
                + "e.g. 'ana' not 'ana arid', 'bghit' not 'urid', 'mzyan' not 'jayyid', 'walo' not 'la shay', "
                + "'fin mchiti' not 'ayna dhahabt', 'khouya/khti' for bro/sis, 'daba' for now, 'bzzaf' for a lot, "
                + "'3awtini' for give me, 'kifach' for how, 'mnin' for where from. "
                + "It is normal and encouraged to mix in French words the way Moroccans naturally do (code-switching), "
                + "e.g. 'mchit l-bureau', 'nta okay?', '3andi rendez-vous'. "
                + "Keep the tone casual, warm, and street-authentic. "
                + "Return exactly two lines, nothing else:\n"
                + "Line 1: The translation in Arabic script (Darija, not MSA)\n"
                + "Line 2: The same translation in Latin/Franco-Arab as Moroccans write it online "
                + "(e.g. 3, 7, 9, 5 for Arabic letters — like '3awtini', 'l7al', 'b9iya')\n\n"
                + englishText;

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray partsArr = new JsonArray();
        partsArr.add(part);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", partsArr);

        JsonArray contentsArr = new JsonArray();
        contentsArr.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contentsArr);

        String url = String.format(VERTEX_URL, projectId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Vertex AI error (" + resp.statusCode() + "): " + resp.body());
        }

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        String rawText = json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString()
                .trim();

        String[] lines = rawText.split("\\r?\\n", 2);
        String arabic = lines[0].trim();
        String latin = lines.length > 1 ? lines[1].trim() : arabic;

        return new String[] { arabic, latin };
    }
}
