package jp.ac.dendai;

import jp.ac.dendai.api.LichessApiClient;
import jp.ac.dendai.model.Game;
import com.google.gson.Gson;

public class App {
    public static void main(String[] args) {
        try {
            LichessApiClient client = new LichessApiClient();
            String response = client.fetchGames("def-e", 1);
            
            Gson gson = new Gson();
            String firstLine = response.split("\n")[0];
            Game game = gson.fromJson(firstLine, Game.class);
            
            System.out.println("Game ID: " + game.getId());
            System.out.println("Opening: " + (game.getOpening() != null ? game.getOpening().getName() : "N/A"));
            System.out.println("Moves: " + game.getMoves());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
