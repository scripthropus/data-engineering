### 3.2 システムが利用するリソース

リソース1: Lichess Games API
名称: Lichess Games Export API
概要: ユーザーの対局データをエクスポートするAPI
利用目的: 対局データの取得
URL: https://lichess.org/api/games/user/{username}
形式: Web API (NDJSON形式)
リクエストパラメータ:
- username: Lichessユーザー名（パスパラメータ）
- opeing: オープニング情報を含める

レスポンス構造:
- 形式: NDJSON (Newline Delimited JSON)
- 各行が1つのゲームオブジェクトを表すJSON
- 主要フィールド:
  - id: ゲームID
  - players: プレイヤー情報（white/black、user、rating）
  - moves: 指し手の列（SAN形式、スペース区切り）
  - opening: オープニング情報（eco、name、ply）
  - clock: 持ち時間設定
  - status: ゲーム終了状態
  - winner: 勝者の色
```JSON
{"id":"tVyiD7v5","rated":true,"variant":"standard","speed":"blitz","perf":"blitz","createdAt":1766986122983,"lastMoveAt":1766986736483,"status":"resign","source":"pool","players":{"white":{"user":{"name":"icedmilktea","id":"icedmilktea"},"rating":1427,"ratingDiff":4},"black":{"user":{"name":"def-e","id":"def-e"},"rating":1378,"ratingDiff":-14}},"winner":"white","opening":{"eco":"D02","name":"Queen's Pawn Game: Symmetrical Variation, Pseudo-Catalan","ply":5},"moves":"d4 d5 Nf3 Nf6 g3 e6 Bg2 Bd6 O-O O-O c4 dxc4 Nc3 a6 e4 Bb4 Bg5 h6 Bxf6 Qxf6 e5 Qd8 Nd2 Qxd4 Rc1 Nc6 Nf3 Qxd1 Rfxd1 Bxc3 Rxc3 Na5 a4 b5 axb5 axb5 Ra3 b4 Ra4 Bd7 Rxd7 c3 Rxb4 c2 Rxc7 Nc4 Rbxc4 Ra1+ Bf1 Rd8 Rxc2 Rdd1 Nd2","clock":{"initial":300,"increment":3,"totalTime":420}}
```

リソース2: Opening Explorer API
名称: Lichess Opening Explorer API
概要: チェス定石データベース．局面ごとの手の統計を提供
利用目的: 各局面での定石手とゲーム数の取得
URL: https://explorer.lichess.ovh/lichess
形式: Web API (JSON形式)

リクエストパラメータ:
- variant: ゲームバリアント（デフォルト: standard）
- speeds: タイムコントロール（カンマ区切り、例: blitz,rapid,classical）
- ratings: レーティング範囲（カンマ区切り、例: 1600,1800,2000,2200,2500）
- play: 現在の局面までの手順（UCI形式、カンマ区切り、例: e2e4,e7e5）

レスポンス構造:
- 形式: JSON
- 主要フィールド:
  - white: 白勝利のゲーム数
  - draws: 引き分けのゲーム数
  - black: 黒勝利のゲーム数
  - moves: この局面から可能な手の配列
    - uci: UCI形式の手
    - san: SAN形式の手
    - averageRating: この手が指された対局の平均レーティング
    - white/draws/black: この手における白勝利/引き分け/黒勝利のゲーム数
    - opening: オープニング情報（eco、name）
  - recentGames: 最近の対局のサンプル
  - opening: 現在の局面のオープニング情報

