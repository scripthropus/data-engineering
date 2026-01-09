# チェス序盤力向上アプリケーション - アーキテクチャドキュメント

## 1. 全体の構成

### 1.1 システム概要

本アプリケーションは、Lichessでの対局を分析し、定石から外れた手を検出して改善点を提示するJavaアプリケーションである。レイヤードアーキテクチャを採用し、以下の5層構造で構成されている。

```
┌────────────────────────────────────────┐
│   Presentation Layer (表示層)           │
│   App.java                             │
│   - コマンドライン入出力                  │
│   - 結果の整形・表示                     │
└───────────────┬────────────────────────┘
                │
                ↓
┌────────────────────────────────────────┐
│   Service Layer (サービス層)            │
│   OpeningTrainerService.java           │
│   - 対局分析のビジネスロジック            │
│   - 定石判定                            │
│   - 推奨手の抽出                        │
└─────┬──────────────────┬───────────────┘
      │                  │
      ↓                  ↓
┌─────────────┐    ┌──────────────────┐
│ API Layer   │    │ Utility Layer    │
│ (API層)     │    │ (ユーティリティ層)│
├─────────────┤    ├──────────────────┤
│ Lichess API │    │ PositionTracker  │
│ Explorer API│    │ - 局面追跡        │
│ Engine API  │    │ - FEN/UCI変換    │
└──────┬──────┘    └──────────────────┘
       │
       ↓
┌────────────────────────────────────────┐
│   Model Layer (モデル層)                │
│   - Game, MoveAnalysis                 │
│   - OpeningMove, OpeningResponse       │
│   - EngineResponse                     │
└────────────────────────────────────────┘
```

### 1.2 ディレクトリ構造

```
/home/user/data-engineering/
├── pom.xml                    # Maven設定ファイル
├── run.sh                     # 実行スクリプト
├── .gitignore                 # Git除外設定
├── README.md                  # プロジェクトドキュメント
├── .mvn/                      # Maven Wrapperファイル
└── src/
    ├── main/
    │   └── java/
    │       └── jp/ac/dendai/
    │           ├── App.java                          # メインエントリーポイント
    │           ├── api/                              # 外部APIクライアント層
    │           │   ├── ChessEngineClient.java        # チェスエンジンAPI
    │           │   ├── LichessApiClient.java         # Lichess API
    │           │   └── OpeningExplorerClient.java    # 定石エクスプローラーAPI
    │           ├── model/                            # データモデル層
    │           │   ├── EngineResponse.java           # エンジン応答データ
    │           │   ├── Game.java                     # 対局データ
    │           │   ├── MoveAnalysis.java             # 手の分析結果
    │           │   ├── OpeningMove.java              # 定石手データ
    │           │   └── OpeningResponse.java          # 定石応答データ
    │           ├── service/                          # ビジネスロジック層
    │           │   └── OpeningTrainerService.java    # メイン分析ロジック
    │           └── util/                             # ユーティリティ層
    │               └── PositionTracker.java          # 局面追跡ユーティリティ
    └── test/
        └── java/
            └── jp/ac/dendai/
                └── AppTest.java                      # ユニットテスト
```

### 1.3 技術スタック

- **言語**: Java 17
- **ビルドツール**: Maven
- **主要ライブラリ**:
  - `chesslib 1.3.4`: チェス盤面管理とルール処理
  - `Gson 2.10.1`: JSON シリアライズ/デシリアライズ
  - `JUnit Jupiter 5.11.0`: テストフレームワーク

### 1.4 外部API依存

| API | エンドポイント | 用途 |
|-----|---------------|------|
| Lichess API | `https://lichess.org/api/games/user/{username}` | 対局データ取得 |
| Opening Explorer API | `https://explorer.lichess.ovh/lichess` | 定石統計情報取得 |
| Chess Engine API | `https://chess-api.com/v1` | 最善手評価 |

---

## 2. 主なクラスの役割

### 2.1 Presentation Layer

