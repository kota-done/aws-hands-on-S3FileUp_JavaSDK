# TODO

## 方針

最優先は、**JavaベースのLambda API + S3直接アップロード + DynamoDB状態管理**を最小構成で動かすことです。

## Pending

- `RequestValidator` を新規実装し、`RequestApiHandler` の入力検証を委譲する
- `S3KeyBuilder` を `RequestS3KeyBuilder` へ名称統一し、`RequestService` の参照を更新する
- `POST /requests` のリクエスト/レスポンス項目を詳細化する
- `GET /requests/{id}` のレスポンス項目を詳細化する
- `GET /requests` の一覧レスポンス項目を詳細化する
- `GET /requests` の一覧取得は初期版では `Scan` とし、後続で `Query / GSI / paging` 最適化の要否を見直す
- DynamoDBテーブル名と属性定義を具体化する
- 初期版のAPI利用者制限の方法を決める
- 承認機能追加時の拡張方針を整理する
- GUI手順を実際のAWSコンソール操作レベルまで詳細化する
- GUI確認後にCLIまたはIaCへ展開する手順を整理する
- 動作確認手順を整理する
- Mavenビルド環境を用意し、ローカルビルド確認を行う

## In Progress

- なし

## Completed

- DeepSeekの調査内容を `PLAN.md` に整理した
- ユーザーとの合意内容を `SPEC.md` に整理した
- 初期実装は「最小構成で動くもの」を優先する方針に決めた
- ChatGPTとの相談内容を反映し、業務システム寄りの最小構成案を明文化した
- 初期版はユーザー限定利用、後続で承認機能追加に合わせて公開範囲を広げる方針を反映した
- 要件定義の叩き台を `SPEC.md` に追記した
- API一覧とGUI先行の実施方針を `SPEC.md` に追記した
- APIの基本入出力とステータスコードを `SPEC.md` に整理した
- データ設計として `requestId` `userId` `fileName` `s3Key` `status` `createdAt` `updatedAt` を採用した
- DynamoDB主キーを `requestId`、S3キー形式を `uploads/{userId}/{requestId}/{fileName}` とする方針を確定した
- 初期版のAWS構成図と処理フローを `SPEC.md` に整理した
- Java 21 + AWS SDK for Java v2 + Jackson を前提としたクラス設計方針を `SPEC.md` に整理した
- 初期版ではフレームワークを使わず、Handler / Service / Repository / DTO / Model 構成で進める方針を確定した
- AWS公式ハンズオン前提のGUI実装手順と確認ポイントを `SPEC.md` に整理した
- DynamoDBテーブル具体値とLambda環境変数を `SPEC.md` に整理した
- MavenプロジェクトとJava Lambdaの最小雛形を追加した
- 最小構成の `template.yaml` を追加し、SAMでのデプロイ構成を整理した
- 生成済みJavaソースを起点に、設計へ戻すための整理方針を固めた
- `DESIGN_RULES.md` を追加し、命名規則・例外方針・コメント方針・レイヤ責務方針を分離した
- システム構成図とI/O整理表を `SPEC.md` と `処理フロー.drawio` に反映した
- エラーレスポンス設計とCloudWatch Logs前提のログ方針を `SPEC.md` に整理した
- 実装着手前の変更対象クラス整理（必須/影響のみ/保留）を `SPEC.md` に反映した

## Waiting For User

- 最終成果物として、コード以外にどこまで用意するかの優先度整理
  - 候補: 構成図、実装解説資料、面談用説明メモ
- ローカルでの `mvn` 実行環境確認

## メモ

- 現時点では、一般公開向けの認証・認可、非同期バッチ本体、通知機能、フロント画面、複雑な検索条件はスコープ外
- ステータスは `RECEIVED` `PROCESSING` `COMPLETED` `FAILED` を基本とする
- 初期版の `POST /requests` は `userId` と `fileName` を受け取る
- この環境では `mvn` コマンド未導入のため、ローカルビルド未検証
- 次の作業順は、要件定義 → API一覧 → データ設計 → AWS構成図 → Javaクラス設計 → GUI実装 → 検証 → CLIまたはIaC化