```JSON
{"white":250702783,"draws":21910923,"black":220445454,"moves":[{"uci":"g1f3","san":"Nf3","averageRating":1836,"white":161214378,"draws":14787883,"black":141595436,"game":null,"opening":{"eco":"C40","name":"King's Knight Opening"}},{"uci":"f1c4","san":"Bc4","averageRating":1789,"white":24827664,"draws":2077492,"black":22392779,"game":null,"opening":{"eco":"C23","name":"Bishop's Opening"}},{"uci":"f2f4","san":"f4","averageRating":1840,"white":23213554,"draws":1602798,"black":20085641,"game":null,"opening":{"eco":"C30","name":"King's Gambit"}},{"uci":"d2d4","san":"d4","averageRating":1837,"white":15358140,"draws":1122647,"black":12423983,"game":null,"opening":{"eco":"C20","name":"Center Game"}},{"uci":"b1c3","san":"Nc3","averageRating":1842,"white":14410523,"draws":1236777,"black":12273113,"game":null,"opening":{"eco":"C25","name":"Vienna Game"}},{"uci":"d2d3","san":"d3","averageRating":1761,"white":4189072,"draws":408439,"black":4274470,"game":null,"opening":{"eco":"C20","name":"King's Pawn Game: Leonardis Variation"}},{"uci":"d1h5","san":"Qh5","averageRating":1743,"white":1729664,"draws":156850,"black":1550308,"game":null,"opening":{"eco":"C20","name":"King's Pawn Game: Wayward Queen Attack"}},{"uci":"c2c3","san":"c3","averageRating":1788,"white":1574481,"draws":129747,"black":1518942,"game":null,"opening":{"eco":"C20","name":"King's Pawn Game: MacLeod Attack"}},{"uci":"d1f3","san":"Qf3","averageRating":1722,"white":971065,"draws":87905,"black":959904,"game":null,"opening":{"eco":"C20","name":"King's Pawn Game: Napoleon Attack"}},{"uci":"c2c4","san":"c4","averageRating":1815,"white":833206,"draws":84003,"black":787154,"game":null,"opening":{"eco":"C20","name":"English Opening: The Whale"}},{"uci":"g2g3","san":"g3","averageRating":1806,"white":570977,"draws":52103,"black":549309,"game":null,"opening":null},{"uci":"b2b3","san":"b3","averageRating":1766,"white":347361,"draws":30938,"black":392430,"game":null,"opening":{"eco":"C20","name":"King's Pawn Opening"}}],"recentGames":[{"uci":"g1f3","id":"pQNSVFp5","winner":null,"speed":"rapid","mode":"rated","black":{"name":"Para85","rating":1912},"white":{"name":"gg264","rating":1927},"year":2025,"month":"2025-11"},{"uci":"f1c4","id":"ijErMjCS","winner":"white","speed":"blitz","mode":"rated","black":{"name":"NurikTurganbek","rating":1999},"white":{"name":"Yarlaxle","rating":1972},"year":2025,"month":"2025-11"},{"uci":"g1f3","id":"tcSaFYFc","winner":"black","speed":"blitz","mode":"rated","black":{"name":"lunamarta","rating":1963},"white":{"name":"Jovudza","rating":1916},"year":2025,"month":"2025-11"},{"uci":"g1f3","id":"BqwxwJ0t","winner":"white","speed":"classical","mode":"rated","black":{"name":"rezav","rating":1708},"white":{"name":"goscpiotrek","rating":1830},"year":2025,"month":"2025-11"}],"topGames":[],"opening":{"eco":"C20","name":"King's Pawn Game"}}
```

リソース3: Chess Engine API
名称: Chess-API.com
概要: チェスエンジンによる局面評価・最善手計算
利用目的: 定石外れ後の相手の最善手を取得（咎められてしまう手の計算）
URL: https://chess-api.com/v1
形式: Web API (JSON形式)
リクエストパラメータ:
- fen: 局面のFEN表記（必須）
- depth: 探索深度（最大18、デフォルト12）
- variants: 評価する変化手数（最大5、デフォルト1）
- maxThinkingTime: 最大思考時間（最大100ms、デフォルト50ms）

レスポンス構造:
- 形式: JSON
- 主要フィールド:
  - type: レスポンスタイプ（move / bestmove / info）
  - eval: 評価値（負の値は黒有利）
  - move: 最善手（UCI形式）
  - san: 最善手（SAN形式）
  - depth: 探索深度
  - continuationArr: 推奨される続きの手順
  - mate: 詰みまでの手数（nullまたは整数）
  - winChance: 勝率（0-100%）
  - text: 評価の説明文
