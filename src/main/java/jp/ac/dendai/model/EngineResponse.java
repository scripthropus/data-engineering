package jp.ac.dendai.model;

import java.util.List;

public class EngineResponse {
    private String text;
    private String from;
    private String to;
    private String san;
    private Double eval;
    private Integer depth;
    private List<String> pv; // Principal Variation (best line)

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSan() { return san; }
    public void setSan(String san) { this.san = san; }

    public Double getEval() { return eval; }
    public void setEval(Double eval) { this.eval = eval; }

    public Integer getDepth() { return depth; }
    public void setDepth(Integer depth) { this.depth = depth; }

    public List<String> getPv() { return pv; }
    public void setPv(List<String> pv) { this.pv = pv; }

    /**
     * Get best move in UCI format (e.g., "e2e4")
     */
    public String getBestMoveUci() {
        if (from != null && to != null) {
            return from + to;
        }
        return null;
    }

    /**
     * Get evaluation in centipawns
     * Positive = better for white, Negative = better for black
     */
    public double getEvaluation() {
        return eval != null ? eval : 0.0;
    }
}
