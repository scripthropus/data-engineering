package jp.ac.dendai.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OpeningExplorerClient {
    private static final String BASE_URL = "https://explorer.lichess.ovh/lichess";

    /**
     * Get opening theory moves for a position
     * @param uciMoves Comma-separated UCI moves (e.g., "e2e4,e7e5,g1f3")
     * @return JSON response with opening statistics
     */
    public String getOpeningMoves(String uciMoves) throws IOException {
        // Don't URL encode the commas - they're part of the API format
        String urlStr = BASE_URL + "?play=" + uciMoves;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        
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