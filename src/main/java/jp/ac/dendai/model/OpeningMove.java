package jp.ac.dendai.model;

public class OpeningMove {
    private String uci;
    private String san;
    private int white;
    private int draws;
    private int black;
    private int averageRating;
    
    public String getUci() { return uci; }
    public void setUci(String uci) { this.uci = uci; }
    
    public String getSan() { return san; }
    public void setSan(String san) { this.san = san; }
    
    public int getWhite() { return white; }
    public void setWhite(int white) { this.white = white; }
    
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    
    public int getBlack() { return black; }
    public void setBlack(int black) { this.black = black; }
    
    public int getAverageRating() { return averageRating; }
    public void setAverageRating(int averageRating) { this.averageRating = averageRating; }
    
    public int getTotalGames() {
        return white + draws + black;
    }
}