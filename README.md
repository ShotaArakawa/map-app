# map-app 🗺️

地図上でクリックした場所にピンを立てて保存・管理できるWebアプリです。

## 使用技術

### バックエンド
| 技術 | 用途 |
|---|---|
| Java 21 | メイン言語 |
| Jersey 3 | REST APIフレームワーク |
| Grizzly | 組み込みHTTPサーバー |
| Jackson | JSON変換 |
| HikariCP | DBコネクションプール |
| PostgreSQL + PostGIS | 地理空間データの保存・検索 |
| Docker | DB環境のコンテナ化 |

### フロントエンド
| 技術 | 用途 |
|---|---|
| Vue 3 (Composition API) | UIフレームワーク |
| Vite | ビルドツール |
| Leaflet + OpenStreetMap | 地図表示 |
| Nominatim API | 住所検索（ジオコーディング） |

## 機能

- 📍 **ピンの登録** - 地図をクリックして名前・カテゴリを入力してピンを立てる
- ✏️ **ピンの編集** - 名前・カテゴリを後から変更できる
- 🗑️ **ピンの削除** - ポップアップから削除できる
- 📋 **サイドバー** - 登録したピンの一覧表示・クリックで地図移動
- 🏷️ **カテゴリ管理** - カテゴリの追加・編集・削除。カテゴリごとにピンの色が変わる
- 🔍 **住所検索** - Nominatim APIで住所・施設名を検索して地図移動
- 📡 **半径検索** - 地図の中心から半径1km以内のピンを赤く表示
- 📍 **現在地** - ブラウザのGeolocation APIで現在地に地図を移動

## 起動方法

### 必要なもの
- Docker / Docker Compose
- Java 21
- Node.js 18以上

### 手順

**1. リポジトリをクローン**
```bash
git clone https://github.com/ShotaArakawa/map-app.git
cd map-app
```

**2. DBを起動**
```bash
docker compose up -d
```

**3. バックエンドを起動**
```bash
java -jar target/map-app.jar
```

ソースから起動する場合：
```bash
mvn clean package -DskipTests && java -jar target/map-app.jar
```

**4. フロントエンドを起動**
```bash
cd frontend
npm install
npm run dev
```

**5. ブラウザで開く**
```
http://localhost:5173
```

## API一覧

| メソッド | エンドポイント | 説明 |
|---|---|---|
| GET | /api/locations | 全ピン取得 |
| GET | /api/locations/{id} | ピン1件取得 |
| GET | /api/locations/nearby?lat=xx&lng=yy&radius=1000 | 半径検索 |
| POST | /api/locations | ピン登録 |
| PUT | /api/locations/{id} | ピン更新 |
| DELETE | /api/locations/{id} | ピン削除 |
| GET | /api/categories | カテゴリ一覧取得 |
| POST | /api/categories | カテゴリ追加 |
| PUT | /api/categories/{id} | カテゴリ更新（デフォルト2件は不可） |
| DELETE | /api/categories/{id} | カテゴリ削除（デフォルト2件は不可） |

## ディレクトリ構成

```
map-app/
├── docker-compose.yml
├── pom.xml
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
    └── src/
        └── components/
            └── MapView.vue
```
