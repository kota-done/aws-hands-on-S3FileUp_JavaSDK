# FileUpAPI_AWS 作業開始メモ

## 結論

`FileUpAPI_AWS` は、現時点では **設計資料中心の Java / Maven プロジェクト** です。  
Lambda への Java デプロイは、基本的に **依存ライブラリを含めた JAR をビルドし、その成果物を Lambda に渡す** 形で進める想定です。  
また、今後この作業で出力する内容については、**結果だけでなく解説も含めて記事として残す前提** で整理していきます。

## 背景

ユーザーから、`dev-project` 配下の `FileUpAPI_AWS` で作業するよう依頼がありました。  
そのため最初に、対象ディレクトリの構成と適用ルールを確認しました。

確認した結果は次の通りです。

- `FileUpAPI_AWS` は独立した Git リポジトリとして存在する
- 子プロジェクト配下に専用の `AGENTS.md` は確認できず、親ワークスペースのルールを適用する
- `pom.xml` が存在し、Java / Maven プロジェクトとして構成されている
- `PLAN.md` `SPEC.md` `TODO.md` があり、設計・整理フェーズの情報はすでにまとまっている
- `src` 配下には、確認時点では実装ファイルがほぼ存在せず、コード本体はこれから整備する段階だった

## 現時点で確認したプロジェクト像

このプロジェクトは、AWS 案件向けのアピール材料として、
**Java ベースの Lambda API + S3 直接アップロード + DynamoDB 状態管理**
を題材にしたサーバーレス構成を目指しています。

主に `PLAN.md` と `TODO.md` から読み取れた点は次の通りです。

- API の主題はファイル受付である
- 想定 API は `POST /requests` `GET /requests/{id}` `GET /requests`
- 初期版は最小構成を重視する
- 認証、複雑な非同期処理、通知、画面は初期スコープ外
- 次の論点は API 詳細化、SAM 構成、GUI 手順、ビルド確認などである

つまり、現時点では「何を作るか」はかなり整理されており、  
「どう実装として起こしていくか」が次の中心課題です。

## 質問: Lambda で実装するのはこの Java プロジェクト全体を JAR にして渡すのか

この問いに対する回答は、**基本的には Yes** です。  
ただし厳密には、「プロジェクト全体を 1 つの実行単位としてまとめた成果物を Lambda にデプロイする」という理解が近いです。

### 通常の考え方

Java で Lambda を使う場合、一般的には次の流れになります。

1. Java コードを書く
2. Maven でビルドする
3. 依存ライブラリを含む JAR を作る
4. その成果物を Lambda に配置する
5. Lambda 設定でハンドラークラスを指定する

このプロジェクトの `pom.xml` には `maven-shade-plugin` が含まれていました。  
そのため、想定構成は **fat JAR / shaded JAR を作る方式** だと判断できます。

### 重要な補足

Lambda は「JAR を置けば自動で全部動く」というより、  
**JAR 内のどのクラスのどのメソッドを入口にするかを設定して実行する** 仕組みです。

たとえば概念上は次のような指定になります。

```text
com.example.fileupapi.handler.CreateRequestHandler::handleRequest
```

このため、1 つの JAR の中に複数のハンドラーを含めておき、

- `POST /requests` 用 Lambda
- `GET /requests/{id}` 用 Lambda
- `GET /requests` 用 Lambda

のように、**関数ごとにハンドラー指定を変えて使い分ける** 構成も可能です。

### 代替案

Java の Lambda デプロイには、JAR 方式以外に **コンテナイメージ方式** もあります。  
ただし、今回のような初期版・最小構成の文脈では、まずは JAR 方式の方が扱いやすいと考えられます。

## 今回ここまでで整理できたこと

- 作業対象は `FileUpAPI_AWS` に確定した
- 子プロジェクト固有の追加ルールは確認できず、親ルールを適用する
- 現状は実装よりも設計資料が先行している
- Java Lambda の配布単位は、基本的に依存込み JAR を前提に考える
- Lambda では JAR 全体を置いた上で、実際にはハンドラー指定で入口を切り替える