```JSON
{
  "text": "Move e2 → e4 (e4): [0.29]. The game is balanced. Depth 12.",
  "captured": false,
  "promotion": false,
  "isCapture": false,
  "isPromotion": false,
  "isCastling": false,
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  "type": "bestmove",
  "depth": 12,
  "move": "e2e4",
  "eval": 0.29,
  "centipawns": "29",
  "mate": null,
  "continuationArr": [
    "e7e5",
    "g1f3",
    "b8c6",
    "d2d4",
    "e5d4",
    "f3d4",
    "g8f6",
    "d4c6",
    "b7c6",
    "f1d3",
    "d7d5",
    "e4e5",
    "f6d7"
  ],
  "debug": "info depth 12 seldepth 26 multipv 1 score cp 29 nodes 334092 nps 30372000 hashfull 16 tbhits 0 time 11 pv e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6 d4c6 b7c6 f1d3 d7d5 e4e5 f6d7",
  "winChance": 52.66697440308632,
  "taskId": "yo6lt5tc1",
  "turn": "w",
  "color": "w",
  "piece": "p",
  "from": "e2",
  "to": "e4",
  "san": "e4",
  "flags": "b",
  "lan": "e2e4",
  "fromNumeric": "52",
  "toNumeric": "54",
  "continuation": [
    {
      "from": "e7",
      "to": "e5",
      "fromNumeric": "57",
      "toNumeric": "55"
    },
    {
      "from": "g1",
      "to": "f3",
      "fromNumeric": "71",
      "toNumeric": "63"
    }
  ]
}
```

### 3.3 プログラムの構成
#### 3.3.1 全体構成
本プログラムは単一のJavaアプリケーションとして構成され，以下の3層構造を持つ：

APIクライアント層: 外部APIとの通信を担当
サービス層: ロジック（定石分析）を実装
表示層: 分析結果の表示を担当
[App.java] (メインクラス・表示)
    ↓
[OpeningTrainerService] (分析ロジック)
    ↓                ↓                    ↓
[LichessApiClient] [OpeningExplorerClient] [ChessEngineClient]

#### 3.3.2 主要クラスとその役割
APIクライアント層:
- LichessApiClient: Lichess APIから対局データを取得
- OpeningExplorerClient: Opening Explorer APIから定石データを取得
- ChessEngineClient: Chess Engine APIから最善手を取得

サービス層:
- OpeningTrainerService: 定石分析のメインロジック
analyzeGame(): 対局を分析し，各手が定石内か判定
getOpeningTheoryLine(): 定石手順を15手まで取得

ユーティリティ層:
- PositionTracker: チェス盤の状態管理
手の適用，FEN取得，UCI変換などを担当
データモデル層:

Game: 対局データのモデル
MoveAnalysis: 各手の分析結果を格納
OpeningResponse, OpeningMove: Opening APIレスポンスのモデル
EngineResponse: Engine APIレスポンスのモデル
メイン層:

App: エントリーポイント．結果表示を担当
3.3.3 クラス図
[TODO: 以下のようなクラス図を描く]

┌─────────────────┐
│      App        │
│  + main()       │
└────────┬────────┘
         │
         ↓
┌─────────────────────────┐
│ OpeningTrainerService   │
│ - explorerClient        │
│ - engineClient          │
│ + analyzeGame()         │
│ + getOpeningTheoryLine()│
└──────┬──────────────────┘
       │
       ├─→ LichessApiClient
       ├─→ OpeningExplorerClient
       ├─→ ChessEngineClient
       └─→ PositionTracker

3.3.4 外部ライブラリ
Gson (com.google.code.gson): JSON解析に使用
chesslib (com.github.bhlangonijr.chesslib): チェスボードの状態管理，手の適用・検証に使用

### 3.4 データ構造とアルゴリズム
#### 3.4.1 データ構造
1. MoveAnalysis（手の分析結果）
```java
class MoveAnalysis {
    int moveNumber;              // 手番
    boolean isWhite;             // 白番か
    String playedMove;           // 実際に指された手
    boolean isOpeningMove;       // 定石内か
    boolean isOutOfTheory;       // 定石範囲外か
    List<OpeningMove> topOpeningMoves;  // 定石手のリスト
    String recommendedMove;      // 推奨手
    String punishmentMove;       // 罰手
}
```

2. PositionTracker（局面管理）

内部にBoardオブジェクト（chesslibライブラリ）を保持
手の履歴をスタック構造で管理
FEN・UCI変換機能を提供
3. OpeningMove（定石手の統計）
```java
class OpeningMove {
    String san;        // SAN表記
    String uci;        // UCI表記
    long white;        // 白勝利数
    long draws;        // 引き分け数
    long black;        // 黒勝利数
    int averageRating; // 平均レーティング
    
    long getTotalGames() {
        return white + draws + black;
    }
}
```

使用したデータ型の選択理由:

