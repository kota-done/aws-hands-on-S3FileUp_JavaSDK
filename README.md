### 全体概要
このハンズオンの目的は、Lambda（Java）で最小構成の受付APIを構築し、設計・IaC・テスト・運用まで一連で整えることです。  
ハンズオンでは、次の構成にしました。

- API Gateway（REST API）
- Lambda（Java 21）
- DynamoDB（受付状態管理）
- S3（アップロード先、Presigned URL）

AWS公式ハンズオンでは、上記を満たすハンズオンが存在しなかったため、以下公式サンプルの強みを組み合わせました。

- 骨格: `aws-samples/aws-sam-java-rest`
- Presigned URL: `aws-samples/generate-s3-accelerate-presigned-url`
- 拡張観点の補助: `aws-samples/bobs-used-bookstore-serverless`

### システム構成図

<img width="1510" height="749" alt="スクリーンショット 2026-04-05 15 03 23" src="https://github.com/user-attachments/assets/f5bfe7fc-7948-45fa-9cbe-b9a9d77c58f9" />

### 詳細記事
実行の様子や手順を以下にまとめています。

https://qiita.com/kota-done/items/c84ac6eacb596d6a02ad