## 今後の進め方

ユーザーから、**これから出力する内容についてはすべて解説を求める** 方針が示されました。  
そのため今後は、単に結果だけを出すのではなく、少なくとも以下をセットで整理します。

- 何をしたか
- なぜそうしたか
- どこが重要か
- 次に何へ繋がるか

## 次アクション候補

- Lambda の実装単位を「1 JAR + 複数ハンドラー」でどう切るか整理する
- `POST /requests` から先に Java の雛形を作る
- SAM テンプレートを追加して Lambda と API Gateway の結線を固める
- 設計資料を記事向けに読みやすく再構成する

## 記事原型: このハンズオンの目的と参照元

### 目的

このハンズオンの目的は、**AWS案件で説明しやすい最小構成を、実際に動く形で示すこと**です。  
具体的には、JavaベースのLambda APIで受付を作成し、S3へファイルを直接アップロードし、DynamoDBで状態を管理する流れを一連で実装します。

### 複数ハンズオンを組み合わせる理由

`SPEC.md` では、今回の要件（Java REST API + Presigned URL + DynamoDB状態管理）を単一の教材で完結するより、**公式サンプルを役割ごとに組み合わせる方針**を採用しています。  
理由は、初期版で重要な「動く最小構成」と「設計意図の説明しやすさ」を両立しやすいためです。

### 組み合わせているハンズオン（参照先）