#### **App.java** (`jp.ac.dendai`)

アプリケーションのエントリーポイントとして、以下の責務を担う。

**主要な責務**:
- コマンドライン引数の解析 (`username`, `playerColor`, `numGames`)
- Lichess APIからの対局データ取得
- プレイヤーの色の自動判定または手動指定
- 分析結果の整形と表示

**重要なメソッド**:

##### `main(String[] args)`

- **引数**: `[username] [color] [num_games]`
- **デフォルト値**: `username="def-e"`, `numGames=1`
- **処理フロー**:
  1. 引数解析
  2. Lichess APIから対局取得
  3. NDJSONレスポンスをGameオブジェクトに変換
  4. プレイヤー色の判定
  5. 対局分析の実行
  6. 結果表示

##### `displayAnalyses(List<MoveAnalysis> analyses, String[] theoryLine)`

分析結果の整形と表示を行う。定石内/定石外の手を区別して表示し、推奨手と罰手（相手の最善応手）、定石手順（最大15手、30ply）を出力する。

**依存関係**:
- `LichessApiClient`: 対局取得
- `OpeningTrainerService`: 分析実行
- `Game`, `MoveAnalysis`: データモデル
- `Gson`: JSON解析

---

### 2.2 Service Layer

#### **OpeningTrainerService.java** (`jp.ac.dendai.service`)

ビジネスロジックの中核を担うサービスクラスである。定石判定、対局分析、理論手順の取得を実装している。

**定数**:
```java
private static final int OPENING_PHASE_LIMIT = 15;  // 分析する手数の上限（15フルムーブ = 30ply）
private static final long MIN_GAMES = 1000;         // 定石と判定する最小対局数
```

**主要なメソッド**:

##### `analyzeGame(String[] moves, String playerColor)`

対局全体を分析し、各手の評価を返す。

**処理フロー**:
1. PositionTrackerで局面を初期化する
2. 各手について以下を実行する（最大15手、30ply）:
   - Opening Explorer APIで現在局面の定石情報を取得する
   - MIN_GAMES（1000）以上の手のみを定石として扱う
   - プレイヤーの手が定石内か判定する:
     - **定石内**: 次の手へ継続する
     - **定石外**: 推奨手を設定し、エンジンで相手の最善応手（罰手）を取得して終了する
     - **定石範囲外**（MIN_GAMES未満）: `outOfTheory`フラグを設定して終了する
3. 分析結果リストを返却する

**重要な設計判断**: 最初の逸脱のみを分析し、それ以降は分析を終了することで、パフォーマンスとユーザー体験を最適化している。

**返り値**: `List<MoveAnalysis>` - 分析結果のリスト

##### `getOpeningTheoryLine(String[] startingMoves)`

定石の理論手順を取得する（最大15手、30ply）。

**処理フロー**:
1. 開始手順を適用する
2. Opening Explorer APIで各局面の最も人気のある手を取得する
3. MIN_GAMES以上の手のみを採用する
4. 30ply（15フルムーブ）まで継続する

**返り値**: SAN形式の定石手順配列

##### `getEngineBestMove(String fen)` (private)

Chess Engine APIを呼び出し、指定されたFEN局面での最善手を取得する。

**パラメータ**:
- `depth=12`: 探索深さ
- `variants=1`: 評価する変化の数

**依存関係**:
- `OpeningExplorerClient`: 定石情報取得
- `ChessEngineClient`: 最善手取得
- `PositionTracker`: 局面追跡
- `OpeningResponse`, `EngineResponse`, `MoveAnalysis`: データモデル
- `Gson`: JSON解析

---

### 2.3 API Layer

#### **LichessApiClient.java** (`jp.ac.dendai.api`)

Lichess APIとの通信を担当するクライアントクラスである。

**主要メソッド**:
```java
public String fetchGames(String username, int max) throws IOException
```