long型: Opening Explorerのゲーム数は21億を超えるため，intではオーバーフローする
List<OpeningMove>: 定石手は複数あり，順序が重要（人気順）なためリストを使用
booleanフラグ: 定石内/範囲外の状態を明確に区別
3.4.2 アルゴリズム
1. 定石判定アルゴリズム

定石判定(局面, 指された手):
    1. Opening Explorer APIから局面の定石データを取得
    2. 100ゲーム以上の手のみをフィルタリング
    3. フィルタ結果が空の場合:
        → 定石範囲外と判定
        → 解析終了
    4. フィルタ結果に指された手が含まれる場合:
        → 定石内と判定
        → 次の手へ
    5. フィルタ結果に指された手が含まれない場合:
        → 定石外れと判定
        → 推奨手 = フィルタ結果の1番目（最多ゲーム数）
        → 解析終了

計算量: O(n × m)

n: 分析する手数（最大15手）
m: 各局面の定石手候補数（通常5-10手程度）
#### 2 定石手順取得アルゴリズム

定石手順取得(開始手順):
    1. 開始手順を適用（実際に指された定石内の手）
    2. 定石リストに開始手順を追加
    3. 30ply（15手）に達するまで繰り返し:
        a. 現在局面の定石データを取得
        b. 100ゲーム以上の手をフィルタ
        c. フィルタ結果が空なら終了
        d. 最多ゲーム数の手を選択
        e. 手を適用し，定石リストに追加
    4. 定石リストを返す

特徴:

貪欲法: 各局面で最多ゲーム数の手を選択
実際の対局との整合性: 開始手順を使うことで，対局と定石の整合性を取っている．
3.4.3 定石判定の閾値
定石として認める基準を100ゲーム以上と設定した理由：

指された手が100回未満の手はおそらく定石外の手であるため．
定石であったら全世界のプレイヤーがその定石を知らず，
データベースに100回も指されたことが記録されないなんてことはないだろうと考えたため．
100回以上指される局面は頻繁に現れる局面だと考え，頻繫現れる局面は
定石と考えてよいだろうと判断したため．


### 3.5 その他の工夫点
#### 3.5.1 数値オーバーフロー対策
Opening Explorer APIのゲーム数は最大35億を超える場合があり，Java のint型（最大21億）では表現できない．
そのためlong型（最大922京）を使用した．

#### 3.5.2 プレイヤーカラー自動検出
ユーザーが白番・黒番のどちらで対局したかを自動判別することで，
ユーザビリティを向上させた．

#### 3.5.3 定石外れと定石範囲外の区別
定石外れ: ユーザーのミス → 学習すべき箇所
定石範囲外: 定石が尽きた → ミスではない
この区別により，ユーザーは自分の責任範囲を明確に理解できる．

#### 3.5.4 Top Opening Movesのフィルタリング
100ゲーム以上の手のみを表示（最大3個）することで，信頼性の高い情報のみを提供．

4. 実験
4.1 実験条件
実行環境:
Java: 17
Maven: [TODO: バージョン]
Gson: 2.10.1
chesslib: 1.3.3

ユーザー名: def-e
対局数: 1（最新対局）
対局形式: ブリッツ/ラピッド/クラシカル（全て含む）
分析範囲: 開始から15手（30ply）

4.2 実験結果
4.2.1 成功例：定石範囲外の検出
```bash
=== Chess Opening Trainer ===
Fetching games for user: def-e

Game ID: tVyiD7v5
Opening: Queen's Pawn Game: Symmetrical Variation

White: icedmilktea (1427)
Black: def-e (1378)

Analyzing black moves for def-e

=== Opening Analysis Results ===

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Move 9... Black: h6
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ℹ️  Opening theory ends here
   (No moves with 100+ games in this position)

⚔️ Opponent's best response to your move:
   Bh4

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Summary:
  Opening followed: 8 moves
  Result: Reached end of theory
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📖 Opening Sequence (Theory):

   1. d4 d5
   2. Nf3 Nf6
   3. g3 e6
   4. Bg2 Bd6
   5. O-O O-O
   6. c4 dxc4
   7. Nc3 a6
   8. e4 Bb4

   (定石は8手目までです)
```
結果の解釈:

8手目まで定石通りに指している
9手目で定石が尽きた（100ゲーム以上の手が存在しない）
これはユーザーのミスではなく，定石の終わり
相手の最善応手（Bh4）は参考情報として表示


