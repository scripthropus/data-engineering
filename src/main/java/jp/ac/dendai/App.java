package jp.ac.dendai;

import jp.ac.dendai.api.LichessApiClient;
import jp.ac.dendai.model.Game;
import jp.ac.dendai.model.MoveAnalysis;
import jp.ac.dendai.service.OpeningTrainerService;
import com.google.gson.Gson;

import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            Gson gson = new Gson();

            // Default values
            String username = "def-e";
            String playerColor = null;
            int numGames = 1;

            // Parse command line arguments
            if (args.length > 0) username = args[0];
            if (args.length > 1) playerColor = args[1];
            if (args.length > 2) numGames = Integer.parseInt(args[2]);

            System.out.println("=== チェス定石トレーナー ===");
            System.out.println("ユーザー: " + username + " の対局を取得中");
            System.out.println();

            // Fetch game
            LichessApiClient lichessClient = new LichessApiClient();
            String response = lichessClient.fetchGames(username, numGames);
            String firstLine = response.split("\n")[0];
            Game game = gson.fromJson(firstLine, Game.class);

            // Auto-detect player color
            if (playerColor == null) {
                playerColor = game.getPlayerColor(username);
                if (playerColor == null) {
                    System.err.println("エラー: プレイヤーの手番を判定できませんでした．");
                    return;
                }
            }

            System.out.println("対局ID: " + game.getId());
            if (game.getOpening() != null && game.getOpening().getName() != null) {
                System.out.println("オープニング: " + game.getOpening().getName());
            }

            // Display player information
            if (game.getPlayers() != null) {
                System.out.println();
                if (game.getPlayers().getWhite() != null &&
                    game.getPlayers().getWhite().getUser() != null) {
                    System.out.println("白: " + game.getPlayers().getWhite().getUser().getId() +
                                       " (" + game.getPlayers().getWhite().getRating() + ")");
                }
                if (game.getPlayers().getBlack() != null &&
                    game.getPlayers().getBlack().getUser() != null) {
                    System.out.println("黒: " + game.getPlayers().getBlack().getUser().getId() +
                                       " (" + game.getPlayers().getBlack().getRating() + ")");
                }
            }

            System.out.println("\n対局を解析中");
            System.out.println();

            // Parse moves
            String[] moves = game.getMoves().split(" ");

            // Analyze game
            OpeningTrainerService trainer = new OpeningTrainerService();
            List<MoveAnalysis> analyses = trainer.analyzeGame(moves, playerColor);
            String[] theoryLine = trainer.getTheoryLine(moves);

            // Display results
            displayAnalyses(analyses, theoryLine);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void displayAnalyses(List<MoveAnalysis> analyses, String[] theoryLine) {
        System.out.println("=== 序盤解析結果 ===\n");

        // 最後の解析結果を判定
        MoveAnalysis lastAnalysis = analyses.isEmpty() ? null : analyses.get(analyses.size() - 1);
        int theoryMoveCount = 0;
        
        for (MoveAnalysis a : analyses) {
            if (a.isOpeningMove()) theoryMoveCount++;
        }

        // 逸脱または定石終了の表示
        if (lastAnalysis != null && !lastAnalysis.isOpeningMove()) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(lastAnalysis.getFormattedMoveNumber() + "手目 " +
                               lastAnalysis.getPlayerName() + ": " + lastAnalysis.getPlayedMove());
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            if (lastAnalysis.isOutOfTheory()) {
                System.out.println("ℹ️  定石はここまでです");
                System.out.println("   (この局面で100局以上指された手はありません)");
            } else {
                System.out.println("❌ この手は定石から外れています！");
                
                if (lastAnalysis.getRecommendedMove() != null) {
                    System.out.println();
                    System.out.println("💡 推奨手: " + lastAnalysis.getRecommendedMove());
                }
                
                if (lastAnalysis.getTopOpeningMoves() != null && !lastAnalysis.getTopOpeningMoves().isEmpty()) {
                    System.out.println();
                    System.out.println("📚 主要な定石手:");
                    lastAnalysis.getTopOpeningMoves()
                        .forEach(move -> System.out.println(
                            "   " + move.getSan() + " - " + move.getTotalGames() + " 局"
                        ));
                }
            }

            if (lastAnalysis.getPunishmentMove() != null) {
                System.out.println();
                System.out.println("⚔️  相手の最善応手:");
                System.out.println("   " + lastAnalysis.getPunishmentMove());
            }
            System.out.println();
        }

        // まとめ
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("まとめ:");
        System.out.println("  定石に沿った手数: " + theoryMoveCount + " 手");
        
        if (lastAnalysis != null && lastAnalysis.isOutOfTheory()) {
            System.out.println("  結果: 定石の終了地点に到達");
        } else if (lastAnalysis != null && !lastAnalysis.isOpeningMove()) {
            System.out.println("  結果: " + lastAnalysis.getMoveNumber() + "手目で逸脱");
        } else {
            System.out.println("  結果: 定石を完遂");
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 定石手順
        System.out.println();
        System.out.println("📖 定石手順:");
        System.out.println();

        if (theoryLine.length == 0) {
            System.out.println("   定石情報はありません");
            System.out.println();
            return;
        }

        // 最大15手表示
        int maxPly = Math.min(30, theoryLine.length);
        for (int ply = 0; ply < maxPly; ply += 2) {
            int moveNumber = (ply / 2) + 1;
            String whiteMove = theoryLine[ply];
            String blackMove = ply + 1 < maxPly ? theoryLine[ply + 1] : "";

            if (!blackMove.isEmpty()) {
                System.out.println("   " + moveNumber + ". " + whiteMove + " " + blackMove);
            } else {
                System.out.println("   " + moveNumber + ". " + whiteMove);
            }
        }

        if (theoryLine.length < 30) {
            int lastMove = (theoryLine.length + 1) / 2;
            System.out.println();
            System.out.println("   (定石は" + lastMove + "手目までです)");
        }

        System.out.println();
    }
}