**通信仕様**:
- **URL**: `https://lichess.org/api/games/user/{username}?max={max}&opening=true`
- **メソッド**: GET
- **Accept**: `application/x-ndjson`
- **返り値**: NDJSON形式の文字列

**エラーハンドリング**: HTTPレスポンスコードが200以外の場合はIOExceptionをスローする。

---

#### **OpeningExplorerClient.java** (`jp.ac.dendai.api`)

Lichess Opening Explorer APIとの通信を担当する。

**主要メソッド**:
```java
public String getOpeningMoves(String uciMoves) throws IOException
```

**通信仕様**:
- **URL**: `https://explorer.lichess.ovh/lichess?play={uciMoves}`
- **メソッド**: GET
- **Accept**: `application/json`
- **パラメータ**: UCI形式の手順をカンマ区切りで指定（例: `"e2e4,e7e5"`）
- **返り値**: JSON形式の定石統計情報

---

#### **ChessEngineClient.java** (`jp.ac.dendai.api`)

Chess Engine APIとの通信を担当する。

**主要メソッド**:
```java
public String getBestMove(String fen) throws IOException
```

**通信仕様**:
- **URL**: `https://chess-api.com/v1`
- **メソッド**: POST
- **Content-Type**: `application/json`
- **リクエストボディ**:
  ```json
  {
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "depth": 12,
    "variants": 1
  }
  ```
- **返り値**: JSON形式のエンジン応答

**依存関係**:
- `Gson`: JSONシリアライズ

---

### 2.4 Utility Layer

#### **PositionTracker.java** (`jp.ac.dendai.util`)

chesslib の `Board` クラスをラップし、局面の追跡と形式変換を提供するユーティリティクラスである。

**プロパティ**:
```java
private Board board;  // chesslib のBoardオブジェクト（局面状態）
```

**主要メソッド**:

| メソッド | 説明 | 使用例 |
|---------|------|--------|
| `PositionTracker()` | 初期局面で開始する | - |
| `PositionTracker(String fen)` | 指定FENから開始する | - |
| `applyMoveSan(String sanMove)` | SAN形式の手を局面に適用する | `"e4"`, `"Nf3"` |
| `getFen()` | 現在の局面をFEN形式で取得する | エンジンAPI呼び出し時 |
| `getAllMovesAsUci()` | 全手順をUCI形式（カンマ区切り）で取得する | `"e2e4,e7e5,g1f3"` |
| `clone()` | 現在の局面のコピーを作成する | 分岐分析時 |

**依存関係**:
- `com.github.bhlangonijr.chesslib.Board`: 盤面管理
- `com.github.bhlangonijr.chesslib.move.Move`: 手の表現

---

### 2.5 Model Layer

#### **Game.java** (`jp.ac.dendai.model`)

Lichess APIから取得した対局データを表現するモデルクラスである。

**プロパティ**:
```java
private String id;           // 対局ID
private String moves;        // 全手のSAN形式文字列（スペース区切り）
private Opening opening;     // オープニング情報
private Players players;     // プレイヤー情報
```

**内部クラス階層**:
```
Game
├── Opening
│   └── String name  // 例: "Sicilian Defense: Najdorf"
└── Players
    ├── Player white
    └── Player black
        ├── UserInfo user
        │   ├── String name  // 表示名
        │   └── String id    // ユーザーID
        └── int rating       // レーティング
```

**重要メソッド**:
```java
public String getPlayerColor(String username)
```
- プレイヤー名から色を自動判定する
- 返り値: `"white"`, `"black"`, または `null`

---

#### **MoveAnalysis.java** (`jp.ac.dendai.model`)

一手の分析結果を格納するモデルクラスである。

**プロパティ**:

