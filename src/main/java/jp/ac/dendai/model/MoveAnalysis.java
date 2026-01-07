package jp.ac.dendai.model;

import java.util.List;

public class MoveAnalysis {
    private int moveNumber;
    private boolean isWhite;
    private String playedMove;
    private boolean isOpeningMove;
    private List<OpeningMove> topOpeningMoves;
    private String recommendedMove;
    private Double playedMoveEval;
    private Double recommendedMoveEval;
    private String punishmentMove;
    private Double evalAfterPunishment;

    public MoveAnalysis(int moveNumber, boolean isWhite, String playedMove) {
        this.moveNumber = moveNumber;
        this.isWhite = isWhite;
        this.playedMove = playedMove;
    }

    // Getters and setters
    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public boolean isWhite() { return isWhite; }
    public void setWhite(boolean white) { isWhite = white; }

    public String getPlayedMove() { return playedMove; }
    public void setPlayedMove(String playedMove) { this.playedMove = playedMove; }

    public boolean isOpeningMove() { return isOpeningMove; }
    public void setOpeningMove(boolean openingMove) { isOpeningMove = openingMove; }

    public List<OpeningMove> getTopOpeningMoves() { return topOpeningMoves; }
    public void setTopOpeningMoves(List<OpeningMove> topOpeningMoves) {
        this.topOpeningMoves = topOpeningMoves;
    }

    public String getRecommendedMove() { return recommendedMove; }
    public void setRecommendedMove(String recommendedMove) {
        this.recommendedMove = recommendedMove;
    }

    public Double getPlayedMoveEval() { return playedMoveEval; }
    public void setPlayedMoveEval(Double playedMoveEval) {
        this.playedMoveEval = playedMoveEval;
    }

    public Double getRecommendedMoveEval() { return recommendedMoveEval; }
    public void setRecommendedMoveEval(Double recommendedMoveEval) {
        this.recommendedMoveEval = recommendedMoveEval;
    }

    public String getPunishmentMove() { return punishmentMove; }
    public void setPunishmentMove(String punishmentMove) {
        this.punishmentMove = punishmentMove;
    }

    public Double getEvalAfterPunishment() { return evalAfterPunishment; }
    public void setEvalAfterPunishment(Double evalAfterPunishment) {
        this.evalAfterPunishment = evalAfterPunishment;
    }

    /**
     * Calculate evaluation loss (positive = mistake)
     */
    public Double getEvalLoss() {
        if (playedMoveEval == null || recommendedMoveEval == null) {
            return null;
        }
        // For white, lower eval is worse. For black, higher eval is worse.
        return isWhite
            ? recommendedMoveEval - playedMoveEval
            : playedMoveEval - recommendedMoveEval;
    }

    /**
     * Check if this is a significant mistake (>= 0.5 pawn loss)
     */
    public boolean isSignificantMistake() {
        Double loss = getEvalLoss();
        return loss != null && loss >= 0.5;
    }

    /**
     * Get player name (White or Black)
     */
    public String getPlayerName() {
        return isWhite ? "White" : "Black";
    }

    /**
     * Format move number for display (e.g., "1." for white, "1..." for black)
     */
    public String getFormattedMoveNumber() {
        return isWhite ? moveNumber + "." : moveNumber + "...";
    }
}
