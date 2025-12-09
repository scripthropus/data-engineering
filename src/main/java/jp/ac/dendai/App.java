package jp.ac.dendai;

import jp.ac.dendai.api.LichessApiClient;
import jp.ac.dendai.api.OpeningExplorerClient;
import jp.ac.dendai.api.ChessEngineClient;
import jp.ac.dendai.model.Game;
import jp.ac.dendai.model.OpeningResponse;
import jp.ac.dendai.util.PositionTracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class App {
    public static void main(String[] args) {
        try {
            Gson gson = new Gson();
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            
            // Fetch one game
            LichessApiClient lichessClient = new LichessApiClient();
            String response = lichessClient.fetchGames("def-e", 1);
            String firstLine = response.split("\n")[0];
            Game game = gson.fromJson(firstLine, Game.class);
            
            System.out.println("=== Game Info ===");
            System.out.println("Game ID: " + game.getId());
            System.out.println("Moves: " + game.getMoves());
            
            // Parse moves (in SAN format)
            String[] moves = game.getMoves().split(" ");
            
            // Track position using SAN moves
            PositionTracker tracker = new PositionTracker();
            tracker.applyMovesSan(new String[]{moves[0], moves[1], moves[2]});
            
            // Get UCI format for API and FEN for engine
            String movesUci = tracker.getLastMovesAsUci(3);
            String fen = tracker.getFen();
            
            System.out.println("\n=== Opening Theory (after move 3) ===");
            System.out.println("Moves (UCI): " + movesUci);
            System.out.println("FEN: " + fen);
            
            // Get opening moves
            OpeningExplorerClient explorerClient = new OpeningExplorerClient();
            String openingJson = explorerClient.getOpeningMoves(movesUci);
            
            OpeningResponse openingResponse = gson.fromJson(openingJson, OpeningResponse.class);
            System.out.println("\nPosition statistics:");
            System.out.println("White wins: " + openingResponse.getWhite());
            System.out.println("Draws: " + openingResponse.getDraws());
            System.out.println("Black wins: " + openingResponse.getBlack());
            
            System.out.println("\nTop 5 moves from this position:");
            openingResponse.getMoves().stream()
                .limit(5)
                .forEach(move -> System.out.println(
                    move.getSan() + " (UCI: " + move.getUci() + ") - " + 
                    move.getTotalGames() + " games"
                ));
            
            // Get best move from chess engine
            System.out.println("\n=== Chess Engine Analysis ===");
            ChessEngineClient engineClient = new ChessEngineClient();
            String engineResponse = engineClient.getBestMove(fen);
            
            System.out.println("Raw Engine Response:");
            Object engineJson = gson.fromJson(engineResponse, Object.class);
            System.out.println(prettyGson.toJson(engineJson));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}