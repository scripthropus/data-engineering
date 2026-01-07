package jp.ac.dendai.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public class PositionTracker {
    private final Board board;

    public PositionTracker() {
        this.board = new Board();
    }

    /**
     * Create a tracker from FEN
     */
    public PositionTracker(String fen) {
        this.board = new Board();
        this.board.loadFromFen(fen);
    }

    /**
     * Apply a move in SAN format (e.g., "e4", "Nf3")
     */
    public void applyMoveSan(String sanMove) {
        board.doMove(sanMove);
    }

    /**
     * Apply multiple moves in SAN format
     */
    public void applyMovesSan(String[] sanMoves) {
        for (String move : sanMoves) {
            applyMoveSan(move);
        }
    }

    /**
     * Get FEN of current position
     */
    public String getFen() {
        return board.getFen();
    }

    /**
     * Get the last N moves in UCI format as comma-separated string
     */
    public String getLastMovesAsUci(int count) {
        if (count == 0 || board.getBackup().isEmpty()) {
            return "";
        }

        StringBuilder uciMoves = new StringBuilder();
        int movesAvailable = Math.min(count, board.getBackup().size());

        for (int i = 0; i < movesAvailable; i++) {
            Move move = board.getBackup().get(i).getMove();
            if (i > 0) {
                uciMoves.append(",");
            }
            uciMoves.append(move.toString());
        }

        return uciMoves.toString();
    }

    /**
     * Get all moves in UCI format as comma-separated string
     */
    public String getAllMovesAsUci() {
        if (board.getBackup().isEmpty()) {
            return "";
        }

        StringBuilder uciMoves = new StringBuilder();

        for (int i = 0; i < board.getBackup().size(); i++) {
            Move move = board.getBackup().get(i).getMove();
            if (i > 0) {
                uciMoves.append(",");
            }
            uciMoves.append(move.toString());
        }

        return uciMoves.toString();
    }

    /**
     * Get the board object
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Clone this tracker
     */
    public PositionTracker clone() {
        PositionTracker clone = new PositionTracker(this.getFen());
        return clone;
    }

    /**
     * Reset to starting position
     */
    public void reset() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }
}