| プロパティ | 型 | 説明 |
|----------|-----|------|
| `moveNumber` | `int` | 手番（1, 2, 3...） |
| `isWhite` | `boolean` | 白の手かどうか |
| `playedMove` | `String` | 実際に指された手（SAN） |
| `isOpeningMove` | `boolean` | 定石内の手か |
| `isOutOfTheory` | `boolean` | 定石範囲外か（MIN_GAMES未満） |
| `topOpeningMoves` | `List<OpeningMove>` | トップ定石手リスト |
| `recommendedMove` | `String` | 推奨手（定石の最善手） |
| `punishmentMove` | `String` | 相手の最善応手（罰手） |

**ヘルパーメソッド**:
- `getPlayerName()`: `"White"` または `"Black"`を返す
- `getFormattedMoveNumber()`: `"1."` (白) または `"1..."` (黒) の形式で返す

---

#### **OpeningMove.java** (`jp.ac.dendai.model`)

定石の一手を表現するモデルクラスである。

**プロパティ**:
```java
private String uci;          // UCI形式の手（例: "e2e4"）
private String san;          // SAN形式の手（例: "e4"）
private long white;          // 白勝利数
private long draws;          // 引き分け数
private long black;          // 黒勝利数
private int averageRating;   // 平均レーティング
```

**重要メソッド**:
```java
public long getTotalGames()  // 総対局数を計算（white + draws + black）
```

---

#### **OpeningResponse.java** (`jp.ac.dendai.model`)

Opening Explorer APIのレスポンスデータを表現する。

**プロパティ**:
```java
private long white;              // 白勝利数
private long draws;              // 引き分け数
private long black;              // 黒勝利数
private List<OpeningMove> moves; // 可能な手のリスト
```

---

#### **EngineResponse.java** (`jp.ac.dendai.model`)

Chess Engine APIのレスポンスデータを表現する。

**プロパティ**:
```java
private String text;      // テキスト説明
private String from;      // 移動元マス（例: "e2"）
private String to;        // 移動先マス（例: "e4"）
private String san;       // SAN形式の手
private Double eval;      // 評価値（センチポーン）
private Integer depth;    // 探索深さ
private List<String> pv;  // Principal Variation（主要変化）
```

**ヘルパーメソッド**:
- `getBestMoveUci()`: UCI形式の最善手（`from + to`）を返す
- `getEvaluation()`: 評価値（正=白有利、負=黒有利）を返す

---

## 3. クラス間の関係

### 3.1 依存関係図

```
App.java
├── [uses] LichessApiClient
├── [uses] OpeningTrainerService
│   ├── [uses] OpeningExplorerClient
│   ├── [uses] ChessEngineClient
│   ├── [uses] PositionTracker
│   │   └── [wraps] chesslib.Board (外部ライブラリ)
│   ├── [returns] MoveAnalysis
│   ├── [uses] OpeningResponse
│   └── [uses] EngineResponse
├── [uses] Game
│   ├── [contains] Opening (内部クラス)
│   └── [contains] Players (内部クラス)
│       └── [contains] Player (内部クラス)
│           └── [contains] UserInfo (内部クラス)
├── [uses] MoveAnalysis
│   └── [contains] List<OpeningMove>
└── [uses] Gson (外部ライブラリ)
```

### 3.2 アーキテクチャレイヤー構造

各レイヤーの依存方向は単方向であり、上位レイヤーから下位レイヤーへの依存のみが許可される。

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (App.java - コマンドライン表示)          │
└─────────────┬───────────────────────────┘
              │ 依存
              ↓
┌─────────────────────────────────────────┐
│        Service Layer                    │
│  (OpeningTrainerService.java)           │
│  - ビジネスロジック                       │
│  - 定石判定・分析                         │
└───────┬─────────────────┬───────────────┘
        │ 使用             │ 使用
        ↓                 ↓
┌───────────────┐   ┌────────────────────┐
│ API Layer     │   │  Utility Layer     │
│ - Lichess API │   │  - PositionTracker │
│ - Explorer API│   └────────────────────┘
│ - Engine API  │
└───────────────┘
        │ 返却
        ↓
