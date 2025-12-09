package jp.ac.dendai.model;

import java.util.List;

public class OpeningResponse {
    private int white;
    private int draws;
    private int black;
    private List<OpeningMove> moves;
    
    public int getWhite() { return white; }
    public void setWhite(int white) { this.white = white; }
    
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    
    public int getBlack() { return black; }
    public void setBlack(int black) { this.black = black; }
    
    public List<OpeningMove> getMoves() { return moves; }
    public void setMoves(List<OpeningMove> moves) { this.moves = moves; }
}
