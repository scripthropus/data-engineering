package jp.ac.dendai.service;

import com.google.gson.Gson;
import jp.ac.dendai.api.ChessEngineClient;
import jp.ac.dendai.api.OpeningExplorerClient;
import jp.ac.dendai.model.EngineResponse;
import jp.ac.dendai.model.MoveAnalysis;
import jp.ac.dendai.model.OpeningMove;
import jp.ac.dendai.model.OpeningResponse;
import jp.ac.dendai.util.PositionTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpeningTrainerService {
    private final OpeningExplorerClient explorerClient;
    private final ChessEngineClient engineClient;
    private final Gson gson;
    private static final int OPENING_PHASE_LIMIT = 15; // Number of moves to analyze
    private static final long MIN_GAMES = 1000; // Minimum games to consider as theory

    public OpeningTrainerService() {
        this.explorerClient = new OpeningExplorerClient();
        this.engineClient = new ChessEngineClient();
        this.gson = new Gson();
    }

    /**
     * Analyze a complete game
     * @param moves Array of moves in SAN format
     * @param playerColor "white" or "black" - the player we're analyzing
     * @return List of move analyses
     */
    public List<MoveAnalysis> analyzeGame(String[] moves, String playerColor) throws IOException {
        List<MoveAnalysis> analyses = new ArrayList<>();
        PositionTracker tracker = new PositionTracker();
        boolean analyzeWhite = "white".equalsIgnoreCase(playerColor);

        // Analyze each move in the opening phase
        for (int i = 0; i < Math.min(moves.length, OPENING_PHASE_LIMIT * 2); i++) {
            boolean isWhiteMove = i % 2 == 0;
            int moveNumber = (i / 2) + 1;
            String move = moves[i];

            // Get opening theory for current position (BEFORE applying the move)
            String currentUciMoves = tracker.getAllMovesAsUci();
            OpeningResponse openingTheory = null;

            try {
                String openingJson = explorerClient.getOpeningMoves(currentUciMoves);
                openingTheory = gson.fromJson(openingJson, OpeningResponse.class);
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch opening theory for move " + moveNumber + ": " + e.getMessage());
            }

            // Filter moves with at least MIN_GAMES
            List<OpeningMove> theoryMoves = new ArrayList<>();
            if (openingTheory != null && openingTheory.getMoves() != null) {
                theoryMoves = openingTheory.getMoves().stream()
                    .filter(om -> om.getTotalGames() >= MIN_GAMES)
                    .toList();
            }

            // Check if theory exists in this position
            if (theoryMoves.isEmpty()) {
                // Out of theory - no moves with MIN_GAMES
                if (isWhiteMove == analyzeWhite) {
                    // Mark this as out of theory
                    MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                    analysis.setOutOfTheory(true);
                    analysis.setOpeningMove(false);

                    // Get punishment move even when out of theory
                    PositionTracker afterPlayed = tracker.clone();
                    afterPlayed.applyMoveSan(move);
                    String opponentResponse = getEngineBestMove(afterPlayed.getFen());
                    if (opponentResponse != null) {
                        analysis.setPunishmentMove(opponentResponse);
                    }

                    analyses.add(analysis);
                }
                // Stop analyzing - opening phase has ended
                break;
            }

            // Theory exists - analyze only the specified player's moves
            if (isWhiteMove == analyzeWhite) {
                // Check if played move is in theory
                boolean isInTheory = theoryMoves.stream()
                    .anyMatch(om -> om.getSan().equals(move));

                MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                analysis.setOpeningMove(isInTheory);
                analysis.setOutOfTheory(false);

                // Set top opening moves (only moves with MIN_GAMES)
                analysis.setTopOpeningMoves(theoryMoves);

                if (!isInTheory) {
                    // Deviated from theory
                    // Set recommended move (top theory move)
                    OpeningMove topMove = theoryMoves.get(0);
                    analysis.setRecommendedMove(topMove.getSan());

                    // Get opponent's best response to the bad move
                    PositionTracker afterPlayed = tracker.clone();
                    afterPlayed.applyMoveSan(move);
                    String opponentResponse = getEngineBestMove(afterPlayed.getFen());
                    if (opponentResponse != null) {
                        analysis.setPunishmentMove(opponentResponse);
                    }

                    analyses.add(analysis);
                    // Apply this move and break
                    tracker.applyMoveSan(move);
                    break;
                }

                analyses.add(analysis);
            }

            // Apply the move for next iteration
            tracker.applyMoveSan(move);
        }

        return analyses;
    }

    /**
     * Get best move from engine for a position
     */
    private String getEngineBestMove(String fen) {
        try {
            String response = engineClient.getBestMove(fen);
            EngineResponse engineResponse = gson.fromJson(response, EngineResponse.class);
            return engineResponse.getSan();
        } catch (Exception e) {
            System.err.println("Warning: Could not get engine best move: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get opening theory line (15 moves)
     * @param startingMoves Initial moves played (up to deviation or start of game)
     * @return Array of opening theory moves (SAN format)
     */
    public String[] getOpeningTheoryLine(String[] startingMoves) throws IOException {
        List<String> theoryLine = new ArrayList<>();
        PositionTracker tracker = new PositionTracker();

        // Apply starting moves
        for (String move : startingMoves) {
            tracker.applyMoveSan(move);
            theoryLine.add(move);
        }

        // Continue with opening theory up to 15 full moves (30 plies)
        while (theoryLine.size() < 30) {
            String currentUciMoves = tracker.getAllMovesAsUci();

            try {
                String openingJson = explorerClient.getOpeningMoves(currentUciMoves);
                OpeningResponse openingResponse = gson.fromJson(openingJson, OpeningResponse.class);

                if (openingResponse.getMoves() == null || openingResponse.getMoves().isEmpty()) {
                    // No more theory available
                    break;
                }

                // Get the most played move (top move)
                OpeningMove topMove = openingResponse.getMoves().get(0);

                // Check if this move has enough games to be considered theory
                if (topMove.getTotalGames() < MIN_GAMES) {
                    // Not enough games, stop here
                    break;
                }

                String move = topMove.getSan();
                theoryLine.add(move);
                tracker.applyMoveSan(move);

            } catch (Exception e) {
                // If we can't get theory, stop here
                break;
            }
        }

        return theoryLine.toArray(new String[0]);
    }
}
