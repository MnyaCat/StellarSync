# StellarSync

複数のMinecraftサーバー間でプレイヤーデータを同期するFabricMOD。

## 概要

複数のMinecraftサーバーでインベントリ等をPostgreSQLを介して同期するFabricMODです。
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

このMODの利用にはPostgreSQLが必要です。予めセットアップしてください。

1. サーバーの`mods`フォルダに`StellarSync-Fabric-<version>.jar`を配置
2. 一度サーバーを起動し、設定ファイルを生成する(`config/StellarSync.yaml`に生成されます)
3. サーバーを停止し、設定ファイルにPostgreSQLの接続情報を書き込む
4. サーバーを起動する

### Example

```yaml
database:
  jdbc-url: jdbc:postgresql://localhost:5432/stellar_sync
  username: stellar_sync
  password: password
```

## 設定

パスは`config/StellarSync.yaml`です。

```yaml
config-version: '1.0' # メタ情報 変更しないでください
database: # データベースの接続情報
  jdbc-url: jdbc:postgresql://<host>:<port>/<database>
  username: username
  password: password
  maximum-pool-size: 5 # コネクションプールの最大数 デフォルト推奨
sync-options: # 同期する項目の設定
  inventory: true
  ender-chest: true
  selected-slot: true
debug-mode: false # debugログを有効にするか
```

## 参考

本プロジェクトは、[pugur](https://github.com/pugur523)氏の[MySQL Playerdata Sync for Fabric](https://github.com/pugur523/MySQL_PlayerdataSync-4-Fabric)を参考にしました。

## LICENSE

Copyright (c) 2025 MnyaCat
Released under the MIT license
see [LICENSE](./LICENSE)