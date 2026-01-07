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
    private final int openingPhaseLimit; // Number of moves to analyze

    public OpeningTrainerService() {
        this(15); // Default: analyze first 15 moves (30 plies)
    }

    public OpeningTrainerService(int openingPhaseLimit) {
        this.explorerClient = new OpeningExplorerClient();
        this.engineClient = new ChessEngineClient();
        this.gson = new Gson();
        this.openingPhaseLimit = openingPhaseLimit;
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
        for (int i = 0; i < Math.min(moves.length, openingPhaseLimit * 2); i++) {
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

            // Only analyze the specified player's moves
            if (isWhiteMove == analyzeWhite) {
                // Create analysis
                MoveAnalysis analysis = analyzeMoveInPosition(
                    tracker,
                    move,
                    moveNumber,
                    isWhiteMove,
                    openingTheory
                );

                analyses.add(analysis);
            }

            // Apply the move for next iteration
            tracker.applyMoveSan(move);
        }

        return analyses;
    }

    /**
     * Analyze a single move in a position
     */
    private MoveAnalysis analyzeMoveInPosition(
            PositionTracker tracker,
            String playedMove,
            int moveNumber,
            boolean isWhiteMove,
            OpeningResponse openingTheory
    ) throws IOException {

        MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, playedMove);

        // Check if move is in opening book
        if (openingTheory != null && openingTheory.getMoves() != null) {
            analysis.setTopOpeningMoves(openingTheory.getMoves());

            // Check if played move is in top opening moves
            boolean isOpeningMove = openingTheory.getMoves().stream()
                .anyMatch(om -> om.getSan().equals(playedMove));

            analysis.setOpeningMove(isOpeningMove);

            if (!isOpeningMove && !openingTheory.getMoves().isEmpty()) {
                // Move deviated from theory - get recommended move
                OpeningMove topMove = openingTheory.getMoves().get(0);
                analysis.setRecommendedMove(topMove.getSan());

                // Evaluate both moves using engine
                evaluateMoves(tracker, analysis, playedMove, topMove.getSan());
            }
        } else {
            // Out of book - use engine only
            analysis.setOpeningMove(false);
            String engineBestMove = getEngineBestMove(tracker.getFen());

            if (engineBestMove != null && !engineBestMove.equals(playedMove)) {
                analysis.setRecommendedMove(engineBestMove);
                evaluateMoves(tracker, analysis, playedMove, engineBestMove);
            }
        }

        return analysis;
    }

    /**
     * Evaluate both the played move and recommended move
     */
    private void evaluateMoves(
            PositionTracker tracker,
            MoveAnalysis analysis,
            String playedMove,
            String recommendedMove
    ) throws IOException {

        // Evaluate after played move
        PositionTracker afterPlayed = tracker.clone();
        afterPlayed.applyMoveSan(playedMove);
        Double playedEval = getPositionEvaluation(afterPlayed.getFen());
        analysis.setPlayedMoveEval(playedEval);

        // Evaluate after recommended move
        PositionTracker afterRecommended = tracker.clone();
        afterRecommended.applyMoveSan(recommendedMove);
        Double recommendedEval = getPositionEvaluation(afterRecommended.getFen());
        analysis.setRecommendedMoveEval(recommendedEval);

        // Find punishment for the played move
        if (analysis.isSignificantMistake()) {
            String punishment = getEngineBestMove(afterPlayed.getFen());
            analysis.setPunishmentMove(punishment);

            if (punishment != null) {
                PositionTracker afterPunishment = afterPlayed.clone();
                afterPunishment.applyMoveSan(punishment);
                Double punishmentEval = getPositionEvaluation(afterPunishment.getFen());
                analysis.setEvalAfterPunishment(punishmentEval);
            }
        }
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
     * Get position evaluation from engine
     */
    private Double getPositionEvaluation(String fen) {
        try {
            String response = engineClient.getBestMove(fen);
            EngineResponse engineResponse = gson.fromJson(response, EngineResponse.class);
            return engineResponse.getEvaluation();
        } catch (Exception e) {
            System.err.println("Warning: Could not get position evaluation: " + e.getMessage());
            return null;
        }
    }

}
