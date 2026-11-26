# 全国鉄道事業者・交通API 調査レポート (`railway-api-coverage.md`)

本ドキュメントは、TRALO における全国の鉄道事業者（JR6社・大手私鉄17社局・公営地下鉄・モノレール・新交通・路面電車・地方私鉄・第三セクター）および主要交通APIプロバイダーの調査結果を記録したものです。

**調査基準**:
- 公式ドキュメント・公開ポータル・仕様書で確認できた事実のみ記載し、存在しないAPIや非公式スクレイピングは除外しています。
- 実装ステータス分類:
  - `IMPLEMENTED`: TRALO内にアダプター実装済み（APIキー設定で即時通信可能）
  - `AVAILABLE`: 公式公開仕様が存在し利用可能だが、一部データセット（例：静的GTFSバッチ取り込み等）が未実装
  - `AUTH_REQUIRED`: APIキー・利用者登録（無料枠含む）が必要
  - `CONTRACT_REQUIRED`: 法人契約・有償ライセンス契約が必要
  - `OPERATOR_PERMISSION_REQUIRED`: プラットフォーム契約に加え、各交通事業者の個別許諾が必要
  - `LIMITED`: 一部路線・実証実験・オープンデータチャレンジ期間等のみ対応
  - `UNAVAILABLE`: 一般向け公式API・オープンデータの公開が確認できない（Web閲覧専用画面のみ等）
  - `INVESTIGATING`: 詳細仕様・公開範囲を確認中

---

## 1. 統合交通APIプラットフォーム・連携基盤

| API提供元 | API名 | 公式ドキュメントURL | 列車位置 | 運行情報 | 時刻表 | 経路検索 | 更新頻度 | 認証方式 | 利用許諾 | 商用利用 | 実装状況 | 備考・未対応理由 |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| 公共交通オープンデータセンター (ODPT) | ODPT API v4 / GTFS-RT (`api.odpt.org/api/v4/`, `api-public.odpt.org/api/v4/`) | https://developer.odpt.org/ | 〇 (`odpt:Train`, GTFS-RT VP) | 〇 (`odpt:TrainInformation`) | 〇 (`odpt:StationTimetable`, GTFS) | × | 位置 20〜60秒 / 運行 1〜3分 / 静的随時 | クエリ `acl:consumerKey`（開発者登録無料） | 開発者規約＋各データセットライセンス確認 | 可（クレジット表記必須・負荷制限遵守） | `IMPLEMENTED` / `AUTH_REQUIRED` | `OdptRealtimeProvider` 実装済み。`ODPT_API_KEY` 未設定時は未設定表示。 |
| JR東日本 | リアルタイムデータ連携基盤 (RT-DIP) | https://www.jreast.co.jp/ | 〇 | 〇 | △（他基盤併用） | × | リアルタイム（非公開仕様） | 法人個別契約・専用接続 | JR東日本および参画各社との個別契約 | 契約条件による（有償） | `CONTRACT_REQUIRED` | 一般公開APIではなく交通案内事業者向け有償基盤。TRALOは通常ODPT/提携経由で補完し無断接続しない。 |
| 株式会社ヴァル研究所 | 駅すぱあと API (`api.ekispert.jp/v1/json/`) | https://docs.ekispert.com/ | ×（別製品mixway提供） | △（有償プラン/臨時情報） | 〇 (`/operationLine/timetable`) | 〇 (`/search/course/extreme`, `/light`) | ダイヤ・改正随時 | クエリ `key`（フリー/スタンダード等） | 利用登録・プラン契約 | プランにより可（フリープランは時刻表・機能制限あり） | `IMPLEMENTED` / `AUTH_REQUIRED` | `EkispertProvider` 実装済み。`EKISPERT_API_KEY` 設定で全国経路・駅・時刻表に接続。 |
| 株式会社ヴァル研究所 | mixway API (`mixway.ekispert.jp/v1/json/realtime/trip`) | https://docs-mixway.ekispert.jp/ | 〇（対応事業者のみ） | 〇（便遅延・運行状況） | 〇（トリップ連動） | 〇（駅すぱあと共通） | 10〜60秒（事業者依存） | アクセスキー＋契約認証 | API契約＋事業者ごとの個別利用許諾 | 契約・許諾範囲で可 | `IMPLEMENTED` / `OPERATOR_PERMISSION_REQUIRED` | `MixwayRealtimeProvider` 実装済み。`MIXWAY_API_KEY` および事業者許諾が必要。 |
| ジョルダン株式会社 | 乗換案内オープンAPI / Biz API | https://norikae.jorudan.co.jp/openapi/ / https://biz.jorudan.co.jp/ | × | 〇（Bizオプション運行情報API） | 〇（Biz） | 〇 | ダイヤ改正時・運行随時 | ドメイン/IP審査＋認証キー | 公開URL審査（オープンAPI）／法人契約（Biz） | オープンAPIは公開サービス限定（月間上限あり）／Bizは有償 | `CONTRACT_REQUIRED` | `JorudanProvider` アダプター実装済み。オープンAPIは本編リンク必須・Web審査要件があるためアプリ内統合はBiz契約が必要。 |
| 株式会社ナビタイムジャパン | NAVITIME API 2.0 (`route_transit`, `transport_node`) | https://api-sdk.navitime.co.jp/ | ×（一般SDK外） | 〇（遅延考慮探索） | 〇 | 〇（トータルナビ） | 随時 | CID+認証キー / RapidAPIヘッダ | 直接契約（90日試用可）またはAPIマーケット契約 | 有償契約で可 | `CONTRACT_REQUIRED` | `NavitimeProvider` アダプター実装済み。`NAVITIME_API_KEY` / 直接契約が必要。 |
| AIGID / 国土交通省 | GTFSデータリポジトリ (GTFS-JP / GTFS-RT) | https://www.gtfs-data.jp/ | △（一部自治体のみ） | △（一部のみ） | 〇（ZIP一括） | × | 事業者更新時 | 多くが認証不要（CC BY等） | 各データのライセンス表記に従う | 多くが可 | `AVAILABLE` | 主に地方バス・一部地方鉄軌道（宇都宮LRT等）の静的GTFS。プロトコル差異とZIPパースが必要なため今後バッチ連携可能。 |

