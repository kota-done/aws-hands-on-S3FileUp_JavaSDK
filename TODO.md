# TODO

## 方針

最優先は、**JavaベースのLambda API + S3直接アップロード + DynamoDB状態管理**を、  
ローカル（SAM + LocalStack）とAWS実環境の両方で再現可能な形に維持することです。

このフェーズは、**ステータス更新の確認**と**AWS本番環境での起動確認**までで一区切りとする。  
クライアント実装は、成果物を分割して次フェーズで実施する。

## Pending

- `GET /requests` の一覧取得は初期版では `Scan` とし、後続で `Query / GSI / paging` 最適化の要否を見直す
- SAMローカル検証で `S3_ENDPOINT` の公式推奨設定（`s3.localhost.localstack.cloud`）を反映し、`POST -> PUT -> GET` を再確認する
- `SAM_LOCAL_TEST_RUNBOOK.md` に公式リファレンス確認手順と、`NoSuchBucket` 系エラーの切り分けを最終反映する
- S3 PUT後に `status=COMPLETED` へ反映されるまでの確認手順（再取得間隔/タイムアウト目安）を明文化する
- 必要に応じて、SAMローカル手動検証を半自動化するスクリプト化を検討する

## In Progress

- SAM + LocalStack のローカルE2E検証手順を安定化中（Presigned URLのHost/Path解釈差異の是正）

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
- `RequestValidator` 実装と `RequestApiHandler` からの入力検証委譲を完了した
- `S3KeyBuilder` から `RequestS3KeyBuilder` への命名統一を完了した
- CIを `mvn verify` 1ジョブ + strict保護で運用する方針を確定し、ワークフローを追加した
- `mvn verify` のローカル実行を確認した
- API Gateway + Lambda + DynamoDB + S3 のAWS実環境スモーク（POST/GET/GET）を確認した
- SAMテンプレートでAPI 3ルートと関連Lambda/EventBridge構成を再現した
- S3 Object Createdを契機に `status=COMPLETED` を更新する `RequestStatusUpdateHandler` を実装した
- テストメソッドコメント方針とJavadoc方針を設計ルールへ反映し、既存テストへ適用した
- 設計/記事/ナレッジへ「採用技術は公式リファレンス確認後に採用する」ルールを追記した

## Waiting For User

- SAMローカル検証の次段で、どこまで自動化（手動維持 / 半自動 / CI組み込み）するかの優先度判断
- 次フェーズ（クライアント実装 / AWS拡張 / テスト強化）の優先順位確定

## メモ

- 現時点では、一般公開向けの認証・認可、非同期バッチ本体、通知機能、フロント画面、複雑な検索条件はスコープ外
- ステータスは `RECEIVED` `PROCESSING` `COMPLETED` `FAILED` を基本とする
- 初期版の `POST /requests` は `userId` と `fileName` を受け取る
- `SESSION_ARTICLE.md` と `.codex/knowledge.md` に、S3 LocalStack設定での失敗事例と再発防止（公式確認必須）を記録済み
- 公式リファレンス未確認の設定値は、採用せず保留する
