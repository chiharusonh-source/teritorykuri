# 陣取りゲーム ― くりやま盤

Spring Bootを対局サーバーにしたWeb版です。画面とローカル対戦は`index.html`、オンライン対戦の盤面状態・合法手・領地・勝敗はJavaサーバーが管理します。

領地判定の詳しい手順と具体例は[`RULES.md`](RULES.md)に記載しています。

## 必要環境

- Java 21
- Maven 3.6.3以上

## ローカル起動

```powershell
mvn spring-boot:run
```

起動後に `http://localhost:8080/` をブラウザ2窓で開き、一方で部屋を作り、もう一方から6桁コードで参加します。

## Dockerでオンライン対戦

Docker Composeを使う場合は、次の1コマンドで画面とWebSocket対局サーバーを一緒に起動できます。

```powershell
docker compose up --build
```

起動後、`http://localhost:8080/`をブラウザ2窓で開いてください。別の端末から接続する場合は、`localhost`をDockerを動かしているPCのIPアドレスに置き換えます。

8080番が使用中なら、ホスト側のポートだけ変更できます。

```powershell
$env:HOST_PORT=8090
docker compose up --build
```

この場合のURLは`http://localhost:8090/`です。HTMLは同じホストの`/ws`へ自動接続するため、公開先がHTTPSならWebSocketも自動的に`wss://`になります。

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
- `compose.yaml`: ローカルまたは1台構成でのDocker起動設定

## 公開

この版のオンライン対戦には常時動作するJavaプロセスとWebSocketが必要です。静的ファイルだけを配信するGitHub Pagesや従来のVercel設定では、ローカル対戦のみ動作します。

公開先ではリポジトリの`Dockerfile`をビルドし、外部ポートを環境変数`PORT`へ割り当ててください。Spring Bootが`index.html`も配信するため、フロントとJavaサーバーを同じURLで公開できます。

部屋情報はJavaプロセスのメモリ内に保持されます。オンライン対戦用コンテナは1レプリカで動かしてください。複数レプリカに増やす場合は、共有ルームストレージとWebSocketのスティッキーセッションが別途必要です。