┌─────────────────────────────────────────┐
│          Model Layer                    │
│  - Game, MoveAnalysis                   │
│  - OpeningMove, OpeningResponse         │
│  - EngineResponse                       │
└─────────────────────────────────────────┘
```

### 3.3 組成（Composition）関係

#### Game クラスの内部構造

```
Game
 ├── Opening (内部クラス)
 └── Players (内部クラス)
      ├── Player white (内部クラス)
      └── Player black (内部クラス)
           └── UserInfo (内部クラス)
```

#### OpeningTrainerService の組成

```java
public class OpeningTrainerService {
    private final OpeningExplorerClient explorerClient;
    private final ChessEngineClient engineClient;
    private final Gson gson;

    public OpeningTrainerService() {
        this.explorerClient = new OpeningExplorerClient();
        this.engineClient = new ChessEngineClient();
        this.gson = new Gson();
    }
}
```

`OpeningTrainerService`は3つのオブジェクトを保持し、それらを協調させて分析機能を実現している。

#### PositionTracker の組成

```java
public class PositionTracker {
    private Board board;  // chesslib.Board

    public PositionTracker() {
        this.board = new Board();
    }

    public PositionTracker(String fen) {
        this.board = new Board();
        this.board.loadFromFen(fen);
    }
}
```

`PositionTracker`はchesslibの`Board`クラスをラップし、プロジェクト固有のインターフェース（`getFen()`, `getAllMovesAsUci()`など）を提供している。

### 3.4 データフロー

#### メイン実行フロー

```
1. [App.main()] 開始
   ↓
2. コマンドライン引数解析
   - username (デフォルト: "def-e")
   - playerColor (オプション、自動判定可)
   - numGames (デフォルト: 1)
   ↓
3. [LichessApiClient.fetchGames()]
   → Lichess API呼び出し
   → NDJSON レスポンス受信
   ↓
4. [Gson] JSON → Game オブジェクトへ変換
   ↓
5. [Game.getPlayerColor()] プレイヤー色の自動判定（必要時）
   ↓
6. 対局情報表示（ID、オープニング名、プレイヤー情報）
   ↓
7. [OpeningTrainerService.analyzeGame()]
   → 手の分析開始
   ↓
8. [OpeningTrainerService.getOpeningTheoryLine()]
   → 定石手順取得
   ↓
9. [App.displayAnalyses()]
   → 分析結果の整形と表示
   ↓
10. 終了
```

#### 対局分析フロー（analyzeGame）

```
[OpeningTrainerService.analyzeGame()]
  ↓
1. PositionTracker初期化（開始局面）
  ↓
2. 各手をループ（最大15手、30ply）
   ↓
   2.1 [PositionTracker.getAllMovesAsUci()]
       → 現在までの手順をUCI形式で取得
       ↓
   2.2 [OpeningExplorerClient.getOpeningMoves()]
       → Opening Explorer APIへリクエスト
       → JSON レスポンス受信
       ↓
   2.3 [Gson] JSON → OpeningResponse へ変換
       ↓
   2.4 定石手のフィルタリング（MIN_GAMES >= 1000）
       ↓
   2.5 ケース分岐:
       ├─ 定石範囲外（moves empty）
       │   ├─ MoveAnalysis作成（outOfTheory=true）
       │   ├─ [getEngineBestMove()] 罰手取得
       │   └─ 分析終了（break）
       │
       ├─ 定石内の手（isInTheory=true）
       │   ├─ MoveAnalysis作成（openingMove=true）
       │   ├─ topOpeningMovesを設定
       │   ├─ [PositionTracker.applyMoveSan()] 手を適用
       │   └─ 次の手へ継続
       │
       └─ 定石から逸脱（isInTheory=false）
           ├─ MoveAnalysis作成（openingMove=false）
           ├─ recommendedMoveを設定（topMove）
           ├─ [getEngineBestMove()] 罰手取得
           │    ↓
           │    [ChessEngineClient.getBestMove()]
           │    → Chess Engine API呼び出し
           │    → JSON レスポンス受信
           │    ↓
           │    [Gson] JSON → EngineResponse へ変換
           │    → SAN形式の手を返却
           ├─ [PositionTracker.applyMoveSan()] 手を適用
           └─ 分析終了（break）
  ↓
