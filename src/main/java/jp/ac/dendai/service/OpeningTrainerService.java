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
    private static final long MIN_GAMES = 100;

    public OpeningTrainerService() {
        this.explorerClient = new OpeningExplorerClient();
        this.engineClient = new ChessEngineClient();
        this.gson = new Gson();
    }

    public List<MoveAnalysis> analyzeGame(String[] moves, String playerColor) throws IOException {
        List<MoveAnalysis> analyses = new ArrayList<>();
        PositionTracker tracker = new PositionTracker();
        boolean isPlayerWhite = "white".equalsIgnoreCase(playerColor);

        for (int i = 0; i < Math.min(moves.length, 30); i++) {
            boolean isWhiteMove = (i % 2 == 0);
            int moveNumber = (i / 2) + 1;
            String move = moves[i];

            List<OpeningMove> theoryMoves = getTheoryMoves(tracker);
            
            if (theoryMoves.isEmpty()) {
                if (isWhiteMove == isPlayerWhite) {
                    MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                    analysis.setOutOfTheory(true);
                    analysis.setPunishmentMove(getBestResponse(tracker, move));
                    analyses.add(analysis);
                }
                break;
            }

            boolean isInTheory = theoryMoves.stream()
                .anyMatch(m -> m.getSan().equals(move));

            if (!isInTheory) {
                if (isWhiteMove == isPlayerWhite) {
                    MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                    analysis.setOpeningMove(false);
                    analysis.setRecommendedMove(theoryMoves.get(0).getSan());
                    analysis.setTopOpeningMoves(theoryMoves.stream().limit(3).toList());
                    analysis.setPunishmentMove(getBestResponse(tracker, move));
                    analyses.add(analysis);
                } else {
                    MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                    analysis.setOpeningMove(false);
                    analysis.setPunishmentMove(getBestResponse(tracker, move));
                    analyses.add(analysis);
                }
                break;
            }

            if (isWhiteMove == isPlayerWhite) {
                MoveAnalysis analysis = new MoveAnalysis(moveNumber, isWhiteMove, move);
                analysis.setOpeningMove(true);
                analyses.add(analysis);
            }

            tracker.applyMoveSan(move);
        }

        return analyses;
    }

    public String[] getTheoryLine(String[] actualMoves) throws IOException {
        List<String> theoryLine = new ArrayList<>();
        PositionTracker tracker = new PositionTracker();
        
        for (int i = 0; i < Math.min(actualMoves.length, 30); i++) {
            String move = actualMoves[i];
            
            List<OpeningMove> theoryMoves = getTheoryMoves(tracker);
            
            if (theoryMoves.isEmpty()) break;
            
            boolean isInTheory = theoryMoves.stream()
                .anyMatch(m -> m.getSan().equals(move));
            
            if (!isInTheory) {
                OpeningMove correctMove = theoryMoves.get(0);
                theoryLine.add(correctMove.getSan());
                tracker.applyMoveSan(correctMove.getSan());
                break;
            }
            
            theoryLine.add(move);
            tracker.applyMoveSan(move);
        }
        
        while (theoryLine.size() < 30) {
            List<OpeningMove> theoryMoves = getTheoryMoves(tracker);
            if (theoryMoves.isEmpty()) break;
            
            String move = theoryMoves.get(0).getSan();
            theoryLine.add(move);
            tracker.applyMoveSan(move);
        }
        
        return theoryLine.toArray(new String[0]);
    }

    private List<OpeningMove> getTheoryMoves(PositionTracker tracker) throws IOException {
        String json = explorerClient.getOpeningMoves(tracker.getAllMovesAsUci());
        OpeningResponse response = gson.fromJson(json, OpeningResponse.class);
        
        if (response.getMoves() == null) return new ArrayList<>();
        
        return response.getMoves().stream()
            .filter(m -> m.getTotalGames() >= MIN_GAMES)
            .toList();
    }

    private String getBestResponse(PositionTracker tracker, String move) {
        try {
            PositionTracker after = tracker.clone();
            after.applyMoveSan(move);
            String json = engineClient.getBestMove(after.getFen());
            return gson.fromJson(json, EngineResponse.class).getSan();
        } catch (Exception e) {
            return null;
        }
    }
}