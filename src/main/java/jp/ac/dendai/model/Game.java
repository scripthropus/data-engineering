package jp.ac.dendai.model;

public class Game {
    private String id;
    private String moves;
    private Opening opening;
    private Players players;

    public static class Opening {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Players {
        private Player white;
        private Player black;

        public Player getWhite() { return white; }
        public void setWhite(Player white) { this.white = white; }

        public Player getBlack() { return black; }
        public void setBlack(Player black) { this.black = black; }
    }

    public static class Player {
        private String user;
        private String userId;
        private int rating;

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMoves() { return moves; }
    public void setMoves(String moves) { this.moves = moves; }

    public Opening getOpening() { return opening; }
    public void setOpening(Opening opening) { this.opening = opening; }

    public Players getPlayers() { return players; }
    public void setPlayers(Players players) { this.players = players; }

    /**
     * Get the color of the specified player
     * @param username Username to check
     * @return "white", "black", or null if not found
     */
    public String getPlayerColor(String username) {
        if (players == null) return null;

        if (players.getWhite() != null &&
            players.getWhite().getUserId() != null &&
            players.getWhite().getUserId().equalsIgnoreCase(username)) {
            return "white";
        }

        if (players.getBlack() != null &&
            players.getBlack().getUserId() != null &&
            players.getBlack().getUserId().equalsIgnoreCase(username)) {
            return "black";
        }

        return null;
    }
}