---

## 2. JR各社（6社）

| 事業者名 | 主な路線名 | API提供元 | API名 / ソース | 公式ドキュメントURL | 列車位置 | 運行情報 | 時刻表 | 経路検索 | 更新頻度 | 認証方式 | 利用許諾 | 商用利用 | 実装状況 | 未対応理由・備考 |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| JR東日本 (東日本旅客鉄道) | 山手線・中央線・京浜東北線・東海道線・東北/上越/北陸新幹線 他 | ODPT / 駅すぱあと / JR東日本RT-DIP | ODPT `odpt:Train`, `TrainInformation`, `StationTimetable` / Ekispert | https://developer.odpt.org/ | 〇（ODPT対応線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` / `key` | ODPT規約（Challenge/基本） | 条件付可 | `LIMITED` | ODPT上で一部線区・チャレンジデータ公開。全線常時商用配信はRT-DIP契約が必要。 |
| JR北海道 (北海道旅客鉄道) | 函館線・千歳線・室蘭線・北海道新幹線 他 | JR北海道公式Web / 駅すぱあと | 列車運行情報Web（API非公開） / Ekispert（経路・時刻表） | https://www3.jrhokkaido.co.jp/webunkou/ | ×（公式API無） | ×（API無・Webのみ） | 〇（Ekispert） | 〇（Ekispert） | Webのみ | API無 / `key` | 経路等はEkispert契約 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 札幌圏のWeb列車位置表示はあるが外部API・GTFS-RT非公開。無断スクレイピング不可。 |
| JR東海 (東海旅客鉄道) | 東海道新幹線・東海道線・中央本線・高山本線 他 | JR東海公式Web / 駅すぱあと | 在来線運行状況Web（API非公開） / Ekispert | https://traininfo.jr-central.co.jp/ | ×（公式API無） | ×（API無・Webのみ） | 〇（Ekispert） | 〇（Ekispert） | Webのみ | API無 / `key` | 経路等はEkispert契約 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 東海道新幹線・在来線とも一般向けリアルタイムAPI・GTFS-RT非公開。スマートEX公式遷移で補完。 |
| JR西日本 (西日本旅客鉄道) | 大阪環状線・JR京都線・神戸線・山陽/北陸新幹線 他 | JR西日本Web(WESTER) / 駅すぱあと | 列車走行位置Web（API非公開） / Ekispert | https://www.train-guide.westjr.co.jp/ | ×（公式API無） | ×（API無・Webのみ） | 〇（Ekispert） | 〇（Ekispert） | Webのみ | API無 / `key` | 経路等はEkispert契約 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | WESTER・Web画面のみ提供で公式開発者APIは一般非公開。 |
| JR四国 (四国旅客鉄道) | 予讃線・土讃線・高徳線・瀬戸大橋線 | JR四国Web / 駅すぱあと | 列車走行位置情報Web（API非公開） / Ekispert | https://www.jr-shikoku.co.jp/train_info/ | ×（公式API無） | ×（API無・Webのみ） | 〇（Ekispert） | 〇（Ekispert） | Webのみ | API無 / `key` | 経路等はEkispert契約 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | ブラウザ閲覧用Webのみで公式API/GTFS-RT非公開。 |
| JR九州 (九州旅客鉄道) | 鹿児島本線・日豊本線・九州/西九州新幹線 | JR九州Web / 駅すぱあと | どれどれWeb（API非公開） / Ekispert | https://www.jrkyushu.co.jp/trains/ | ×（公式API無） | ×（API無・Webのみ） | 〇（Ekispert） | 〇（Ekispert） | Webのみ | API無 / `key` | 経路等はEkispert契約 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 公式APIおよびGTFS-RTの外部公開は確認できない。 |

