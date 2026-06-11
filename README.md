# map-app

PostGIS の `locations` テーブルに対する REST API。
**Jersey + 組み込み Grizzly HTTP サーバー + Jackson(JSON)** で構成し、`java -jar` 単体で起動できます。

## 構成

| 技術 | 役割 |
|---|---|
| Jersey 3.1 (JAX-RS) | REST フレームワーク |
| Grizzly | 組み込みHTTPサーバー（外部のTomcat等が不要） |
| Jackson | JSON ⇔ Javaオブジェクト変換 |
| HikariCP + PostgreSQL JDBC | DB接続（コネクションプール） |
| PostGIS | 座標を `geography(Point,4326)` で保存 |

## 必要なもの

- Java 21
- 起動中の PostgreSQL/PostGIS（`docker compose up -d` で `map_db` が立つ）
- テーブル作成: [db/create_locations.sql](db/create_locations.sql)

## ビルド & 起動

```bash
# DB を起動
docker compose up -d

# ビルド（fat jar を生成）
./mvnw clean package

# 起動
java -jar target/map-app.jar
# もしくは: ./mvnw exec:java
```

起動後のベースURL: `http://localhost:8080/api/`

### 接続設定（環境変数で上書き可）

| 変数 | 既定値 |
|---|---|
| `PORT` | `8080` |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `map_db` |
| `DB_USER` | `shota` |
| `DB_PASSWORD` | `password` |

## API

### `GET /api/locations` — 全件取得
```bash
curl http://localhost:8080/api/locations
```

### `GET /api/locations/{id}` — 1件取得（無ければ 404）
```bash
curl http://localhost:8080/api/locations/1
```

### `POST /api/locations` — 新規登録（201 Created）
```bash
curl -X POST http://localhost:8080/api/locations \
  -H 'Content-Type: application/json' \
  -d '{"name":"東京駅","latitude":35.6812,"longitude":139.7671}'
```

レスポンス例:
```json
{
  "id": 4,
  "name": "東京駅",
  "latitude": 35.6812,
  "longitude": 139.7671,
  "createdAt": "2026-06-09T09:46:51.034510Z"
}
```

`latitude`(-90〜90) / `longitude`(-180〜180) の範囲外や `name` 未指定は 400 を返します。
