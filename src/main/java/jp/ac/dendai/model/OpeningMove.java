package jp.ac.dendai.model;

public class OpeningMove {
    private String uci;
    private String san;
    private long white;
    private long draws;
    private long black;
    private int averageRating;

    public String getUci() { return uci; }
    public void setUci(String uci) { this.uci = uci; }

    public String getSan() { return san; }
    public void setSan(String san) { this.san = san; }

    public long getWhite() { return white; }
    public void setWhite(long white) { this.white = white; }

    public long getDraws() { return draws; }
    public void setDraws(long draws) { this.draws = draws; }

    public long getBlack() { return black; }
    public void setBlack(long black) { this.black = black; }

    public int getAverageRating() { return averageRating; }
    public void setAverageRating(int averageRating) { this.averageRating = averageRating; }

    public long getTotalGames() {
        return white + draws + black;
    }
}