---

## 3. 大手私鉄（17社局）

| 事業者名 | 主な路線名 | API提供元 | API名 / ソース | 公式ドキュメントURL | 列車位置 | 運行情報 | 時刻表 | 経路検索 | 更新頻度 | 認証方式 | 利用許諾 | 商用利用 | 実装状況 | 未対応理由・備考 |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| 東京地下鉄（東京メトロ） | 銀座線・丸ノ内線・日比谷線・東西線・千代田線・有楽町線・半蔵門線・南北線・副都心線 | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable`, `Railway`, `Station` | https://developer.odpt.org/ | 〇 | 〇 | 〇 | 〇（Ekispert） | 20〜60秒 | `acl:consumerKey` | ODPT開発者登録 | 可 | `IMPLEMENTED` | `ODPT_API_KEY` で全9路線の位置・運行・駅時刻表を直接取得可能。 |
| 東京都交通局（都営地下鉄・軌道） | 浅草線・三田線・新宿線・大江戸線・東京さくらトラム（都電荒川線）・日暮里舎人ライナー | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable`, GTFS-RT | https://developer.odpt.org/ | 〇 | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT基本ライセンス | 可 | `IMPLEMENTED` | 都営4線＋都電荒川線＋日暮里・舎人ライナー対応。 |
| 東武鉄道 | 東武スカイツリーライン・伊勢崎線・東上線・野田線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。データセット公開範囲・ライセンスにより一部線区限定あり。 |
| 西武鉄道 | 池袋線・新宿線・拝島線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。`ODPT_API_KEY` 連携対象に追加済み。 |
| 京成電鉄 | 京成本線・押上線・スカイアクセス線 | ODPT / 駅すぱあと | ODPT `odpt:TrainInformation`, `StationTimetable` | https://developer.odpt.org/ | △（チャレンジ/公開条件依存） | 〇 | 〇 | 〇（Ekispert） | 1〜3分 | `acl:consumerKey` | ODPT規約 | 条件付可 | `LIMITED` | 運行情報・時刻表中心。常時全列車位置の公開範囲が限定的。 |
| 京王電鉄 | 京王線・井の頭線・相模原線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。RT-DIPにも参画。 |
| 小田急電鉄 | 小田原線・江ノ島線・多摩線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation`, `StationTimetable` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。ロマンスカー含む検索はEkispert連携。 |
| 東急電鉄 | 東横線・田園都市線・目黒線・大井町線・池上線・東急多摩川線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation`, `StationTimetable` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。 |
| 京浜急行電鉄（京急） | 京急本線・空港線・久里浜線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation`, `StationTimetable` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。羽田空港アクセス路線。 |
| 相模鉄道（相鉄） | 相鉄本線・いずみ野線・新横浜線 | ODPT / 駅すぱあと | ODPT `odpt:Train`, `TrainInformation` | https://developer.odpt.org/ | 〇（公開線区） | 〇 | 〇 | 〇（Ekispert） | 30〜60秒 | `acl:consumerKey` | ODPT規約 | 条件付可 | `IMPLEMENTED`（ODPT経由） | ODPT参画社。 |
| 名古屋鉄道（名鉄） | 名鉄名古屋本線・犬山線・常滑線・空港線 | 駅すぱあと / 自社Web | Ekispert（経路・時刻表） | https://www.meitetsu.co.jp/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | バス部門GTFSはあるが鉄道リアルタイムAPIの一般公開なし。 |
| 近畿日本鉄道（近鉄） | 大阪線・奈良線・京都線・名古屋線 | 駅すぱあと / 自社Web | Ekispert（経路・時刻表） | https://www.kintetsu.co.jp/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 列車走行位置の一般公開API/GTFS-RTなし。 |
| 南海電気鉄道 | 南海本線・高野線・空港線 | 駅すぱあと / 自社アプリ | Ekispert（経路・時刻表） | https://www.howto-osaka.com/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 公式オープンAPI非公開。関空アクセス経路はEkispert対応。 |
| 京阪電気鉄道 | 京阪本線・鴨東線・中之島線・宇治線 | 駅すぱあと / 自社Web | Ekispert（経路・時刻表） | https://www.keihan.co.jp/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 運行情報Webのみ、一般開発者API非公開。 |
| 阪急電鉄 | 神戸線・宝塚線・京都線 | 駅すぱあと / 自社Web | Ekispert（経路・時刻表） | https://www.hankyu.co.jp/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 公式開発者向けリアルタイムAPI非公開。 |
| 阪神電気鉄道 | 阪神本線・なんば線 | 駅すぱあと / 自社Web | Ekispert（経路・時刻表） | https://rail.hanshin.co.jp/ | ×（公式API無） | ×（API無） | 〇（Ekispert） | 〇（Ekispert） | ダイヤ改正時 | `key`（Ekispert） | 鉄道位置API未公開 | Ekispert可 | `UNAVAILABLE`（位置） / `IMPLEMENTED`（経路） | 公式開発者向けリアルタイムAPI非公開。 |
| 西日本鉄道（西鉄） | 天神大牟田線・太宰府線・甘木線・貝塚線 | ODPT / 駅すぱあと | ODPT（一部）・GTFS（主にバス）・Ekispert | https://developer.odpt.org/ | △（バス中心・鉄道限定） | △ | 〇（Ekispert） | 〇（Ekispert） | 随時 | `acl:consumerKey` / `key` | 規約準拠 | 条件付可 | `LIMITED` | バス分野GTFS-RTが充実。鉄道リアルタイム位置は限定・非公開線区あり。 |

---

## 4. 公営地下鉄・政令市交通（8局）

| 事業者名 | 主な路線名 | API提供元 | API名 | 公式URL | 列車位置 | 運行情報 | 時刻表 | 経路検索 | 実装状況 | 未対応理由・備考 |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :--- | :--- |
| 横浜市交通局 | 横浜市営地下鉄ブルーライン・グリーンライン | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable` | https://developer.odpt.org/ | 〇 | 〇 | 〇 | 〇 | `IMPLEMENTED` | ODPT上で列車位置・運行情報を公開。 |
| Osaka Metro | 御堂筋線・谷町線・四つ橋線・中央線・堺筋線・ニュートラム | ODPT / 大阪オープンデータ / Ekispert | ODPT（参加）・Ekispert | https://developer.odpt.org/ | △ | △ | 〇 | 〇 | `LIMITED` | 一部オープンデータ連携実績はあるが全線リアルタイム位置常時公開は限定的。 |
| 京都市交通局 | 京都市営地下鉄烏丸線・東西線 | ODPT / 京都オープンデータ / Ekispert | ODPT（参加）・Ekispert | https://developer.odpt.org/ | △ | △ | 〇 | 〇 | `LIMITED` | 静的GTFS・一部情報中心でリアルタイム列車位置APIは限定的。 |
| 神戸市交通局 | 神戸市営地下鉄西神山手線・海岸線 | ODPT / 神戸オープンデータ / Ekispert | ODPT（参加）・Ekispert | https://developer.odpt.org/ | △ | △ | 〇 | 〇 | `LIMITED` | 市バス中心のオープンデータ展開で地下鉄リアルタイム位置は限定。 |
| 名古屋市交通局 | 東山線・名城線・鶴舞線・桜通線 | 名古屋市OD / Ekispert | GTFS（主に市バス）・Ekispert | https://www.kotsu.city.nagoya.jp/ | × | × | 〇（Ekispert） | 〇 | `UNAVAILABLE`（位置） | 地下鉄の一般向けリアルタイム位置API非公開。 |
| 札幌市交通局 | 南北線・東西線・東豊線・札幌市電 | 札幌市OD / Ekispert | 静的データ・Ekispert | https://www.city.sapporo.jp/st/ | × | × | 〇（Ekispert） | 〇 | `UNAVAILABLE`（位置） | リアルタイム列車位置APIは一般公開されていない。 |
| 仙台市交通局 | 南北線・東西線 | 仙台市OD / Ekispert | 静的GTFS（バス中心）・Ekispert | https://www.kotsu.city.sendai.jp/ | × | × | 〇（Ekispert） | 〇 | `UNAVAILABLE`（位置） | 地下鉄リアルタイム位置APIは非公開。 |
| 福岡市交通局 | 空港線・箱崎線・七隈線 | 福岡市OD / Ekispert | 福岡オープンデータ・Ekispert | https://subway.city.fukuoka.lg.jp/ | × | × | 〇（Ekispert） | 〇 | `UNAVAILABLE`（位置） | 地下鉄リアルタイム位置の一般向けAPI非公開。 |

