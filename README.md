# map-app 🗺️

地図上でクリックした場所にピンを立てて保存・管理できるWebアプリです。

🌐 **デモ**: https://map-app-ashy-xi.vercel.app

## 使用技術

### バックエンド

| 技術                 | 用途                       |
| -------------------- | -------------------------- |
| Java 21              | メイン言語                 |
| Jersey 3             | REST APIフレームワーク     |
| Grizzly              | 組み込みHTTPサーバー       |
| Jackson              | JSON変換                   |
| HikariCP             | DBコネクションプール       |
| PostgreSQL + PostGIS | 地理空間データの保存・検索 |
| Docker               | DB環境のコンテナ化         |

### フロントエンド

| 技術                    | 用途                         |
| ----------------------- | ---------------------------- |
| Vue 3 (Composition API) | UIフレームワーク             |
| Vite                    | ビルドツール                 |
| Leaflet + OpenStreetMap | 地図表示                     |
| Nominatim API           | 住所検索（ジオコーディング） |

## 機能

- 📍 **ピンの登録** - 地図をクリックして名前・カテゴリを入力してピンを立てる
- ✏️ **ピンの編集** - 名前・カテゴリを後から変更できる
- 🗑️ **ピンの削除** - ポップアップから削除できる
- 📋 **サイドバー** - 登録したピンの一覧表示・クリックで地図移動
- 🏷️ **カテゴリ管理** - カテゴリの追加・編集・削除。カテゴリごとにピンの色が変わる
- 🔍 **住所検索** - Nominatim APIで住所・施設名を検索して地図移動
- 📡 **半径検索** - 地図の中心から半径1km以内のピンを赤く表示
- 📍 **現在地** - ブラウザのGeolocation APIで現在地に地図を移動
- 📱 **レスポンシブ対応** - スマホでもサイドバーをスライドメニューで操作可能

## ローカル開発環境の構築

### 必要なもの

- Docker / Docker Compose
- Java 21
- Node.js 18以上
- Maven 3.9以上（または同梱の`./mvnw`を使用）

### 手順

**1. リポジトリをクローン**

```bash
git clone https://github.com/ShotaArakawa/map-app.git
cd map-app
```

**2. 環境変数ファイルを作成**

```bash
cat > frontend/.env.local << 'EOF'
VITE_API_BASE_URL=http://localhost:8080/api
EOF
```

**3. DBを起動**

```bash
docker compose up -d
```

**4. バックエンドをビルド・起動**

```bash
# ビルド
mvn clean package -DskipTests

# 起動
java -jar target/map-app.jar
```

**5. フロントエンドを起動**

```bash
cd frontend
npm install
npm run dev
```

**6. ブラウザで開く**

```
http://localhost:5173
```

### よく使うコマンド

| コマンド                        | 説明                       |
| ------------------------------- | -------------------------- |
| `docker compose up -d`          | DBをバックグラウンドで起動 |
| `docker compose down`           | DBを停止                   |
| `mvn clean package -DskipTests` | バックエンドをビルド       |
| `java -jar target/map-app.jar`  | バックエンドを起動         |
| `kill -9 $(lsof -t -i :8080)`   | 8080ポートを強制解放       |
| `cd frontend && npm run dev`    | フロントエンドを起動       |
| `cd frontend && npm run build`  | フロントエンドを本番ビルド |

### 環境変数一覧（バックエンド）

| 変数名        | デフォルト値            | 説明                   |
| ------------- | ----------------------- | ---------------------- |
| `DB_HOST`     | `localhost`             | DBホスト名             |
| `DB_PORT`     | `5432`                  | DBポート番号           |
| `DB_NAME`     | `map_db`                | DB名                   |
| `DB_USER`     | `shota`                 | DBユーザー名           |
| `DB_PASSWORD` | `password`              | DBパスワード           |
| `CORS_ORIGIN` | `http://localhost:5173` | CORSを許可するオリジン |
| `PORT`        | `8080`                  | サーバーポート番号     |

## 本番環境へのデプロイ

### 構成

```
フロントエンド  → Vercel（無料）
バックエンド   → Render（無料枠）
DB          → Render PostgreSQL（無料枠）
```

### バックエンド + DB（Render）

**1. RenderでPostgreSQLを作成**

- Renderダッシュボード → New → PostgreSQL
- Plan: Free
- 作成後にHostname・Port・Database・Username・Passwordをメモ

**2. RenderでWeb Serviceを作成**

- New → Web Service → GitHubリポジトリを選択
- Runtime: Docker
- Plan: Free

**3. 環境変数を設定**

Renderダッシュボード → Environment に以下を追加：

| Key           | Value                                                    |
| ------------- | -------------------------------------------------------- |
| `DB_HOST`     | RenderのDB Hostname                                      |
| `DB_PORT`     | `5432`                                                   |
| `DB_NAME`     | RenderのDB Database名                                    |
| `DB_USER`     | RenderのDB Username                                      |
| `DB_PASSWORD` | RenderのDB Password                                      |
| `CORS_ORIGIN` | VercelのデプロイURL（例: `https://your-app.vercel.app`） |

### フロントエンド（Vercel）

**1. 本番用環境変数ファイルを確認**

`frontend/.env.production` にバックエンドのURLが設定されていることを確認：

```
VITE_API_BASE_URL=https://your-render-app.onrender.com/api
```

**2. Vercel CLIでデプロイ**

```bash
cd frontend
npm install -g vercel
vercel --prod
```

または GitHubリポジトリをVercelに連携して自動デプロイも可能。

### 注意事項

- Renderの無料枠は**15分アクセスがないとスリープ**します。初回アクセス時に50秒程度かかる場合があります
- `target/` ディレクトリはgitignoreに追加することを推奨します

## API一覧

| メソッド | エンドポイント                                  | 説明                                |
| -------- | ----------------------------------------------- | ----------------------------------- |
| GET      | /api/locations                                  | 全ピン取得                          |
| GET      | /api/locations/{id}                             | ピン1件取得                         |
| GET      | /api/locations/nearby?lat=xx&lng=yy&radius=1000 | 半径検索                            |
| POST     | /api/locations                                  | ピン登録                            |
| PUT      | /api/locations/{id}                             | ピン更新                            |
| DELETE   | /api/locations/{id}                             | ピン削除                            |
| GET      | /api/categories                                 | カテゴリ一覧取得                    |
| POST     | /api/categories                                 | カテゴリ追加                        |
| PUT      | /api/categories/{id}                            | カテゴリ更新（デフォルト2件は不可） |
| DELETE   | /api/categories/{id}                            | カテゴリ削除（デフォルト2件は不可） |

## ディレクトリ構成

```
map-app/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── render.yaml
├── src/main/java/com/portfolio/mapapp/
│   ├── Main.java
│   ├── config/
│   ├── db/
│   │   ├── Database.java
│   │   ├── LocationDao.java
│   │   └── CategoryDao.java
│   ├── filter/
│   │   └── CorsFilter.java
│   ├── model/
│   │   ├── Location.java
│   │   ├── Category.java
│   │   └── ...
│   └── resource/
│       ├── LocationResource.java
│       └── CategoryResource.java
└── frontend/
    ├── .env.production
    └── src/
        └── components/
            └── MapView.vue
```
