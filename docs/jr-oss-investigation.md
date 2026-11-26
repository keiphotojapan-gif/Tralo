# 既存鉄道プロジェクト調査・利用許諾・無料リアルタイム連携設計 (`jr-oss-investigation.md`)

TRALO における無料リアルタイム列車位置情報の拡張検討にあたり、指定2プロジェクトの技術構成・データ取得元・ライセンス・鉄道事業者規約を調査した結果を記録します。

---

## 1. 参照リポジトリ詳細調査

### 1.1 JR列車在線情報 (`Ktraveller285/jr-trainformation`)
- **URL**: https://github.com/Ktraveller285/jr-trainformation
- **使用技術**: TypeScript / Angular（SPA Webアプリケーション）
- **ソースコードライセンス**: **MIT License**（著作権表示・許諾表示を保持すれば改変・再配布可）
- **データ取得元・エンドポイント構造**:
  - **JR西日本 列車走行位置**: `https://www.train-guide.westjr.co.jp/` 系のWebフロント向けJSON（`api/v3/{line}.json`, `{line}_st.json`, `area_{area}_master.json`）。`trains[]` 内の `no`（列車番号）、`displayType`（種別）、`dest.text`（行先）、`pos`（`駅コードA_駅コードB` 形式の在線区間）、`delayMinutes`（遅延分）、`direction`（上下）を参照。
  - **JR東海 在来線在線情報**: `https://traininfo.jr-central.co.jp/zairaisen/` のWeb向け在線データ構造。
  - **JR北海道 列車運行情報**: `https://www3.jrhokkaido.co.jp/trainlocation/index.html` の札幌圏等列車位置Web表示。
  - **天気表示補助**: `https://weather.tsukumijima.net/`（ライブドア天気互換JSON）
- **更新頻度・キャッシュ・エラー処理**:
  - ブラウザ上の定期ポーリングで現在線区のJSONを取得し、表示状態へ変換。リポジトリ自身は外部向け公開APIサーバーを提供していない（READMEにもJR各社とは無関係な非公式ツールである旨を明記）。

### 1.2 JR四国 列車位置情報アプリ (`harukin/jrshikoku`)
- **URL**: https://git.pcgf.io/harukin/jrshikoku/src/branch/master
- **使用技術**: React Native / Expo（モバイルアプリ）
- **ソースコードライセンス**: **LICENSEファイル未掲載（All Rights Reserved 扱い）**
  - **TRALOでの扱い**: **ソースコード・UI・ロジックの無断複製/移植は一切禁止**（独立実装のみ）。
- **データ取得元・エンドポイント構造**:
  - **JR四国 公式Web**: `https://train.jr-shikoku.co.jp/` / `https://www.jr-shikoku.co.jp/train_info/`
  - **個人開発者バックエンド**: `https://api.haruk.in/dev/jrshikoku/...`（個人運用の非公式プロキシAPIであり、第三者アプリでの利用許諾・SLA・安定性保証は確認できない）。
- **データ構造**: 列車番号 (`trainNumber`)・種別 (`trainType`)・行先 (`destination`)・現在駅 (`currentStation`)・次駅 (`nextStation`)・遅延分 (`delayMinutes`)・運行状況 (`operationStatus`)・時刻表 (`timetable`)・最終更新時刻 (`lastUpdated`)。

---

## 2. コードライセンスとデータ提供元規約の分離評価

| 項目 | `jr-trainformation` | `jrshikoku` |
| :--- | :--- | :--- |
| **コードライセンス** | MIT License（帰属表示で参照可） | ライセンス未明示（**All Rights Reserved**・流用不可） |
| **データ元の公式API** | JR西日本・JR東海・JR北海道ともに一般開発者向け公開API/GTFS-RTなし | JR四国も一般開発者向け公開API/GTFS-RTなし |
| **データ元の利用規約** | JR北海道は「無断転載・複写・電磁媒体等への加工禁止」を明記。JR西日本/東海も公式Webページ上の閲覧を前提とし無断スクレイピング・再配信の許諾なし。 | JR四国公式サービスは規約同意に基づくブラウザ閲覧を前提とし、自動取得・複製・改変・再配布を禁止。`api.haruk.in` は個人APIで許諾未確認。 |
| **TRALO 実装方針** | 技術構造を参考に **完全独立Kotlin実装**（`JrTrainformationAdapter`）。デフォルトでは無断アクセスせず `ProviderResult.Unsupported` を返し、公式Webリンクを提供。許可済みプロキシ (`JR_TRAINFORMATION_PROXY_URL`) 設定時のみ通信可能。 | コード流用ゼロの **完全独立Kotlin実装**（`JrShikokuRealtimeAdapter`）。`api.haruk.in` や公式Webへの無断取得を禁止し、許可済みプロキシ (`JR_SHIKOKU_PROXY_URL`) 設定時のみ取得。未設定時は公式Web遷移で対応。 |

---

## 3. TRALO アダプター構成

1. **`JrTrainformationAdapter`** (`op_jr_west`, `op_jr_central`, `op_jr_hokkaido`)
   - `RailwayRealtimeProvider` 実装
   - `JrTrainformationApi` + `JrTrainformationJsonParser` + `ApiCacheAndRateLimiter`（30秒最小間隔 / 45秒新鮮キャッシュ / 15分オフライン時Staleフォールバック）
   - 承認済みエンドポイント設定時にJR西日本（大阪環状線・JR京都線/神戸線）、JR東海（東海道線・新幹線）、JR北海道（函館線・千歳線）の実データを `Train` / `TrainPosition`（駅座標補間＋方位角） / `TrainOperationStatus` / `StationTimetable` へ変換。
2. **`JrShikokuRealtimeAdapter`** (`op_jr_shikoku`)
   - `RailwayRealtimeProvider` 実装
   - `JrShikokuRealtimeApi` + `JrShikokuJsonParser` + `ApiCacheAndRateLimiter`
   - 承認済みエンドポイント設定時に予讃線・土讃線の列車走行位置・遅延・運行情報・時刻表を共通モデルへ変換。未設定時は仮想データ生成を行わず、規約上の理由を明示して「JR四国 公式走行位置Webを開く」ボタンを表示。