---

## 5. モノレール・新交通・空港アクセス・首都圏中堅私鉄（10社）

| 事業者名 | 主な路線名 | API提供元 | API名 | 列車位置 | 運行情報 | 時刻表 | 実装状況 | 備考 |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :--- | :--- |
| 首都圏新都市鉄道 | つくばエクスプレス (TX) | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable` | 〇 | 〇 | 〇 | `IMPLEMENTED` | ODPT公開対象（`odpt.Operator:MIR`）。 |
| 多摩都市モノレール | 多摩都市モノレール線 | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable` | 〇 | 〇 | 〇 | `IMPLEMENTED` | ODPT公開対象（`odpt.Operator:TamaMonorail`）。 |
| ゆりかもめ | 東京臨海新交通臨海線 | ODPT | `odpt:TrainInformation`, `StationTimetable` | △ | 〇 | 〇 | `LIMITED` | ODPT登録済み（`odpt.Operator:Yurikamome`）。 |
| 東京モノレール | 東京モノレール羽田空港線 | ODPT / Ekispert | ODPT（チャレンジ等） / Ekispert | △ | 〇 | 〇 | `LIMITED` | 羽田空港アクセス線。ODPTおよびEkispert経路対応。 |
| 東京臨海高速鉄道 | りんかい線 | ODPT | `odpt:TrainInformation`, `StationTimetable` | △ | 〇 | 〇 | `LIMITED` | `odpt.Operator:TWR` としてODPT登録。 |
| 埼玉高速鉄道 | 埼玉スタジアム線 | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable` | 〇 | 〇 | 〇 | `IMPLEMENTED` | `odpt.Operator:SaitamaRailway`。東京メトロ南北線と直通。 |
| 東葉高速鉄道 | 東葉高速線 | ODPT | `odpt:Train`, `TrainInformation`, `StationTimetable` | 〇 | 〇 | 〇 | `IMPLEMENTED` | `odpt.Operator:ToyoRapid`。東京メトロ東西線と直通。 |
| 横浜シーサイドライン | 金沢シーサイドライン | ODPT / Ekispert | Ekispert・ODPT | × | △ | 〇 | `LIMITED` | 静的時刻表・経路中心。 |
| 大阪モノレール | 大阪モノレール本線・彩都線 | 駅すぱあと / 自社Web | Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | 一般向けリアルタイム位置API非公開。 |
| 沖縄都市モノレール | ゆいレール | 沖縄オープンデータ / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE`（静的GTFS） | 静的GTFS・Ekispert経路は利用可能、リアルタイム列車位置APIなし。 |

