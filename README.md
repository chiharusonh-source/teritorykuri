# 陣取りゲーム ― くりやま盤

このフォルダは、そのまま静的Webサイトとして公開できます。

## ファイル

- `index.html`: ゲーム本体です。画像とJavaScriptを埋め込んだ単体HTMLなので、追加アセットなしで動きます。
- `.nojekyll`: GitHub Pagesで余計な処理を避けるための空ファイルです。
- `netlify.toml`: Netlifyでこのフォルダを公開するための最小設定です。

## おすすめ公開方法

1. Netlify Drop
   - `territory-game-site` フォルダを Netlify Drop にドラッグ&ドロップします。

2. GitHub Pages
   - このフォルダの中身をリポジトリに置き、Pages の公開元にします。
   - 公開トップは `index.html` です。

3. Vercel
   - Framework Preset は `Other` を選びます。
   - Build Command は空欄、Output Directory は `.` で公開できます。

## ローカル確認

ブラウザで `index.html` を直接開いて動作確認できます。