3. List<MoveAnalysis>を返却
```

#### 定石手順取得フロー（getOpeningTheoryLine）

```
[OpeningTrainerService.getOpeningTheoryLine()]
  ↓
1. PositionTracker初期化
  ↓
2. 開始手順（startingMoves）を適用
   - 各手に対して applyMoveSan() 実行
   - theoryLineリストに追加
  ↓
3. 定石手順を拡張（最大30ply）
   ↓
   while (theoryLine.size() < 30):
     ↓
     3.1 [PositionTracker.getAllMovesAsUci()]
         → 現在の手順を取得
         ↓
     3.2 [OpeningExplorerClient.getOpeningMoves()]
         → Opening Explorer API呼び出し
         ↓
     3.3 [Gson] JSON → OpeningResponse へ変換
         ↓
     3.4 チェック:
         ├─ moves が空 → break（定石終了）
         ├─ topMove.getTotalGames() < MIN_GAMES
         │   → break（十分な対局数なし）
         └─ OK
             ↓
     3.5 topMove（最もプレイされた手）を選択
         ↓
     3.6 theoryLineに追加
         ↓
     3.7 [PositionTracker.applyMoveSan()] 手を適用
         ↓
     3.8 次のループへ
  ↓
4. String[] として返却
```

#### 定石判定のロジック

```java
// OpeningTrainerService.analyzeGame() の核心部分

for (String move : moves) {
    // 1. Opening Explorer APIで定石情報取得
    OpeningResponse response = getOpeningResponse(uciMoves);

    // 2. MIN_GAMES以上の手のみフィルタリング
    List<OpeningMove> theoryMoves = response.getMoves().stream()
        .filter(om -> om.getTotalGames() >= MIN_GAMES)
        .collect(Collectors.toList());

    // 3. 定石範囲外チェック
    if (theoryMoves.isEmpty()) {
        analysis.setOutOfTheory(true);
        analysis.setPunishmentMove(getEngineBestMove(fen));
        break;  // 分析終了
    }

    // 4. 定石内チェック
    boolean isInTheory = theoryMoves.stream()
        .anyMatch(om -> om.getSan().equals(move));

    if (isInTheory) {
        analysis.setOpeningMove(true);
        // 次の手へ継続
    } else {
        // 5. 定石から逸脱
        analysis.setOpeningMove(false);
        analysis.setRecommendedMove(theoryMoves.get(0).getSan());
        analysis.setPunishmentMove(getEngineBestMove(fen));
        break;  // 分析終了
    }
}
```

### 3.5 継承関係

本アプリケーションでは継承は使用されていない。全てのクラスは単純なPOJO（Plain Old Java Object）として実装されている。これはシンプルさと保守性を優先した設計判断である。

---

## 4. 設計パターンと設計原則

### 4.1 適用されている設計パターン

#### レイヤードアーキテクチャ

全体構造をPresentation → Service → API/Util → Modelの層に分離している。各レイヤーは明確に定義された責務を持ち、依存方向は一方向である。

#### ファサードパターン

`OpeningTrainerService`が複数のAPIクライアント（`OpeningExplorerClient`, `ChessEngineClient`）とユーティリティ（`PositionTracker`）を統合し、呼び出し側（`App`）に対してシンプルなインターフェース（`analyzeGame()`, `getOpeningTheoryLine()`）を提供している。

#### DTO (Data Transfer Object) パターン

`model`パッケージの全クラスがDTOとして機能し、JSONレスポンスとアプリケーション内部でのデータ受け渡しに使用される。これらのクラスはビジネスロジックを持たず、純粋なデータコンテナである。

#### ラッパーパターン

`PositionTracker`はchesslibの`Board`クラスをラップし、プロジェクト固有のインターフェース（`getFen()`, `getAllMovesAsUci()`など）を提供している。これにより、外部ライブラリへの依存を局所化している。

#### 早期終了パターン

`analyzeGame()`メソッドは、定石から逸脱した時点で分析を終了する。これによりパフォーマンスを最適化し、ユーザーが最も重要な情報（最初の誤手）に集中できるようにしている。

### 4.2 設計原則

#### 単一責任原則（SRP）

各クラスは明確に定義された単一の責務を持つ：
- `App`: ユーザーインターフェース
- `OpeningTrainerService`: ビジネスロジック
- APIクライアント: 各外部APIとの通信
- `PositionTracker`: 局面管理
- モデルクラス: データ保持

#### 依存性の注入

`OpeningTrainerService`は依存するクライアント（`OpeningExplorerClient`, `ChessEngineClient`）をコンストラクタで初期化している。これによりテスト容易性が向上する。

#### Open/Closed原則

新しい分析機能（例：新しい評価基準、異なる定石データベースのサポート）を既存コードを変更せずに追加可能な構造になっている。

#### インターフェース分離

各APIクライアントは独立したインターフェースを提供し、互いに依存していない。これにより、特定のAPIの変更が他の部分に影響を与えない。

---

## 5. 重要な処理ロジックの詳細

### 5.1 定石判定ロジック

定石と判定される条件は以下の通りである：

1. Opening Explorer APIで`moves`が返される
2. 各`move`の`totalGames >= MIN_GAMES (1000)`

```java
boolean isInTheory = theoryMoves.stream()
    .anyMatch(om -> om.getSan().equals(move));