---

## 6. 路面電車・LRT・地方私鉄・第三セクター（14社）

| 事業者名 | 主な路線名 | API提供元 | API名 / 形式 | 列車位置 | 運行情報 | 時刻表 | 実装状況 | 備考・未対応理由 |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :--- | :--- |
| 宇都宮ライトレール | 芳賀・宇都宮LRT（ライトライン） | GTFSリポジトリ / Ekispert | GTFS-JP / Ekispert | ×（静的中心） | × | 〇 | `AVAILABLE` | 静的GTFS公開実績あり。列車リアルタイム位置API常時配信は未確認。 |
| 広島電鉄 | 市内線・宮島線 | 地域OD / Ekispert | Ekispert（経路・時刻表） | × | × | 〇 | `UNAVAILABLE`（位置） | 鉄道リアルタイム位置の一般API公開未確認。 |
| 伊予鉄道 | 松山市内電車・郊外電車 | 地域OD / Ekispert | GTFS（一部） / Ekispert | × | × | 〇 | `AVAILABLE` | 静的データ中心。リアルタイム列車API未公開。 |
| 熊本市交通局 | 熊本市電 A系統・B系統 | 熊本市OD / GTFS / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 静的GTFS・Ekispert経路対応。 |
| 函館市企業局交通部 | 函館市電 | 函館市OD / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 観光ルートはEkispert経路で対応可能。 |
| 富山地方鉄道 | 富山市内電車・鉄道線 | 富山県内OD / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 静的GTFS連携対象。 |
| 万葉線 | 万葉線（高岡〜越ノ潟） | GTFSリポジトリ / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 静的GTFS。 |
| とさでん交通 | 伊野線・後免線・桟橋線 | 地域OD / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 静的GTFS（主にバス）・Ekispert経路。 |
| えちぜん鉄道 | 勝山永平寺線・三国芦原線 | 福井県OD / Ekispert | GTFS / Ekispert | × | × | 〇 | `AVAILABLE` | 静的データ・Ekispert経路対応。 |
| しなの鉄道 | しなの鉄道線・北しなの線 | 長野県OD / Ekispert | GTFS / Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | 公式リアルタイムAPI非公開。 |
| IGRいわて銀河鉄道 | いわて銀河鉄道線 | Ekispert | Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | 公式リアルタイムAPI非公開。 |
| 青い森鉄道 | 青い森鉄道線 | Ekispert | Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | 公式リアルタイムAPI非公開。 |
| 江ノ島電鉄 | 江ノ電 | ODPT（小田急グループ関連） / Ekispert | Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | リアルタイム列車位置APIは一般非公開。 |
| 小田急箱根（箱根登山電車） | 箱根登山線 | ODPT（関連） / Ekispert | Ekispert | × | × | 〇 | `UNAVAILABLE`（位置） | リアルタイム列車位置APIは一般非公開。 |

