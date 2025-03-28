# StellarSync

複数のMinecraftサーバー間でプレイヤーデータを同期するFabricMOD / Paperプラグイン。

## 概要

複数のMinecraftサーバーでインベントリ等をPostgreSQLを介して同期するFabricMOD / Paperプラグインです。
現在、以下のプレイヤーデータを共有できます。

- インベントリ(装備などを含む)
- エンダーチェスト
- 選択スロット

今後、以下のプレイヤーデータを追加する予定です。

- 満腹度(隠し満腹度)
- レシピブック
- 経験値(レベル)
- 実績

## インストール / 要件

詳細は各プラットフォームのREADME.mdを参照してください。

## ビルド

リポジトリをクローンし、`./gradlew build`を実行してください。

## TODO

- ロールバックへの対応
- 同期できるデータの追加

## 参考

本プロジェクトは、[pugur](https://github.com/pugur523)氏の[MySQL Playerdata Sync](https://github.com/pugur523/MySQL_PlayerdataSync)を参考にしました。

## LICENSE

Copyright (c) 2025 MnyaCat
Released under the MIT license
see [LICENSE](./LICENSE)