5. 考察
5.1 機能
5.1.1 実現した機能の評価
本プログラムは当初の目的である「定石外れの検出と学習支援」を達成できた．特に以下の点で有効である：

長所:



短所:
定石外れの深刻度（どれだけ悪手か）が示されない

5.1.2 改良案
深刻度の評価: エンジン評価値を使い小さなミスと大きなミスを区別
定石の傾向分析: 複数の対局を分析し，よく外れる箇所を統計的に提示
定石の幅を表示: 2番手，3番手の定石手も提示し，定石の柔軟性を示す

5.2 実現方法
5.2.1 プログラム構造の評価
3層構造（APIクライアント / サービス / 表示）の採用は適切だった．責任の分離により，各層の変更が他層に影響しにくくなっている．

良かった点:

API変更に対する耐性（クライアント層のみ修正で対応可能）
テストのしやすさ（各層を独立してテスト可能）
可読性の向上
改善点:

App.javaが表示ロジックを多く含み肥大化している
→ 専用のViewクラスを作成すべき
エラーハンドリングが不十分
→ API失敗時のリトライ機構，ユーザーへのエラーメッセージ改善
5.2.2 データ構造とアルゴリズムの評価
データ構造:

List<MoveAnalysis>の使用は適切．順序が重要で，サイズが小さい（最大15要素）
long型の採用により数値オーバーフロー問題を回避
MoveAnalysisクラスが定石外れ/範囲外をbooleanフラグで区別しているのは明快
アルゴリズム:

定石判定の貪欲法（最多ゲーム数を選択）は簡潔で効率的
計算量O(n×m)は実用上問題なし（n≤15, m≤10程度）
効率面の改善案:

API呼び出しのキャッシング: 同じ局面への複数回の問い合わせを避ける
並列処理: 定石判定とエンジン評価を並行実行
バッチ処理: 複数対局を一度に分析
5.2.3 リソース選択の評価
Lichess API:

無料で制限が緩い（適切）
公開対局のみという制約はあるが，学習目的では問題なし
Opening Explorer API:

マスターゲーム・Lichessゲーム両方のデータベースを持つ（適切）
ゲーム数のフィルタリングが可能（適切）
Chess Engine API:

5.3 その他
5.3.1 ユーザビリティ
コマンドライン実行は手軽だが，一般ユーザーには敷居が高い
GUI版やWeb版があれば利便性が向上する
5.3.2 拡張性
現在の設計は拡張しやすい構造になっている：
新しい分析機能の追加が容易（サービス層にメソッド追加）

6. おわりに
本プログラムは，Lichess対局から定石外れを自動検出し，学習を支援するシステムとして，当初の目的を達成できた．
特に「定石外れ」と「定石範囲外」を区別することで，ユーザーは自分の責任範囲を明確に理解でき，効果的な学習が可能となった．

今後の展開として，以下が考えられる：

Web UI化によるアクセス性の向上
複数対局の一括分析・統計機能
定石の傾向分析とレコメンデーション
ユーザー間での定石学習の共有機能
チェスの上達には定石学習が不可欠であり，本システムがその一助となることを期待する．

感想

とても大変だった．
実際に自分の対局を分析してみて気づいたこと

など
参考文献
Lichess API Documentation
URL: https://lichess.org/api
概要: Lichess APIの公式ドキュメント．対局データ取得APIの仕様について記載．

Lichess Opening Explorer API
URL: https://github.com/lichess-org/lila-openingexplorer
概要: Opening Explorer APIのGitHubリポジトリ．定石データベースの構造とAPI仕様について説明．

Standard Algebraic Notation (SAN)
URL: https://en.wikipedia.org/wiki/Algebraic_notation_(chess)
概要: チェスの標準表記法についての解説．本プログラムで手の表記に使用．

Forsyth-Edwards Notation (FEN)
URL: https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
概要: チェス局面を文字列で表現する形式の解説．

Gson User Guide
URL: https://github.com/google/gson/blob/master/UserGuide.md
概要: Gsonライブラリのユーザーガイド．JSON解析の実装方法について記載．

chesslib - Java Chess Library
URL: https://github.com/bhlangonijr/chesslib
概要: Javaでチェスのロジックを扱うためのライブラリ．盤面管理と手の検証に使用．

Chess Engine APIのドキュメント
URL:https://chess-api.com/