- 骨格（Java REST API + DynamoDB）
  - [aws-samples/aws-sam-java-rest](https://github.com/aws-samples/aws-sam-java-rest)
- Presigned URL発行（Java）
  - [aws-samples/generate-s3-accelerate-presigned-url](https://github.com/aws-samples/generate-s3-accelerate-presigned-url)
- 実サービス寄り設計・拡張観点（補助参照）
  - [aws-samples/bobs-used-bookstore-serverless](https://github.com/aws-samples/bobs-used-bookstore-serverless)

### 実装メモ: Config / DTOでRecordクラスを採用

本実装では、`Config` と `DTO` を中心に `record` を採用しています。  
具体的には、`AppConfig` および各レスポンス・入力DTOを `record` で定義しています。

採用理由は次の通りです。

- 設定値や入出力データを不変（immutable）として扱える
- コンストラクタ、アクセサ、`equals`、`hashCode`、`toString` を自動生成でき、ボイラープレートを減らせる
- データ保持が主目的のクラスであることを型定義から明確に表現できる

:::note info
`record` のアクセサは `getXxx()` ではなく、コンポーネント名そのままの `xxx()` です。  
例: `AppConfig(String requestsTableName, String uploadBucketName)` の場合、`requestsTableName()` と `uploadBucketName()` が生成されます。
:::

### Record仕様: `Object` デフォルト挙動との違い

Javaの通常クラスで `equals` `hashCode` `toString` を明示オーバーライドしない場合、`Object` のデフォルト挙動が使われます。  
一方で `record` は、値オブジェクト向けの実装をコンパイラが自動生成する仕様です。

- 通常クラス（`Object` デフォルト）
  - `equals`: 同一インスタンスかどうか（参照同一性）で比較する
  - `hashCode`: インスタンス識別ベースのハッシュになる
  - `toString`: `ClassName@16進数` 形式になる
- `record`（自動生成）
  - `equals`: 全コンポーネント値で比較する
  - `hashCode`: 全コンポーネント値から計算する
  - `toString`: `ClassName[field1=..., field2=...]` 形式で出力する

:::note info
`record` は「何も書かなくても値として扱いやすい」ことが仕様上の利点です。  
要件に応じて、`equals` `hashCode` `toString` を明示オーバーライドして挙動を調整することもできます。
:::

### APIリクエストからイベントハンドラー起動まで

本ハンズオンの入口は API Gateway です。利用者が `POST /requests` などのAPIを呼び出すと、まず API Gateway がHTTPリクエストを受け取ります。  
次に API Gateway は、SAMテンプレートで統合設定された Lambda 関数を同期呼び出しします。  
Lambda 側では Java のイベントハンドラー（`RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`）が起動し、API Gateway から渡されたイベントを解析します。  
その上でメソッドとパスを判定し、`POST /requests` の場合は受付情報の保存と S3 Presigned URL の生成を行い、JSONレスポンスを返します。

:::note info
S3へ一定時間だけアップロード可能な「Presigned URL（署名付きURL）」が生成されます。
:::

## 記事原型: 実装から設計へ戻した整理

### 背景

途中から、生成済みの Java ソースをそのまま前提にせず、**実装を観察して設計へ引き戻す** 方針へ切り替えました。  
その理由は、構想レベルの `SPEC.md` と、実際に存在する Java ソースとの間に、責務や状態遷移の粒度差があったためです。

### 実装から確認できたこと

- API は `POST /requests` `GET /requests/{id}` `GET /requests` の3本である
- 入口は `RequestApiHandler` であり、API Gateway からのイベントを受けて分岐している
- 処理の責務は `Handler -> Service -> Repository / S3PresignService` に分かれている
- DynamoDB は `requestId` を主キーにした単純構成である
- S3 Presigned URL の発行は Lambda から行う
- 一覧取得は DynamoDB `Scan` ベースであり、初期版の最小構成に寄せている

### 設計へ戻すときの論点

- 実装済みの責務を先に明確化する
- `SPEC.md` には機能仕様を残し、共通ルールは別ファイルへ分離する
- 将来拡張の話と、現行実装で確定している話を混在させない

## 記事原型: DESIGN_RULES を分離した理由

### 結論

`SPEC.md` にすべてを追記すると肥大化するため、**設計と実装の共通ルールだけを `DESIGN_RULES.md` に分離**しました。  
以後は、`SPEC.md` でユーザーと壁打ちした内容のうち、継続的に参照する基準だけを `DESIGN_RULES.md` に移す運用です。

### 分離した内容

- 命名規則
- 例外方針
- コメント方針
- レイヤ責務方針

### そのとき決めたポイント

- `Handler` は薄いアダプターに寄せる
- 入力値検証は `Validator` に委譲する
- 例外は独自例外を導入する前提とする
- ただし、HTTP ステータスとの対応表は後で設計する
- コメントは少なめにし、履歴管理は Git を正本とする
- `Util` は安易な退避先として使わない

## 記事原型: システム構成図と I/O 整理

### なぜ構成図から整理したか

API の項目詳細に入る前に、まず **誰がどこと何をやり取りするか** を構成図レベルで固定した方が、設計の前提がぶれにくいためです。  
そのため、利用者PC、API Gateway、Lambda、DynamoDB、S3 の関係を図にし、あわせて I/O の一覧を整理しました。

### 構成図で示したこと

- 利用者PCから API Gateway へ HTTP / JSON でリクエストが送られる
- API Gateway から Lambda が同期呼び出しされる
- Lambda から DynamoDB に受付情報を保存・取得する
- Lambda が S3 Presigned URL を生成する
- 利用者PCが Presigned URL を用いて S3 へ直接アップロードする

### I/O整理表で示したこと

- 入力元
- 出力先
- I/O内容
- I/O形式
- 同期区分
- 備考

### 意図

構成図は「全体像を一目で理解するため」、I/O整理表は「各接続の意味を取り違えないため」に併用しています。  
図だけだと粒度が粗くなり、表だけだと構成の直感が失われるため、両方を残す構成にしました。

## 記事原型: SAMテンプレートの位置付け

### 初期版の考え方

初期版では GUI での理解を優先しつつ、**同じ内容を後で IaC に落とせる形**も意識しました。  
そのため、`template.yaml` には以下の最小構成を持たせています。

- API Gateway
- Java 21 の Lambda 関数
- DynamoDB テーブル
- S3 バケット

### 環境変数と権限

- `REQUESTS_TABLE_NAME`
- `UPLOAD_BUCKET_NAME`

これらを Lambda に渡し、DynamoDB と S3 の必要最小限の権限を付与する前提で整理しています。

### 公式参照リンク

- AWS Lambda + Java ハンドラー
  - https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html
- API Gateway と Lambda 統合
  - https://docs.aws.amazon.com/lambda/latest/dg/services-apigateway.html
- S3 Presigned URL（AWS SDK for Java v2）
  - https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html
- Java `Object` リファレンス（`equals` `hashCode` `toString`）
  - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html

## 記事原型: AWS作業はローカル先行で1セッション完結に設計する

### 方針

このハンズオンでは、AWS環境に入る前にローカルで可能な確認を先に終わらせ、  
AWS作業は可能な限り1セッションで完結させる運用に設計しています。

具体的には、AWS着手前に `mvn test` と `mvn package` を実行し、  
当日の終了条件（例: API疎通確認、ログ確認、後片付け）を先に固定してから進めます。

### 理由

- クラウド環境は作成状態のまま残るとコストや管理負荷が発生するため
- セッションをまたぐと、削除漏れや設定差分の見落としが起きやすくなるため
- AWSでしか確認できない項目に時間を集中し、検証効率を上げるため
- 実務運用に近い「事前準備してから短時間で検証・回収する」進め方に合わせるため

### 期待する効果

- 作業の中断リスクと消し忘れリスクを低減できる
- AWS作業時間を短縮し、必要な確認に集中できる
- 実施結果を次回へ引き継ぎやすくなる

## 記事原型: GitHub運用とCI方針（1ジョブ verify + strict）

### 採用した方針

このハンズオンでは、PR必須のCIを **1ジョブ `mvn verify`** で運用します。  
あわせて `master` ブランチは、GitHub の保護ルールで **required status checks + strict** を前提にします。

### この方針にした理由

- 初期段階で複数ジョブへ分割しすぎると、設定保守の負荷が先に増えるため
- `verify` は Maven 標準ライフサイクルに沿って、ビルドとテスト観点を一括で確認しやすいため
- strict を有効にすると、マージ時点でベースブランチ追随済みの結果を要求できるため
- AWS実環境の重い検証は別レイヤに分離し、PRゲートは高速性と安全性のバランスを取るため

### GitHub運用ルール

- `master` への変更は Pull Request 経由のみとする
- CIジョブ `verify` を required status check として必須化する
- `Require branches to be up to date before merging` を有効化し、strict運用とする
- ジョブ名は `verify` で固定し、required check 名の運用を安定化する

### CIの位置付け

- PR必須CI
  - `mvn --batch-mode --update-snapshots verify`
- 別レイヤ（後続）
  - AWS実環境の疎通確認
  - 長時間の統合検証
  - 監視・運用テスト

### 今後の拡張方針

- テストケースが増えた段階で `compile / test / package` の分割ジョブ化を検討する
- 静的解析（Checkstyle/SpotBugs等）を段階導入する
- カバレッジ閾値の導入は、テスト母数が揃ってから行う

## 記事原型: テストメソッド命名規則を固定した理由

### 採用した命名規則

テストメソッドは次の形式で統一します。

- `対象メソッド_Check確認観点_with条件`

例:

- `validateCreateRequest_CheckRequiredUserId_withBlankUserId`
- `validateRequestId_CheckFormatValidation_withInvalidFormat`
- `createRequest_CheckInitialStatusAndTimestamp_withValidInput`

### この形式にした理由

- 先頭で対象メソッドを固定し、どの機能のテストかを即時に判別しやすくするため
- `Check` で確認観点を明示し、何を検証したいテストかを名前だけで伝えるため
- `with` を末尾に置き、前提条件の差分を見比べやすくするため
- CIの失敗ログ上で、失敗理由の切り分けをしやすくするため

## 記事原型: 今回実装した単体テストの確認項目とテストパターン

### 対象テストクラス

- `RequestValidatorTest`
- `RequestS3KeyBuilderTest`
- `RequestServiceTest`
- `RequestApiHandlerTest`

### 確認できる項目

- 入力値検証の必須条件
  - `request body`、`userId`、`fileName`、`requestId` の必須性
- 入力値検証の形式条件
  - `requestId` のフォーマットチェック
- S3キー生成ルール
  - `uploads/{userId}/{requestId}/{fileName}` 形式
  - `/` と `\`、前後空白の sanitize
- サービス層の初期登録ロジック
  - `requestId`、`s3Key`、`status=RECEIVED`、`createdAt/updatedAt` の設定
  - 保存処理とレスポンス組み立て
- DTOマッピングの整合
  - `RequestDetailResponse` 全項目のマッピング一致
  - `findById` の存在/非存在ハンドリング
  - 一覧取得時の複数レコード変換

### テストパターン

- 正常系
  - 妥当な入力で例外が発生しない
  - 期待フォーマットのレスポンス/キーが生成される
- 境界系
  - `blank` 入力
  - `null` 入力
  - 許容/非許容フォーマットの境界
- 異常系
  - 不正な `requestId` 形式で `IllegalArgumentException`
  - 未存在 `requestId` で `Optional.empty`
- 副作用確認
  - `createRequest` 実行時に保存データ件数と `status` が期待値になる

### 今回の位置付け

- CI（`mvn verify`）で毎回実行する単体テストとして、AWS実環境に依存しない範囲をカバーした
- IAM、S3実PUT、DynamoDB実アクセスなどのクラウド依存確認は、後続の統合テストで扱う前提とした

## 記事原型: ハンドラー契約テストを追加した理由と確認内容

### 追加した背景

Lambda実行前に手戻りを減らすため、`RequestApiHandler` に対して API Gateway REST v1 契約に沿った単体テストを追加した。  
Java実装の細部よりも、AWSハンズオンで詰まりやすい入口契約（メソッド・パス・ID抽出・エラー応答）を優先して確認する方針とした。

### テスト観点（契約別）

- `POST /requests` は作成契約として `201` を返す
- `GET /requests/{id}` は有効IDで `200`、未存在で `404`、不正形式で `400` を返す
- 既知外ルートは `404` を返す
- `/requests/`（末尾スラッシュ）は非対応として `404` を返す

### AWS契約への寄せ方

- `requestId` 抽出は `pathParameters.id` を優先
- 既存運用互換のため、`path` 文字列からの抽出は暫定フォールバックとして残置
- フォールバックは暫定対応であり、`pathParameters` が安定取得できることを確認後に削除する前提

### この段階で確認できること / できないこと

- 確認できること
  - ハンドラーのルーティングとレスポンス契約
  - バリデーション失敗時と例外時のHTTPステータス整合
- できないこと
  - IAM権限やAWSネットワークを含む実環境依存挙動
  - API Gateway実統合時の周辺設定ミス（統合テストで確認）

## 記事原型: CI高速ローカル + AWS手動実環境テストを採用した経緯

### 背景

テスト戦略の検討では、次の2点を重視した。  

- 再実行しやすく、今後も運用できること（再実効性）
- Lambda APIの通信観点を継続的に確認できること

一方で、AWS作業には時間確保と片付けが必要であり、毎回CIに含めると運用コストが高くなる課題があった。

### 比較した論点

- CIにクラウド依存テストを含めるか
- ローカル補助検証（SAM + local emulation）を必須ゲートにするか
- AWS実環境テストを自動化するか手動で分離するか

### 採用した結論

- PR必須CIは `mvn verify` のみ
- AWS実環境テストは手動実施（マージ前またはリリース前）
- `SAM + LocalStack` 相当のローカル統合検証は補助手段として利用し、CI必須からは外す

### この結論にした理由

- CIを高速かつ安定に保ち、日常開発のフィードバックを優先できる
- IAM、実API Gateway統合、実DynamoDB/S3連携はAWSでしか最終確認できない
- 「ローカルで早く検知」しつつ「クラウドで最終保証」する二段構えが、運用負荷と品質のバランスを取りやすい
