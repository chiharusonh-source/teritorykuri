# 陣取りゲーム ― くりやま盤

Spring Bootを対局サーバーにしたWeb版です。画面とローカル対戦は`index.html`、オンライン対戦の盤面状態・合法手・領地・勝敗はJavaサーバーが管理します。

## 必要環境

- Java 21
- Maven 3.6.3以上

## ローカル起動

```powershell
mvn spring-boot:run
```

起動後に `http://localhost:8080/` をブラウザ2窓で開き、一方で部屋を作り、もう一方から6桁コードで参加します。

## テスト

```powershell
mvn test
```

## 構成

- `index.html`: SVG盤面、操作UI、ローカル対戦、WebSocketクライアント
- `GameEngine.java`: 合法手、壁、閉路、領地、石除去、勝敗
- `RoomService.java`: 6桁招待コードと1対1の部屋管理
- `GameWebSocketHandler.java`: ブラウザとJavaサーバー間の通信
- `Dockerfile`: Java対応ホスティング向けコンテナ

## 公開

この版のオンライン対戦には常時動作するJavaプロセスとWebSocketが必要です。静的ファイルだけを配信するGitHub Pagesや従来のVercel設定では、ローカル対戦のみ動作します。

公開先ではリポジトリの`Dockerfile`をビルドし、外部ポートを環境変数`PORT`へ割り当ててください。Spring Bootが`index.html`も配信するため、フロントとJavaサーバーを同じURLで公開できます。