---

## 7. まとめ（調査事業者数・カバレッジ方針）
- **調査済み鉄道・軌道事業者数**: **55社局**（JR 6社 / 大手私鉄 17社局 / 公営地下鉄 8局 / モノレール・新交通・空港線 10社 / 路面電車・地方私鉄・第三セクター 14社）＋ **7統合APIプラットフォーム**
- **リアルタイム列車位置の実装範囲**: `ODPT API v4 (odpt:Train)` および `mixway API (/v1/json/realtime/trip)` を通じて、認証キー・許諾が有効な公開路線（東京メトロ全線、都営地下鉄・都電・舎人ライナー、横浜市営地下鉄、多摩都市モノレール、TX、東急・小田急・京王・西武・東武・京急・相鉄・JR東日本のODPT公開線区等）の実データ取得に対応。
- **JR4社（西日本・東海・北海道・四国）独立アダプター**: `JrTrainformationAdapter` および `JrShikokuRealtimeAdapter` を実装済み（詳細: `docs/jr-oss-investigation.md`）。鉄道会社の利用規約と無許諾OSS保護の観点からデフォルトでの無断スクレイピングを無効化しており、公式Web閲覧リンクの提示および承認済みゲートウェイURL設定（`JR_TRAINFORMATION_PROXY_URL` / `JR_SHIKOKU_PROXY_URL`）時のリアルタイム位置・運行情報・時刻表パースに対応しています。
- **リアルタイム経路検索・駅時刻表の実装範囲**: `駅すぱあと API (/v1/json/search/course/extreme`, `/station`, `/operationLine/timetable`) および `ODPT (/odpt:StationTimetable)` により全国鉄道・新幹線・バス経路と時刻表に対応し、取得できたODPT/mixway遅延情報を経路レグに統合。
- **非対応事業者・APIキー未設定時の挙動**: 公式API非公開または契約・許諾未済の路線では架空の列車アイコンや推測位置を生成せず、調査結果に基づく明確な未対応理由・契約要件・公式Web遷移ボタンのみを表示します。

