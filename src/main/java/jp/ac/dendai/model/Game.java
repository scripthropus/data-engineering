package jp.ac.dendai.model;

public class Game {
    private String id;
    private String moves;
    private Opening opening;
    
    public static class Opening {
        private String name;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getMoves() { return moves; }
    public void setMoves(String moves) { this.moves = moves; }
    
    public Opening getOpening() { return opening; }
    public void setOpening(Opening opening) { this.opening = opening; }
}