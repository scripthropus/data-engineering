package jp.ac.dendai.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChessEngineClient {
    private static final String BASE_URL = "https://chess-api.com/v1";
    private final Gson gson;

    public ChessEngineClient() {
        this.gson = new Gson();
    }

    /**
     * Get best move for a position
     * @param fen Position in FEN format
     * @return JSON response from engine
     */
    public String getBestMove(String fen) throws IOException {
        URL url = new URL(BASE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("fen", fen);
        requestBody.addProperty("depth", 12);
        requestBody.addProperty("variants", 1);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP error: " + responseCode);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        return response.toString();
    }
}