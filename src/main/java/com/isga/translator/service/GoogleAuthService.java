package com.isga.translator.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Jwts;

import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

public class GoogleAuthService {

    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static String getAccessToken(String keyFilePath) throws Exception {
        JsonObject key = JsonParser.parseReader(new FileReader(keyFilePath)).getAsJsonObject();
        String clientEmail = key.get("client_email").getAsString();
        String privateKeyPem = key.get("private_key").getAsString();
        String tokenUri = key.get("token_uri").getAsString();

        PrivateKey privateKey = loadPrivateKey(privateKeyPem);

        long now = System.currentTimeMillis() / 1000;
        String jwt = Jwts.builder()
                .issuer(clientEmail)
                .claim("aud", tokenUri)
                .claim("scope", SCOPE)
                .issuedAt(new Date(now * 1000))
                .expiration(new Date((now + 3600) * 1000))
                .signWith(privateKey)
                .compact();

        String body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();

        if (!json.has("access_token")) {
            throw new RuntimeException("Failed to get access token: " + resp.body());
        }
        return json.get("access_token").getAsString();
    }

    public static String getProjectId(String keyFilePath) throws Exception {
        JsonObject key = JsonParser.parseReader(new FileReader(keyFilePath)).getAsJsonObject();
        return key.get("project_id").getAsString();
    }

    private static PrivateKey loadPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
