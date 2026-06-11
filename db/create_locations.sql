-- PostGIS拡張を有効化（未導入の場合のみ作成される）
CREATE EXTENSION IF NOT EXISTS postgis;

-- 場所（緯度経度）と名前を保存するテーブル
CREATE TABLE IF NOT EXISTS locations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    -- 緯度経度。SRID 4326 = WGS84（GPSと同じ座標系）
    geom        geography(Point, 4326) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 地理空間検索を高速化する空間インデックス
CREATE INDEX IF NOT EXISTS idx_locations_geom ON locations USING GIST (geom);