```

### 5.2 定石範囲外の判定

定石範囲外と判定される条件は以下の通りである：

- MIN_GAMES以上の手が存在しない（`theoryMoves.isEmpty()`）

```java
if (theoryMoves.isEmpty()) {
    // 定石範囲外として処理
    analysis.setOutOfTheory(true);
    analysis.setOpeningMove(false);
    // 分析を終了
    break;
}
```

これは定石から逸脱した場合とは異なる。定石範囲外は「まだ十分に研究されていない領域に入った」ことを意味し、定石外は「定石が存在するがそれを外れた」ことを意味する。

### 5.3 分析終了条件

分析は以下のいずれかで終了する：

1. **定石から逸脱**: プレイヤーが定石外の手を指した
2. **定石範囲外**: MIN_GAMES未満の局面に到達した
3. **手数上限**: OPENING_PHASE_LIMIT（15手、30ply）に到達した

この設計により、最初の誤手に焦点を当てることができる。

---

## 6. API通信パターン

### 6.1 GET リクエスト（Lichess, Opening Explorer）

```
Client (LichessApiClient / OpeningExplorerClient)
  ↓
1. URL構築
  ↓
2. HttpURLConnection作成
  ↓
3. Request設定:
   - setRequestMethod("GET")
   - setRequestProperty("Accept", ...)
  ↓
4. リクエスト送信
  ↓
5. レスポンスコード確認（200以外でIOException）
  ↓
6. BufferedReader でレスポンス読み取り
  ↓
7. String として返却
```

### 6.2 POST リクエスト（Chess Engine）

```
ChessEngineClient
  ↓
1. URL作成
  ↓
2. HttpURLConnection作成
  ↓
3. Request設定:
   - setRequestMethod("POST")
   - setRequestProperty("Content-Type", "application/json")
   - setDoOutput(true)
  ↓
4. リクエストボディ作成:
   - JsonObject構築（Gson使用）
   - {"fen": "...", "depth": 12, "variants": 1}
  ↓
5. OutputStream でJSON送信
  ↓
6. レスポンスコード確認
  ↓
7. BufferedReader でレスポンス読み取り
  ↓
8. String として返却
```

---

## 7. 設定とカスタマイズポイント

### 7.1 定数の意味と調整可能性

```java
// OpeningTrainerService.java
private static final int OPENING_PHASE_LIMIT = 15;  // 分析する手数
private static final long MIN_GAMES = 1000;         // 定石判定の閾値

