package jp.ac.dendai;

import jp.ac.dendai.api.LichessApiClient;
import jp.ac.dendai.model.Game;
import jp.ac.dendai.model.MoveAnalysis;
import jp.ac.dendai.model.OpeningMove;
import jp.ac.dendai.service.OpeningTrainerService;
import com.google.gson.Gson;

import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            Gson gson = new Gson();

            // Default values
            String username = "def-e";
            String playerColor = null; // Will be auto-detected
            int numGames = 1;

            // Parse command line arguments
            if (args.length > 0) username = args[0];
            if (args.length > 1) playerColor = args[1];
            if (args.length > 2) numGames = Integer.parseInt(args[2]);

            System.out.println("=== Chess Opening Trainer ===");
            System.out.println("Fetching games for user: " + username);
            System.out.println();

            // Fetch game
            LichessApiClient lichessClient = new LichessApiClient();
            String response = lichessClient.fetchGames(username, numGames);
            String firstLine = response.split("\n")[0];
            Game game = gson.fromJson(firstLine, Game.class);

            // Auto-detect player color if not specified
            if (playerColor == null) {
                playerColor = game.getPlayerColor(username);
                if (playerColor == null) {
                    System.err.println("Error: Could not determine player color. Player '" + username + "' not found in game.");
                    return;
                }
            }

            System.out.println("Game ID: " + game.getId());
            if (game.getOpening() != null && game.getOpening().getName() != null) {
                System.out.println("Opening: " + game.getOpening().getName());
            }

            // Display player information
            if (game.getPlayers() != null) {
                System.out.println();
                if (game.getPlayers().getWhite() != null &&
                    game.getPlayers().getWhite().getUser() != null) {
                    System.out.println("White: " + game.getPlayers().getWhite().getUser().getId() +
                                       " (" + game.getPlayers().getWhite().getRating() + ")");
                }
                if (game.getPlayers().getBlack() != null &&
                    game.getPlayers().getBlack().getUser() != null) {
                    System.out.println("Black: " + game.getPlayers().getBlack().getUser().getId() +
                                       " (" + game.getPlayers().getBlack().getRating() + ")");
                }
            }

            System.out.println("\nAnalyzing " + playerColor + " moves for " + username);
            System.out.println();

            // Parse moves (in SAN format)
            String[] moves = game.getMoves().split(" ");

            // Analyze game
            OpeningTrainerService trainer = new OpeningTrainerService(15);
            List<MoveAnalysis> analyses = trainer.analyzeGame(moves, playerColor);

            // Get opening theory line
            String[] theoryLine = trainer.getOpeningTheoryLine(new String[0]);

            // Display results
            boolean isWhite = "white".equalsIgnoreCase(playerColor);
            displayAnalyses(analyses, moves, isWhite, theoryLine);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayAnalyses(List<MoveAnalysis> analyses, String[] allMoves, boolean analyzingWhite, String[] theoryLine) {
        System.out.println("=== Opening Analysis Results ===\n");

        int deviations = 0;

        // Find deviation and display it
        for (int i = 0; i < analyses.size(); i++) {
            MoveAnalysis analysis = analyses.get(i);

            if (!analysis.isOpeningMove()) {
                deviations++;

                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("Move " + analysis.getFormattedMoveNumber() + " " +
                                   analysis.getPlayerName() + ": " + analysis.getPlayedMove());
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                System.out.println("❌ This move deviates from opening theory!");
                System.out.println();

                // Show top opening moves
                if (analysis.getTopOpeningMoves() != null && !analysis.getTopOpeningMoves().isEmpty()) {
                    System.out.println("📚 Top Opening Moves:");
                    analysis.getTopOpeningMoves().stream()
                        .limit(3)
                        .forEach(move -> System.out.println(
                            "   " + move.getSan() + " - " + move.getTotalGames() + " games"
                        ));
                    System.out.println();
                }

                // Show recommended move (what you should have played)
                if (analysis.getRecommendedMove() != null) {
                    System.out.println("💡 You should have played: " + analysis.getRecommendedMove());
                    System.out.println();
                }

                // Show opponent's best response to your bad move
                if (analysis.getPunishmentMove() != null) {
                    System.out.println("⚔️  Opponent's best response to your move:");
                    System.out.println("   " + analysis.getPunishmentMove());
                }

                System.out.println();
                break; // Only show first deviation
            }
        }

        // Summary
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Summary:");
        System.out.println("  Total moves analyzed: " + analyses.size());
        System.out.println("  Opening deviations: " + deviations);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Display opening theory line (15 moves)
        System.out.println();
        System.out.println("📖 Opening Sequence (Theory):");
        System.out.println();

        if (theoryLine.length < 2) {
            System.out.println("   定石情報はありません");
            System.out.println();
            return;
        }

        // Display up to 15 full moves (30 plies) from theory
        int maxPly = Math.min(30, theoryLine.length);

        // Display moves in pairs (White move, Black move)
        for (int ply = 0; ply < maxPly; ply += 2) {
            int moveNumber = (ply / 2) + 1;
            String whiteMove = theoryLine[ply];
            String blackMove = ply + 1 < maxPly ? theoryLine[ply + 1] : "";

            if (!blackMove.isEmpty()) {
                System.out.println("   " + moveNumber + ". " + whiteMove + " " + blackMove);
            } else {
                System.out.println("   " + moveNumber + ". " + whiteMove);
            }
        }

        // Show if theory ran out before 15 moves
        if (theoryLine.length < 30) {
            int lastMove = (theoryLine.length + 1) / 2;
            System.out.println();
            System.out.println("   (定石は" + lastMove + "手目までです)");
        }

        System.out.println();
    }
}