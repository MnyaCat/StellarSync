# StellarSync

複数のMinecraftサーバ間でインベントリ等を同期するFabric MOD/Paper プラグイン。

## 概要

複数のMinecraftサーバの間で以下のプレイヤーデータを同期できます。また、これらはサーバごとに同期するかを設定できます。

- インベントリ(装備などを含む)
- エンダーチェスト
- 選択スロット

## インストール

詳細は各プラットフォームのREADME.mdを参照してください。

### 要件

StellarSyncの利用には、MOD/プラグインに加えPostgreSQLが必要です。初回起動後、設定ファイルが生成されるのでPostgreSQLの接続情報を追加してください。

#### example

```yaml
database:
  jdbc-url: jdbc:postgresql://localhost:5432/stellar_sync
  username: stellar_sync
  password: password
```

### 設定

全てのプラットフォーム共通で以下の設定ファイルを使用します。

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
debug-mode: false # trueにするとdebugログが有効になります
```

設定ファイルのパスは以下の通りです。

- Fabric: `./config/StellarSync.yaml`
- Paper: `./plugins/StellarSync/StellarSync.yaml`

## TODO

- ロールバックへの対応
- 同期できるデータの追加

## LICENSE

Copyright (c) 2025 MnyaCat
Released under the MIT license
see [LICENSE](./LICENSE)