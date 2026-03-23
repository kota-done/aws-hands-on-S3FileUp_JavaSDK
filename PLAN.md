# PLAN

## 結論

AWS案件参画に向けたアピール用ハンズオンとしては、**JavaベースのLambda API + S3直接アップロード + DynamoDB状態管理**を題材にした構成が適しています。  
ただし、**Javaで REST API + Presigned URL発行 + DynamoDB状態管理 を一気通貫で扱う単一のAWS公式ハンズオンは未発見**のため、公式サンプルを組み合わせてハンズオン化する前提で進めるのが現実的です。

現時点の有力方針は、以下の3つを主軸に組み合わせることです。

- `aws-samples/aws-sam-java-rest`
- `aws-samples/generate-s3-accelerate-presigned-url`
- `aws-samples/bobs-used-bookstore-serverless`

## 背景

- ユーザーはAWS案件参画に向けて、技術力を示せるハンズオン題材を整理したい
- すでにChatGPTおよびDeepSeekを用いて、AWS公式ハンズオン、公式サンプル、公式ブログ、公式ドキュメントの調査を実施済み
- 調査対象は、**ファイル受付API（POST /requests, GET /requests/{id}, GET /requests）** を **API Gateway + Lambda（Java） + S3 + DynamoDB** で実装するための参考資料

## 目的

- AWS案件で説明しやすい、実務に近いサーバーレス構成のハンズオンテーマを定義する
- 公式情報を根拠に、実装対象と参照先を明確化する
- 今後の `RESEARCH.md`、`SPEC.md`、`TODO.md` に繋がる初期方針を固める

## 想定ハンズオンテーマ

**テーマ案**  
Java製Lambdaを用いたファイル受付APIを構築し、ファイル本体はS3へ直接アップロード、受付状態はDynamoDBで管理する。

**想定API**

- `POST /requests`
- `GET /requests/{id}`
- `GET /requests`

**想定状態遷移**

- `CREATED`
- `UPLOAD_URL_ISSUED`
- `UPLOADED`
- `PROCESSING`
- `DONE`
- `FAILED`

## 提案アーキテクチャ

```mermaid
flowchart LR
  Client[Client] -->|POST /requests| APIGW[API Gateway]
  APIGW --> Lambda1[Lambda (Java)\nRequest API]
  Lambda1 --> DDB[(DynamoDB\nrequests state)]
  Lambda1 -->|Generate Presigned URL| S3[(S3 bucket)]
  Client -->|PUT via Presigned URL| S3

  S3 -->|ObjectCreated event| Evt[S3 Notification or EventBridge]
  Evt --> Lambda2[Lambda\nUpdate status / kick workflow]
  Lambda2 --> DDB
```

## 現時点の整理

### 事実

- API Gateway / Lambda でバイナリを中継する方式はサイズ制約があり、大きなファイル受付には不向き
- そのため、**APIで受付情報を作成し、Presigned URLを返し、クライアントがS3へ直接アップロードする方式**が有力
- JavaでのREST API + DynamoDBの骨格としては `aws-sam-java-rest` が有力
- JavaでのS3 Presigned URL発行例としては `generate-s3-accelerate-presigned-url` が有力
- Presigned URL発行API、S3イベント後処理、一覧取得など実サービス寄りの参照として `bobs-used-bookstore-serverless` が有力

### 未確認事項

- Javaで上記要件を一気通貫に学べるAWS公式ワークショップが本当に存在しないかの最終確認
- EventBridge と S3 Notification のどちらをハンズオン範囲に含めるのが最適か
- 認証を IAM にするか Cognito にするか

### 仮説

- 案件アピール用途では、**全部盛りよりも「Java Lambda API + S3直送 + DynamoDB状態管理」を確実に見せる構成**の方が伝わりやすい
- 認証まで含める場合、学習コストと説明コストが上がるため、初期版では後回しにする可能性が高い
- 非同期処理は、まず `UPLOADED` への状態更新までを最小スコープにし、その後 `PROCESSING` 以降を拡張すると進めやすい

## 問題設定

- 単一の公式ハンズオンで完結しないため、**どのサンプルをどう組み合わせるか**を整理する必要がある
- AWS案件向けアピールとしては、単なる動作確認ではなく、**設計意図・AWSサービス選定理由・制約回避の考え方**まで説明できる構成が望ましい
- 範囲を広げすぎると、ハンズオンの完成度より調査コストが先行するリスクがある

## 制約

- 公式情報を主な根拠にする
- ベース実装はJavaを前提とする
- ハンズオンは、案件説明時に短時間で概要を伝えられる粒度を意識する
- 現時点では調査メモの整理段階であり、最終的な実装方式は未確定

## 初期提案

### 第一候補

以下を組み合わせたハンズオンとして設計する。

- `aws-sam-java-rest` をベースに `POST /requests`, `GET /requests/{id}`, `GET /requests` を構築
- `generate-s3-accelerate-presigned-url` の考え方を取り込み、`POST /requests` でPresigned URLを返す
- S3アップロード完了後に状態を `UPLOADED` へ更新する処理を追加

### ハンズオンで示したい価値

- API Gateway と Lambda によるJava REST API実装
- DynamoDBによる受付状態管理
- API Gatewayのサイズ制約を避けるS3直送設計
- 非同期処理へ発展可能な設計

### 追加候補

- Cognito または IAM による認証
- EventBridge または S3 Notification を用いた後続処理起動
- 一覧APIのページング

## 次に詰めるべき論点

- このハンズオンの主目的を「案件アピール用ポートフォリオ」に寄せるか、「学習教材」としての網羅性に寄せるか
- 実装スコープを最小構成に絞るか、認証や非同期処理まで含めるか
- 最終成果物を、コード中心にするか、設計説明資料込みにするか

## ユーザー確認があると精度が上がる点

- このハンズオンで最終的に作りたい成果物
- 例: 動くコード、構成図、実装解説資料、面談用説明メモ
- 想定するアピール先
- 例: AWS案件の面談、社内評価、学習記録、GitHub公開
- 実装範囲の希望
- 例: 最小構成重視、認証込み、イベント駆動込み
