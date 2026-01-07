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
            String playerColor = "white";
            int numGames = 1;

            // Parse command line arguments
            if (args.length > 0) username = args[0];
            if (args.length > 1) playerColor = args[1];
            if (args.length > 2) numGames = Integer.parseInt(args[2]);

            System.out.println("=== Chess Opening Trainer ===");
            System.out.println("Analyzing " + playerColor + " moves for user: " + username);
            System.out.println();

            // Fetch game
            LichessApiClient lichessClient = new LichessApiClient();
            String response = lichessClient.fetchGames(username, numGames);
            String firstLine = response.split("\n")[0];
            Game game = gson.fromJson(firstLine, Game.class);

            System.out.println("Game ID: " + game.getId());
            if (game.getOpening() != null && game.getOpening().getName() != null) {
                System.out.println("Opening: " + game.getOpening().getName());
            }
            System.out.println();

            // Parse moves (in SAN format)
            String[] moves = game.getMoves().split(" ");

            // Analyze game
            OpeningTrainerService trainer = new OpeningTrainerService(15);
            List<MoveAnalysis> analyses = trainer.analyzeGame(moves, playerColor);

            // Display results
            displayAnalyses(analyses);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayAnalyses(List<MoveAnalysis> analyses) {
        System.out.println("=== Opening Analysis Results ===\n");

        int deviations = 0;
        int mistakes = 0;

        for (MoveAnalysis analysis : analyses) {
            if (!analysis.isOpeningMove()) {
                deviations++;

                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("Move " + analysis.getFormattedMoveNumber() + " " +
                                   analysis.getPlayerName() + ": " + analysis.getPlayedMove());
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                System.out.println("❌ This move deviates from opening theory!");
                System.out.println();

                // Show recommended moves
                if (analysis.getTopOpeningMoves() != null && !analysis.getTopOpeningMoves().isEmpty()) {
                    System.out.println("📚 Top Opening Moves:");
                    analysis.getTopOpeningMoves().stream()
                        .limit(3)
                        .forEach(move -> System.out.println(
                            "   " + move.getSan() + " - " + move.getTotalGames() + " games"
                        ));
                    System.out.println();
                }

                if (analysis.getRecommendedMove() != null) {
                    System.out.println("💡 Recommended: " + analysis.getRecommendedMove());
                }

                // Show evaluation if available
                if (analysis.getPlayedMoveEval() != null && analysis.getRecommendedMoveEval() != null) {
                    System.out.println();
                    System.out.println("📊 Evaluation:");
                    System.out.println("   After " + analysis.getPlayedMove() + ": " +
                                       formatEval(analysis.getPlayedMoveEval()));
                    System.out.println("   After " + analysis.getRecommendedMove() + ": " +
                                       formatEval(analysis.getRecommendedMoveEval()));

                    Double loss = analysis.getEvalLoss();
                    if (loss != null && loss > 0.1) {
                        System.out.println("   Loss: " + String.format("%.2f", loss) + " pawns");

                        if (analysis.isSignificantMistake()) {
                            mistakes++;
                            System.out.println("   ⚠️  Significant mistake!");
                        }
                    }
                }

                // Show punishment
                if (analysis.getPunishmentMove() != null) {
                    System.out.println();
                    System.out.println("⚔️  How to punish:");
                    System.out.println("   Opponent should play: " + analysis.getPunishmentMove());
                    if (analysis.getEvalAfterPunishment() != null) {
                        System.out.println("   Resulting eval: " +
                                           formatEval(analysis.getEvalAfterPunishment()));
                    }
                }

                System.out.println();
            }
        }

        // Summary
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Summary:");
        System.out.println("  Total moves analyzed: " + analyses.size());
        System.out.println("  Opening deviations: " + deviations);
        System.out.println("  Significant mistakes: " + mistakes);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private static String formatEval(double eval) {
        if (eval > 0) {
            return "+" + String.format("%.2f", eval);
        } else {
            return String.format("%.2f", eval);
        }
    }
}