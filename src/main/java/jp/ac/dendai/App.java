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
            OpeningTrainerService trainer = new OpeningTrainerService();
            List<MoveAnalysis> analyses = trainer.analyzeGame(moves, playerColor);

            // Get starting moves for theory line (moves that were in theory)
            int lastTheoryMoveIndex = -1;

            for (MoveAnalysis analysis : analyses) {
                if (analysis.isOpeningMove()) {
                    // Calculate the actual index in the moves array
                    int moveNumber = analysis.getMoveNumber();
                    boolean isWhiteMove = analysis.isWhite();
                    lastTheoryMoveIndex = (moveNumber - 1) * 2 + (isWhiteMove ? 0 : 1);
                }
            }

            // Get starting moves (up to last theory move)
            String[] startingMoves;
            if (lastTheoryMoveIndex >= 0) {
                startingMoves = java.util.Arrays.copyOfRange(moves, 0, lastTheoryMoveIndex + 1);
            } else {
                startingMoves = new String[0];
            }

            // Get opening theory line based on actual game
            String[] theoryLine = trainer.getOpeningTheoryLine(startingMoves);

            // Display results
            displayAnalyses(analyses, theoryLine);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayAnalyses(List<MoveAnalysis> analyses, String[] theoryLine) {
        System.out.println("=== Opening Analysis Results ===\n");

        // Count moves that followed theory
        int movesInTheory = 0;
        MoveAnalysis deviationAnalysis = null;
        MoveAnalysis outOfTheoryAnalysis = null;

        for (MoveAnalysis analysis : analyses) {
            if (analysis.isOpeningMove()) {
                movesInTheory++;
            } else if (analysis.isOutOfTheory()) {
                outOfTheoryAnalysis = analysis;
                break;
            } else {
                deviationAnalysis = analysis;
                break;
            }
        }

        // Display deviation or out-of-theory
        if (deviationAnalysis != null) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("Move " + deviationAnalysis.getFormattedMoveNumber() + " " +
                               deviationAnalysis.getPlayerName() + ": " + deviationAnalysis.getPlayedMove());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            System.out.println("❌ This move deviates from opening theory!");
            System.out.println();

            // Show recommended move (what you should have played)
            if (deviationAnalysis.getRecommendedMove() != null) {
                System.out.println("💡 You should have played: " + deviationAnalysis.getRecommendedMove());
                System.out.println();
            }

            // Show top opening moves (1000+ games, max 3)
            if (deviationAnalysis.getTopOpeningMoves() != null && !deviationAnalysis.getTopOpeningMoves().isEmpty()) {
                System.out.println("📚 Top Opening Moves:");
                deviationAnalysis.getTopOpeningMoves().stream()
                    .limit(3)
                    .forEach(move -> System.out.println(
                        "   " + move.getSan() + " - " + move.getTotalGames() + " games"
                    ));
                System.out.println();
            }

            // Show opponent's best response to your bad move
            if (deviationAnalysis.getPunishmentMove() != null) {
                System.out.println("⚔️  Opponent's best response to your move:");
                System.out.println("   " + deviationAnalysis.getPunishmentMove());
            }

            System.out.println();
        } else if (outOfTheoryAnalysis != null) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("Move " + outOfTheoryAnalysis.getFormattedMoveNumber() + " " +
                               outOfTheoryAnalysis.getPlayerName() + ": " + outOfTheoryAnalysis.getPlayedMove());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            System.out.println("ℹ️  Opening theory ends here");
            System.out.println("   (No moves with 1000+ games in this position)");
            System.out.println();

            // Show opponent's best response
            if (outOfTheoryAnalysis.getPunishmentMove() != null) {
                System.out.println("⚔️  Opponent's best response to your move:");
                System.out.println("   " + outOfTheoryAnalysis.getPunishmentMove());
            }

            System.out.println();
        }

        // Summary
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Summary:");
        System.out.println("  Opening followed: " + movesInTheory + " moves");

        if (deviationAnalysis != null) {
            System.out.println("  Result: Deviated at move " + deviationAnalysis.getMoveNumber());
        } else if (outOfTheoryAnalysis != null) {
            System.out.println("  Result: Reached end of theory");
        } else {
            System.out.println("  Result: Completed opening theory");
        }
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