// ChessEngineClient.java
requestBody.addProperty("depth", 12);  // エンジンの探索深さ
requestBody.addProperty("variants", 1); // 評価する変化の数
```

これらの値を変更することで、アプリケーションの動作を調整できる：

- **OPENING_PHASE_LIMIT**: より長い手順を分析したい場合は増やす
- **MIN_GAMES**: より厳密な定石判定を行いたい場合は増やし、より柔軟にしたい場合は減らす
- **depth**: より正確な評価が必要な場合は増やす（ただし処理時間が増加する）

### 7.2 コマンドライン引数

```bash
java -jar chess-1.0-SNAPSHOT.jar [username] [color] [num_games]
```

- **username**: Lichessユーザー名（デフォルト: "def-e"）
- **color**: "white" または "black"（省略時は自動判定）
- **num_games**: 分析する対局数（デフォルト: 1）

---

## 8. エラーハンドリング戦略

### 8.1 API呼び出しエラー

- HTTPレスポンスコードが200以外でIOExceptionをスローする
- `System.err.println()`で警告表示を行う
- 可能な限り処理を継続する（nullチェックで対応）

### 8.2 JSON解析エラー

- Gsonの例外をキャッチする
- 警告メッセージを表示する
- 処理を継続する

### 8.3 チェスライブラリエラー

- chesslibの例外は伝播させる
- `App.main()`のtry-catchで最終的に捕捉する

現状のエラーハンドリングは最小限であり、本番環境ではより包括的なエラー処理とロギングが必要である。

---

## 9. テスト構造

現状は最小限のテスト構成である：

- **AppTest.java**: ダミーテスト（`assertTrue(true)`）
- **JUnit Jupiter 5.11.0**を使用
- 今後の拡張に備えた構造

今後追加すべきテストカテゴリ：

1. **単体テスト**:
   - `PositionTracker`のロジック
   - `Game.getPlayerColor()`の自動判定
   - `OpeningMove.getTotalGames()`の計算

2. **統合テスト**:
   - `OpeningTrainerService.analyzeGame()`の全体フロー
   - モックAPIを使用したテスト

3. **エンドツーエンドテスト**:
   - 実際のAPIを使用した完全な対局分析フロー

---

## 10. まとめ

### 10.1 アーキテクチャの特徴

本アプリケーションは、以下の特徴を持つ構造化された設計である：

1. **レイヤードアーキテクチャ**: 明確な責任分離により保守性が高い
2. **外部API統合**: 3つの異なるAPIを効率的に利用している
3. **ドメイン駆動**: チェス固有のロジックを適切にモデル化している
4. **拡張性**: 新機能追加が容易な構造である
5. **ユーザビリティ**: 自動判定や早期終了で使いやすさを実現している

### 10.2 主要な改善余地

1. **テストカバレッジ**: より包括的なテストが必要である
2. **エラーハンドリング**: ロバストなエラー処理の強化が必要である
3. **設定ファイル**: 定数をプロパティファイルで管理することが望ましい
4. **ロギング**: SLF4J/Logbackなどのフレームワーク導入が必要である
5. **非同期処理**: 複数API呼び出しの並列化によるパフォーマンス向上が可能である
6. **キャッシング**: Opening Explorer APIのレスポンスをキャッシュすることで効率化できる

### 10.3 設計の強み

- **シンプルさ**: 過度な抽象化を避け、理解しやすい構造である
- **責任の明確化**: 各クラスの役割が明確で、変更の影響範囲が限定的である
- **依存性の管理**: 外部ライブラリへの依存を適切に局所化している
- **実用性重視**: 実際の使用シナリオに基づいた設計判断がなされている

本アーキテクチャは、小規模から中規模のチェス分析ツールとして適切な設計であり、今後の機能拡張にも対応できる基盤を提供している。
