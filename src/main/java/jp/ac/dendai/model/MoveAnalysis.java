package jp.ac.dendai.model;

import java.util.List;

public class MoveAnalysis {
    private int moveNumber;
    private boolean isWhite;
    private String playedMove;
    private boolean isOpeningMove;
    private boolean isOutOfTheory;  // True if this position has no theory (< 100 games)
    private List<OpeningMove> topOpeningMoves;
    private String recommendedMove;
    private String punishmentMove;

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

    public boolean isOutOfTheory() { return isOutOfTheory; }
    public void setOutOfTheory(boolean outOfTheory) { isOutOfTheory = outOfTheory; }

    public List<OpeningMove> getTopOpeningMoves() { return topOpeningMoves; }
    public void setTopOpeningMoves(List<OpeningMove> topOpeningMoves) {
        this.topOpeningMoves = topOpeningMoves;
    }

    public String getRecommendedMove() { return recommendedMove; }
    public void setRecommendedMove(String recommendedMove) {
        this.recommendedMove = recommendedMove;
    }

    public String getPunishmentMove() { return punishmentMove; }
    public void setPunishmentMove(String punishmentMove) {
        this.punishmentMove = punishmentMove;
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
