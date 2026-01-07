package jp.ac.dendai.model;

import java.util.List;

public class OpeningResponse {
    private long white;
    private long draws;
    private long black;
    private List<OpeningMove> moves;

    public long getWhite() { return white; }
    public void setWhite(long white) { this.white = white; }

    public long getDraws() { return draws; }
    public void setDraws(long draws) { this.draws = draws; }

    public long getBlack() { return black; }
    public void setBlack(long black) { this.black = black; }

    public List<OpeningMove> getMoves() { return moves; }
    public void setMoves(List<OpeningMove> moves) { this.moves = moves; }
}
