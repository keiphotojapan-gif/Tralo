package com.example.data.remote

import com.example.model.CoverageStatus
import com.example.model.LatLngPoint
import com.example.model.ProviderCapability
import com.example.model.RailwayLine
import com.example.model.RailwayOperator
import com.example.model.RailwayStation

object NationalRailwayRegistry {

    val operators: List<RailwayOperator> = listOf(
        // JR 6社
        RailwayOperator(
            operatorId = "op_jr_east",
            operatorName = "JR東日本（東日本旅客鉄道）",
            operatorCode = "JRE",
            supportedRegions = listOf("首都圏", "関東", "東北", "甲信越"),
            apiProvider = "ODPT API v4 / JR東日本 RT-DIP / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "ODPT公開線区・チャレンジ対象線区のみAPI連携可。全線商用リアルタイム配信はRT-DIP契約が必要。",
            odptOperatorId = "odpt.Operator:JR-East"
        ),
        RailwayOperator(
            operatorId = "op_jr_central",
            operatorName = "JR東海（東海旅客鉄道）",
            operatorCode = "JRC",
            supportedRegions = listOf("東海", "首都圏〜関西（新幹線）"),
            apiProvider = "駅すぱあと（経路・時刻表） / 公式Web画面",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "東海道新幹線・在来線の列車走行位置APIおよびGTFS-RTは一般公開されていません（規約上無断取得禁止・承認済プロキシ接続のみ対応）。",
            officialInfoUrl = "https://traininfo.jr-central.co.jp/zairaisen/"
        ),
        RailwayOperator(
            operatorId = "op_jr_west",
            operatorName = "JR西日本（西日本旅客鉄道）",
            operatorCode = "JRW",
            supportedRegions = listOf("近畿", "北陸", "中国"),
            apiProvider = "駅すぱあと（経路・時刻表） / WESTER Web",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "WESTER等で列車走行位置を提供していますが、外部向け公式APIは一般非公開です（規約上無断取得禁止・承認済プロキシ接続のみ対応）。",
            officialInfoUrl = "https://www.train-guide.westjr.co.jp/"
        ),
        RailwayOperator(
            operatorId = "op_jr_hokkaido",
            operatorName = "JR北海道（北海道旅客鉄道）",
            operatorCode = "JRH",
            supportedRegions = listOf("北海道"),
            apiProvider = "駅すぱあと（経路・時刻表） / 公式Web",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "公式Webの列車位置表示は著作権・転載禁止規定があり外部API・GTFS-RTは非公開です（承認済プロキシ接続のみ対応）。",
            officialInfoUrl = "https://www3.jrhokkaido.co.jp/trainlocation/index.html"
        ),
        RailwayOperator(
            operatorId = "op_jr_shikoku",
            operatorName = "JR四国（四国旅客鉄道）",
            operatorCode = "JRS",
            supportedRegions = listOf("四国"),
            apiProvider = "駅すぱあと（経路・時刻表） / 公式Web",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "JR四国公式Webは自動取得・複製・再配信を禁止しており公開API・GTFS-RTはありません（承認済プロキシ接続のみ対応）。",
            officialInfoUrl = "https://www.jr-shikoku.co.jp/train_info/"
        ),
        RailwayOperator(
            operatorId = "op_jr_kyushu",
            operatorName = "JR九州（九州旅客鉄道）",
            operatorCode = "JRK",
            supportedRegions = listOf("九州"),
            apiProvider = "駅すぱあと（経路・時刻表） / 公式Web",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "どれどれ等のWeb提供のみで外部開発者向けリアルタイムAPIは非公開です。",
            officialInfoUrl = "https://www.jrkyushu.co.jp/trains/"
        ),

        // 大手私鉄・公営・地下鉄 (ODPT対応中心 + 全国大手)
        RailwayOperator(
            operatorId = "op_tokyo_metro",
            operatorName = "東京地下鉄（東京メトロ）",
            operatorCode = "TYO",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 経由で全9路線の列車走行位置・運行情報・駅時刻表に対応。",
            odptOperatorId = "odpt.Operator:TokyoMetro"
        ),
        RailwayOperator(
            operatorId = "op_toei",
            operatorName = "東京都交通局（都営地下鉄・都電・舎人）",
            operatorCode = "TOEI",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 経由で都営4線・都電荒川線・日暮里舎人ライナーに対応。",
            odptOperatorId = "odpt.Operator:Toei"
        ),
        RailwayOperator(
            operatorId = "op_tobu",
            operatorName = "東武鉄道",
            operatorCode = "TOBU",
            supportedRegions = listOf("首都圏", "北関東"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT公開データセット（スカイツリーライン・東上線等）に対応。",
            odptOperatorId = "odpt.Operator:Tobu"
        ),
        RailwayOperator(
            operatorId = "op_seibu",
            operatorName = "西武鉄道",
            operatorCode = "SEIBU",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（池袋線・新宿線等）に対応。",
            odptOperatorId = "odpt.Operator:Seibu"
        ),
        RailwayOperator(
            operatorId = "op_keisei",
            operatorName = "京成電鉄",
            operatorCode = "KEISEI",
            supportedRegions = listOf("首都圏", "成田空港"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "ODPT経由の運行情報・時刻表中心。常時列車位置APIは公開条件に依存します。",
            odptOperatorId = "odpt.Operator:Keisei"
        ),
        RailwayOperator(
            operatorId = "op_keio",
            operatorName = "京王電鉄",
            operatorCode = "KEIO",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（京王線・井の頭線）に対応。",
            odptOperatorId = "odpt.Operator:Keio"
        ),
        RailwayOperator(
            operatorId = "op_odakyu",
            operatorName = "小田急電鉄",
            operatorCode = "ODAKYU",
            supportedRegions = listOf("首都圏", "箱根・湘南"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（小田原線・江ノ島線・多摩線）に対応。",
            odptOperatorId = "odpt.Operator:Odakyu"
        ),
        RailwayOperator(
            operatorId = "op_tokyu",
            operatorName = "東急電鉄",
            operatorCode = "TOKYU",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（東横線・田園都市線等）に対応。",
            odptOperatorId = "odpt.Operator:Tokyu"
        ),
        RailwayOperator(
            operatorId = "op_keikyu",
            operatorName = "京浜急行電鉄（京急）",
            operatorCode = "KEIKYU",
            supportedRegions = listOf("首都圏", "羽田空港"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（本線・空港線等）に対応。",
            odptOperatorId = "odpt.Operator:Keikyu"
        ),
        RailwayOperator(
            operatorId = "op_sotetsu",
            operatorName = "相模鉄道（相鉄）",
            operatorCode = "SOTETSU",
            supportedRegions = listOf("首都圏", "神奈川"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（相鉄本線・いずみ野線等）に対応。",
            odptOperatorId = "odpt.Operator:Sotetsu"
        ),
        RailwayOperator(
            operatorId = "op_meitetsu",
            operatorName = "名古屋鉄道（名鉄）",
            operatorCode = "MEITETSU",
            supportedRegions = listOf("東海"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "鉄道の一般向けリアルタイム位置APIは非公開です。"
        ),
        RailwayOperator(
            operatorId = "op_kintetsu",
            operatorName = "近畿日本鉄道（近鉄）",
            operatorCode = "KINTETSU",
            supportedRegions = listOf("近畿", "東海"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置API/GTFS-RTは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_nankai",
            operatorName = "南海電気鉄道",
            operatorCode = "NANKAI",
            supportedRegions = listOf("近畿", "関西空港"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "リアルタイム列車位置APIの一般公開は確認できません。"
        ),
        RailwayOperator(
            operatorId = "op_keihan",
            operatorName = "京阪電気鉄道",
            operatorCode = "KEIHAN",
            supportedRegions = listOf("近畿"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向け公式列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_hankyu",
            operatorName = "阪急電鉄",
            operatorCode = "HANKYU",
            supportedRegions = listOf("近畿"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_hanshin",
            operatorName = "阪神電気鉄道",
            operatorCode = "HANSHIN",
            supportedRegions = listOf("近畿"),
            apiProvider = "駅すぱあと（経路・時刻表）",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_nishitetsu",
            operatorName = "西日本鉄道（西鉄）",
            operatorCode = "NISHITETSU",
            supportedRegions = listOf("九州"),
            apiProvider = "ODPT（一部） / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "バスGTFS-RTは充実していますが、鉄道リアルタイム列車位置の常時API提供は限定的です。"
        ),

        // 公営地下鉄・モノレール・新交通・第三セクター・LRT
        RailwayOperator(
            operatorId = "op_yokohama_sub",
            operatorName = "横浜市交通局（横浜市営地下鉄）",
            operatorCode = "YOKOHAMA",
            supportedRegions = listOf("首都圏", "神奈川"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4（ブルーライン・グリーンライン）に対応。",
            odptOperatorId = "odpt.Operator:YokohamaMunicipal"
        ),
        RailwayOperator(
            operatorId = "op_tx",
            operatorName = "首都圏新都市鉄道（つくばエクスプレス）",
            operatorCode = "MIR",
            supportedRegions = listOf("首都圏", "北関東"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE,
                ProviderCapability.ROUTE_SEARCH
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 に対応。",
            odptOperatorId = "odpt.Operator:MIR"
        ),
        RailwayOperator(
            operatorId = "op_tama_mono",
            operatorName = "多摩都市モノレール",
            operatorCode = "TAMA",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 に対応。",
            odptOperatorId = "odpt.Operator:TamaMonorail"
        ),
        RailwayOperator(
            operatorId = "op_yurikamome",
            operatorName = "ゆりかもめ",
            operatorCode = "YURIKAMOME",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(ProviderCapability.OPERATION_STATUS, ProviderCapability.STATION_TIMETABLE),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "ODPT上で運行情報・時刻表公開対象。",
            odptOperatorId = "odpt.Operator:Yurikamome"
        ),
        RailwayOperator(
            operatorId = "op_twr",
            operatorName = "東京臨海高速鉄道（りんかい線）",
            operatorCode = "TWR",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(ProviderCapability.OPERATION_STATUS, ProviderCapability.STATION_TIMETABLE),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "ODPT登録事業者。運行情報・時刻表中心。",
            odptOperatorId = "odpt.Operator:TWR"
        ),
        RailwayOperator(
            operatorId = "op_saitama_rail",
            operatorName = "埼玉高速鉄道",
            operatorCode = "SR",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 に対応。",
            odptOperatorId = "odpt.Operator:SaitamaRailway"
        ),
        RailwayOperator(
            operatorId = "op_toyo_rapid",
            operatorName = "東葉高速鉄道",
            operatorCode = "TOYORAPID",
            supportedRegions = listOf("首都圏"),
            apiProvider = "ODPT API v4",
            capabilities = setOf(
                ProviderCapability.TRAIN_POSITIONS,
                ProviderCapability.OPERATION_STATUS,
                ProviderCapability.STATION_TIMETABLE
            ),
            coverageStatus = CoverageStatus.IMPLEMENTED,
            statusReason = "ODPT API v4 に対応。",
            odptOperatorId = "odpt.Operator:ToyoRapid"
        ),
        RailwayOperator(
            operatorId = "op_osaka_metro",
            operatorName = "Osaka Metro（大阪メトロ）",
            operatorCode = "OSAKAMETRO",
            supportedRegions = listOf("近畿"),
            apiProvider = "ODPT（一部） / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "全線常時のリアルタイム列車位置APIは一般非公開です。"
        ),
        RailwayOperator(
            operatorId = "op_kyoto_sub",
            operatorName = "京都市交通局（京都市営地下鉄）",
            operatorCode = "KYOTOSUB",
            supportedRegions = listOf("近畿"),
            apiProvider = "ODPT（一部） / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "静的GTFS中心でリアルタイム列車位置APIは限定的です。"
        ),
        RailwayOperator(
            operatorId = "op_nagoya_sub",
            operatorName = "名古屋市交通局（名古屋市営地下鉄）",
            operatorCode = "NAGOYASUB",
            supportedRegions = listOf("東海"),
            apiProvider = "駅すぱあと / 名古屋市オープンデータ",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "市営地下鉄のリアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_sapporo_sub",
            operatorName = "札幌市交通局（地下鉄・札幌市電）",
            operatorCode = "SAPPORO",
            supportedRegions = listOf("北海道"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "リアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_fukuoka_sub",
            operatorName = "福岡市交通局（福岡市地下鉄）",
            operatorCode = "FUKUOKASUB",
            supportedRegions = listOf("九州"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_utsunomiya_lrt",
            operatorName = "宇都宮ライトレール（LRT）",
            operatorCode = "ULRT",
            supportedRegions = listOf("北関東"),
            apiProvider = "GTFSリポジトリ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFSの公開実績がありますが、リアルタイム列車位置APIは常時提供対象外です。"
        ),
        RailwayOperator(
            operatorId = "op_hiroden",
            operatorName = "広島電鉄（路面電車）",
            operatorCode = "HIRODEN",
            supportedRegions = listOf("中国"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置APIの常時公開は確認できていません。"
        ),
        RailwayOperator(
            operatorId = "op_yui_rail",
            operatorName = "沖縄都市モノレール（ゆいレール）",
            operatorCode = "YUIRAIL",
            supportedRegions = listOf("沖縄"),
            apiProvider = "沖縄GTFS / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFSおよび駅すぱあと経路検索に対応。リアルタイム位置APIなし。"
        ),
        RailwayOperator(
            operatorId = "op_kobe_sub",
            operatorName = "神戸市交通局（神戸市営地下鉄）",
            operatorCode = "KOBESUB",
            supportedRegions = listOf("近畿"),
            apiProvider = "ODPT（参加） / 神戸オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "市バス中心のオープンデータ公開で、地下鉄リアルタイム列車位置は限定的です。"
        ),
        RailwayOperator(
            operatorId = "op_sendai_sub",
            operatorName = "仙台市交通局（仙台市地下鉄）",
            operatorCode = "SENDAISUB",
            supportedRegions = listOf("東北"),
            apiProvider = "仙台市オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "市バスGTFSはあるものの地下鉄リアルタイム列車位置APIは非公開です。"
        ),
        RailwayOperator(
            operatorId = "op_tokyo_monorail",
            operatorName = "東京モノレール",
            operatorCode = "TMONO",
            supportedRegions = listOf("首都圏", "羽田空港"),
            apiProvider = "ODPT API v4 / 駅すぱあと",
            capabilities = setOf(ProviderCapability.OPERATION_STATUS, ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "ODPT参画事業者。羽田空港アクセス経路と運行情報に対応。",
            odptOperatorId = "odpt.Operator:TokyoMonorail"
        ),
        RailwayOperator(
            operatorId = "op_seaside_line",
            operatorName = "横浜シーサイドライン",
            operatorCode = "YSL",
            supportedRegions = listOf("首都圏", "神奈川"),
            apiProvider = "ODPT（一部） / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.LIMITED,
            statusReason = "静的時刻表・経路検索中心の連携です。"
        ),
        RailwayOperator(
            operatorId = "op_osaka_monorail",
            operatorName = "大阪モノレール",
            operatorCode = "OMONO",
            supportedRegions = listOf("近畿", "伊丹空港"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "一般向けリアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_iyotetsu",
            operatorName = "伊予鉄道（松山市内電車・郊外電車）",
            operatorCode = "IYOTETSU",
            supportedRegions = listOf("四国"),
            apiProvider = "地域オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFS・駅すぱあと経路に対応。リアルタイム列車位置API未公開。"
        ),
        RailwayOperator(
            operatorId = "op_kumamoto_tram",
            operatorName = "熊本市交通局（熊本市電）",
            operatorCode = "KUMAMOTO",
            supportedRegions = listOf("九州"),
            apiProvider = "熊本市GTFS / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFS・駅すぱあと経路検索に対応。"
        ),
        RailwayOperator(
            operatorId = "op_hakodate_tram",
            operatorName = "函館市企業局交通部（函館市電）",
            operatorCode = "HAKODATE",
            supportedRegions = listOf("北海道"),
            apiProvider = "函館市オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "観光経路・時刻表は駅すぱあとおよび静的データで対応。"
        ),
        RailwayOperator(
            operatorId = "op_toyama_chiho",
            operatorName = "富山地方鉄道（市内電車・鉄道線）",
            operatorCode = "TOYAMA",
            supportedRegions = listOf("北陸"),
            apiProvider = "富山県オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFS連携対象・リアルタイム列車位置API未公開。"
        ),
        RailwayOperator(
            operatorId = "op_manyosen",
            operatorName = "万葉線",
            operatorCode = "MANYOSEN",
            supportedRegions = listOf("北陸"),
            apiProvider = "GTFSリポジトリ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的GTFS対応。リアルタイム列車位置APIなし。"
        ),
        RailwayOperator(
            operatorId = "op_tosaden",
            operatorName = "とさでん交通（路面電車）",
            operatorCode = "TOSADEN",
            supportedRegions = listOf("四国"),
            apiProvider = "地域GTFS / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的データ中心・駅すぱあと経路対応。"
        ),
        RailwayOperator(
            operatorId = "op_echizen",
            operatorName = "えちぜん鉄道",
            operatorCode = "ECHIZEN",
            supportedRegions = listOf("北陸"),
            apiProvider = "福井県オープンデータ / 駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.AVAILABLE,
            statusReason = "静的時刻表・経路検索対応。"
        ),
        RailwayOperator(
            operatorId = "op_shinano",
            operatorName = "しなの鉄道",
            operatorCode = "SHINANO",
            supportedRegions = listOf("甲信越"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "公式リアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_igr",
            operatorName = "IGRいわて銀河鉄道",
            operatorCode = "IGR",
            supportedRegions = listOf("東北"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "公式リアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_aoimori",
            operatorName = "青い森鉄道",
            operatorCode = "AOIMORI",
            supportedRegions = listOf("東北"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "公式リアルタイム列車位置APIは公開されていません。"
        ),
        RailwayOperator(
            operatorId = "op_enoden",
            operatorName = "江ノ島電鉄（江ノ電）",
            operatorCode = "ENODEN",
            supportedRegions = listOf("首都圏", "湘南"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "リアルタイム列車位置APIの一般公開は確認できていません。"
        ),
        RailwayOperator(
            operatorId = "op_hakone_tozan",
            operatorName = "小田急箱根（箱根登山電車）",
            operatorCode = "HAKONE",
            supportedRegions = listOf("箱根・富士"),
            apiProvider = "駅すぱあと",
            capabilities = setOf(ProviderCapability.STATION_TIMETABLE, ProviderCapability.ROUTE_SEARCH),
            coverageStatus = CoverageStatus.UNAVAILABLE,
            statusReason = "リアルタイム列車位置APIの一般公開は確認できていません。"
        )
    )

    val stations: List<RailwayStation> = listOf(
        // Tokyo Metro Ginza Line
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Shibuya", "渋谷", "G01", 35.6580, 139.7016, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Omotesando", "表参道", "G02", 35.6652, 139.7125, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.AkasakaMitsuke", "赤坂見附", "G05", 35.6770, 139.7372, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Shimbashi", "新橋", "G08", 35.6663, 139.7583, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Ginza", "銀座", "G09", 35.6712, 139.7650, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Nihombashi", "日本橋", "G11", 35.6823, 139.7736, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Ueno", "上野", "G16", 35.7115, 139.7753, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),
        RailwayStation("odpt.Station:TokyoMetro.Ginza.Asakusa", "浅草", "G19", 35.7107, 139.7977, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Ginza")),

        // Tokyo Metro Marunouchi Line
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Ogikubo", "荻窪", "M01", 35.7045, 139.6201, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Shinjuku", "新宿", "M08", 35.6915, 139.7012, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Ginza", "銀座", "M16", 35.6719, 139.7639, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Tokyo", "東京", "M17", 35.6814, 139.7652, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Otemachi", "大手町", "M18", 35.6848, 139.7660, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),
        RailwayStation("odpt.Station:TokyoMetro.Marunouchi.Ikebukuro", "池袋", "M25", 35.7295, 139.7109, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Marunouchi")),

        // Tokyo Metro Hibiya Line
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Nakameguro", "中目黒", "H01", 35.6443, 139.6991, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Ebisu", "恵比寿", "H02", 35.6467, 139.7101, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Roppongi", "六本木", "H04", 35.6628, 139.7314, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Ginza", "銀座", "H09", 35.6721, 139.7640, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Akihabara", "秋葉原", "H16", 35.6987, 139.7755, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),
        RailwayStation("odpt.Station:TokyoMetro.Hibiya.Ueno", "上野", "H18", 35.7124, 139.7764, listOf("op_tokyo_metro"), listOf("odpt.Railway:TokyoMetro.Hibiya")),

        // Toei Oedo Line
        RailwayStation("odpt.Station:Toei.Oedo.ShinjukuNishiguchi", "新宿西口", "E01", 35.6933, 139.6990, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),
        RailwayStation("odpt.Station:Toei.Oedo.UenoOkachimachi", "上野御徒町", "E09", 35.7075, 139.7745, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),
        RailwayStation("odpt.Station:Toei.Oedo.Ryogoku", "両国", "E12", 35.6968, 139.7959, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),
        RailwayStation("odpt.Station:Toei.Oedo.Tsukishima", "月島", "E16", 35.6646, 139.7848, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),
        RailwayStation("odpt.Station:Toei.Oedo.Roppongi", "六本木", "E23", 35.6628, 139.7314, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),
        RailwayStation("odpt.Station:Toei.Oedo.Tochomae", "都庁前", "E28", 35.6905, 139.6927, listOf("op_toei"), listOf("odpt.Railway:Toei.Oedo")),

        // Toei Asakusa Line
        RailwayStation("odpt.Station:Toei.Asakusa.NishiMagome", "西馬込", "A01", 35.5871, 139.7060, listOf("op_toei"), listOf("odpt.Railway:Toei.Asakusa")),
        RailwayStation("odpt.Station:Toei.Asakusa.Gotanda", "五反田", "A05", 35.6264, 139.7234, listOf("op_toei"), listOf("odpt.Railway:Toei.Asakusa")),
        RailwayStation("odpt.Station:Toei.Asakusa.Shimbashi", "新橋", "A10", 35.6660, 139.7590, listOf("op_toei"), listOf("odpt.Railway:Toei.Asakusa")),
        RailwayStation("odpt.Station:Toei.Asakusa.Asakusa", "浅草", "A18", 35.7087, 139.7963, listOf("op_toei"), listOf("odpt.Railway:Toei.Asakusa")),
        RailwayStation("odpt.Station:Toei.Asakusa.Oshiage", "押上", "A20", 35.7103, 139.8133, listOf("op_toei"), listOf("odpt.Railway:Toei.Asakusa")),

        // JR Yamanote Line
        RailwayStation("odpt.Station:JR-East.Yamanote.Tokyo", "東京", "JY01", 35.6812, 139.7671, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),
        RailwayStation("odpt.Station:JR-East.Yamanote.Ueno", "上野", "JY05", 35.7141, 139.7774, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),
        RailwayStation("odpt.Station:JR-East.Yamanote.Ikebukuro", "池袋", "JY13", 35.7295, 139.7109, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),
        RailwayStation("odpt.Station:JR-East.Yamanote.Shinjuku", "新宿", "JY17", 35.6896, 139.7006, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),
        RailwayStation("odpt.Station:JR-East.Yamanote.Shibuya", "渋谷", "JY20", 35.6580, 139.7016, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),
        RailwayStation("odpt.Station:JR-East.Yamanote.Shinagawa", "品川", "JY25", 35.6285, 139.7388, listOf("op_jr_east"), listOf("odpt.Railway:JR-East.Yamanote")),

        // Tokyu Toyoko Line
        RailwayStation("odpt.Station:Tokyu.Toyoko.Shibuya", "渋谷", "TY01", 35.6580, 139.7016, listOf("op_tokyu"), listOf("odpt.Railway:Tokyu.Toyoko")),
        RailwayStation("odpt.Station:Tokyu.Toyoko.Jiyugaoka", "自由が丘", "TY07", 35.6075, 139.6687, listOf("op_tokyu"), listOf("odpt.Railway:Tokyu.Toyoko")),
        RailwayStation("odpt.Station:Tokyu.Toyoko.MusashiKosugi", "武蔵小杉", "TY11", 35.5764, 139.6596, listOf("op_tokyu"), listOf("odpt.Railway:Tokyu.Toyoko")),
        RailwayStation("odpt.Station:Tokyu.Toyoko.Yokohama", "横浜", "TY21", 35.4658, 139.6223, listOf("op_tokyu"), listOf("odpt.Railway:Tokyu.Toyoko")),

        // Odakyu Odawara Line
        RailwayStation("odpt.Station:Odakyu.Odawara.Shinjuku", "新宿", "OH01", 35.6896, 139.6996, listOf("op_odakyu"), listOf("odpt.Railway:Odakyu.Odawara")),
        RailwayStation("odpt.Station:Odakyu.Odawara.Machida", "町田", "OH27", 35.5422, 139.4454, listOf("op_odakyu"), listOf("odpt.Railway:Odakyu.Odawara")),
        RailwayStation("odpt.Station:Odakyu.Odawara.Odawara", "小田原", "OH47", 35.2562, 139.1556, listOf("op_odakyu"), listOf("odpt.Railway:Odakyu.Odawara")),

        // Keikyu Main / Airport Line
        RailwayStation("odpt.Station:Keikyu.Main.Shinagawa", "品川", "KK01", 35.6285, 139.7380, listOf("op_keikyu"), listOf("odpt.Railway:Keikyu.Main")),
        RailwayStation("odpt.Station:Keikyu.Main.KeikyuKamata", "京急蒲田", "KK11", 35.5608, 139.7236, listOf("op_keikyu"), listOf("odpt.Railway:Keikyu.Main")),
        RailwayStation("odpt.Station:Keikyu.Main.Yokohama", "横浜", "KK37", 35.4658, 139.6223, listOf("op_keikyu"), listOf("odpt.Railway:Keikyu.Main")),

        // Tokaido Shinkansen (for reference & unavailable live-position honesty)
        RailwayStation("jrc.TokaidoShinkansen.Tokyo", "東京", "TS01", 35.6812, 139.7671, listOf("op_jr_central"), listOf("jrc.TokaidoShinkansen")),
        RailwayStation("jrc.TokaidoShinkansen.Nagoya", "名古屋", "TS10", 35.1709, 136.8815, listOf("op_jr_central"), listOf("jrc.TokaidoShinkansen")),
        RailwayStation("jrc.TokaidoShinkansen.Kyoto", "京都", "TS16", 34.9858, 135.7588, listOf("op_jr_central"), listOf("jrc.TokaidoShinkansen")),
        RailwayStation("jrc.TokaidoShinkansen.ShinOsaka", "新大阪", "TS17", 34.7335, 135.5003, listOf("op_jr_central"), listOf("jrc.TokaidoShinkansen")),

        // JR West Osaka Loop & JR Kyoto/Kobe Line
        RailwayStation("jrw.OsakaLoop.Osaka", "大阪", "JR-O11", 34.7025, 135.4959, listOf("op_jr_west"), listOf("jrw.OsakaLoop", "jrw.KyotoLine")),
        RailwayStation("jrw.OsakaLoop.Tennoji", "天王寺", "JR-O01", 34.6468, 135.5133, listOf("op_jr_west"), listOf("jrw.OsakaLoop")),
        RailwayStation("jrw.KyotoLine.Kyoto", "京都", "JR-A31", 34.9858, 135.7588, listOf("op_jr_west"), listOf("jrw.KyotoLine")),
        RailwayStation("jrw.KyotoLine.Takatsuki", "高槻", "JR-A38", 34.8517, 135.6177, listOf("op_jr_west"), listOf("jrw.KyotoLine")),
        RailwayStation("jrw.KyotoLine.ShinOsaka", "新大阪", "JR-A46", 34.7335, 135.5003, listOf("op_jr_west"), listOf("jrw.KyotoLine")),
        RailwayStation("jrw.KyotoLine.Sannomiya", "三ノ宮", "JR-A61", 34.6945, 135.1955, listOf("op_jr_west"), listOf("jrw.KyotoLine")),

        // JR Central Tokaido Conventional Line
        RailwayStation("jrc.Tokaido.Toyohashi", "豊橋", "CA42", 34.7629, 137.3819, listOf("op_jr_central"), listOf("jrc.TokaidoLine")),
        RailwayStation("jrc.Tokaido.Kanayama", "金山", "CA66", 35.1430, 136.9006, listOf("op_jr_central"), listOf("jrc.TokaidoLine")),
        RailwayStation("jrc.Tokaido.Nagoya", "名古屋", "CA68", 35.1709, 136.8815, listOf("op_jr_central"), listOf("jrc.TokaidoLine")),
        RailwayStation("jrc.Tokaido.Gifu", "岐阜", "CA74", 35.4095, 136.7567, listOf("op_jr_central"), listOf("jrc.TokaidoLine")),

        // JR Hokkaido Hakodate / Chitose Airport Line
        RailwayStation("jrh.Hakodate.Otaru", "小樽", "S15", 43.1976, 140.9937, listOf("op_jr_hokkaido"), listOf("jrh.HakodateChitose")),
        RailwayStation("jrh.Hakodate.Sapporo", "札幌", "01", 43.0687, 141.3508, listOf("op_jr_hokkaido"), listOf("jrh.HakodateChitose")),
        RailwayStation("jrh.Chitose.ShinSapporo", "新札幌", "H05", 43.0389, 141.4724, listOf("op_jr_hokkaido"), listOf("jrh.HakodateChitose")),
        RailwayStation("jrh.Chitose.MinamiChitose", "南千歳", "H14", 42.8091, 141.6749, listOf("op_jr_hokkaido"), listOf("jrh.HakodateChitose")),
        RailwayStation("jrh.Chitose.NewChitoseAirport", "新千歳空港", "AP15", 42.7875, 141.6813, listOf("op_jr_hokkaido"), listOf("jrh.HakodateChitose")),

        // JR Shikoku All 8 Lines Stations
        RailwayStation("jrs.Yosan.Takamatsu", "高松", "Y00", 34.3510, 134.0466, listOf("op_jr_shikoku"), listOf("jrs.YosanLine", "jrs.KotokuLine")),
        RailwayStation("jrs.Yosan.Sakaide", "坂出", "Y08", 34.3125, 133.8562, listOf("op_jr_shikoku"), listOf("jrs.YosanLine")),
        RailwayStation("jrs.Yosan.Tadotsu", "多度津", "Y12", 34.2704, 133.7574, listOf("op_jr_shikoku"), listOf("jrs.YosanLine", "jrs.DosanLine")),
        RailwayStation("jrs.Yosan.Kanonji", "観音寺", "Y19", 34.1220, 133.6537, listOf("op_jr_shikoku"), listOf("jrs.YosanLine")),
        RailwayStation("jrs.Yosan.Matsuyama", "松山", "Y55", 33.8400, 132.7517, listOf("op_jr_shikoku"), listOf("jrs.YosanLine", "jrs.UchikoLine")),
        RailwayStation("jrs.Yosan.Uwajima", "宇和島", "U28", 33.2239, 132.5622, listOf("op_jr_shikoku"), listOf("jrs.YosanLine", "jrs.YodoLine")),
        RailwayStation("jrs.Dosan.Kotohira", "琴平", "D15", 34.1889, 133.8226, listOf("op_jr_shikoku"), listOf("jrs.DosanLine")),
        RailwayStation("jrs.Dosan.AwaIkeda", "阿波池田", "D22", 34.0282, 133.8054, listOf("op_jr_shikoku"), listOf("jrs.DosanLine", "jrs.TokushimaLine")),
        RailwayStation("jrs.Dosan.Kochi", "高知", "D45", 33.5671, 133.5435, listOf("op_jr_shikoku"), listOf("jrs.DosanLine")),
        RailwayStation("jrs.Dosan.Kubokawa", "窪川", "TK26", 33.2183, 133.1433, listOf("op_jr_shikoku"), listOf("jrs.DosanLine", "jrs.YodoLine")),
        RailwayStation("jrs.Kotoku.Ritsurin", "栗林", "T03", 34.3311, 134.0533, listOf("op_jr_shikoku"), listOf("jrs.KotokuLine")),
        RailwayStation("jrs.Kotoku.Tokushima", "徳島", "T00", 34.0725, 134.5517, listOf("op_jr_shikoku"), listOf("jrs.KotokuLine", "jrs.TokushimaLine", "jrs.MugiLine")),
        RailwayStation("jrs.Tokushima.Sako", "佐古", "B01", 34.0811, 134.5361, listOf("op_jr_shikoku"), listOf("jrs.TokushimaLine", "jrs.NarutoLine")),
        RailwayStation("jrs.Mugi.Mugi", "牟岐", "M23", 33.6700, 134.4250, listOf("op_jr_shikoku"), listOf("jrs.MugiLine")),
        RailwayStation("jrs.Naruto.Naruto", "鳴門", "N10", 34.2361, 134.6064, listOf("op_jr_shikoku"), listOf("jrs.NarutoLine")),
        RailwayStation("jrs.Yodo.Egawasaki", "江川崎", "G30", 33.0850, 132.8683, listOf("op_jr_shikoku"), listOf("jrs.YodoLine")),
        RailwayStation("jrs.Uchiko.Uchiko", "内子", "U12", 33.5550, 132.6517, listOf("op_jr_shikoku"), listOf("jrs.YosanLine", "jrs.UchikoLine"))
    )

    val lines: List<RailwayLine> = listOf(
        buildLine(
            lineId = "odpt.Railway:TokyoMetro.Ginza",
            operatorId = "op_tokyo_metro",
            lineName = "東京メトロ銀座線",
            lineCode = "G",
            lineColorHex = "#FF9500",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:TokyoMetro.Marunouchi",
            operatorId = "op_tokyo_metro",
            lineName = "東京メトロ丸ノ内線",
            lineCode = "M",
            lineColorHex = "#F62E36",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:TokyoMetro.Hibiya",
            operatorId = "op_tokyo_metro",
            lineName = "東京メトロ日比谷線",
            lineCode = "H",
            lineColorHex = "#B5B5AC",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:Toei.Oedo",
            operatorId = "op_toei",
            lineName = "都営大江戸線",
            lineCode = "E",
            lineColorHex = "#B6007A",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:Toei.Asakusa",
            operatorId = "op_toei",
            lineName = "都営浅草線",
            lineCode = "A",
            lineColorHex = "#EC6E65",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:JR-East.Yamanote",
            operatorId = "op_jr_east",
            lineName = "JR山手線",
            lineCode = "JY",
            lineColorHex = "#9ACD32",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.LIMITED,
            unsupportedReason = "ODPT公開条件に準拠（全線常時商用配信にはRT-DIP契約が必要）"
        ),
        buildLine(
            lineId = "odpt.Railway:Tokyu.Toyoko",
            operatorId = "op_tokyu",
            lineName = "東急東横線",
            lineCode = "TY",
            lineColorHex = "#DA0442",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:Odakyu.Odawara",
            operatorId = "op_odakyu",
            lineName = "小田急小田原線",
            lineCode = "OH",
            lineColorHex = "#2288CC",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "odpt.Railway:Keikyu.Main",
            operatorId = "op_keikyu",
            lineName = "京急本線",
            lineCode = "KK",
            lineColorHex = "#E8383D",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrc.TokaidoShinkansen",
            operatorId = "op_jr_central",
            lineName = "東海道新幹線",
            lineCode = "TK",
            lineColorHex = "#0B5394",
            realtimeSupported = false,
            timetableSupported = true,
            coverageStatus = CoverageStatus.UNAVAILABLE,
            unsupportedReason = "JR東海は列車走行位置の外部API/GTFS-RTを一般公開していません（時刻表・経路検索は駅すぱあとAPI対応）。"
        ),
        buildLine(
            lineId = "jrw.OsakaLoop",
            operatorId = "op_jr_west",
            lineName = "JR大阪環状線",
            lineCode = "O",
            lineColorHex = "#E60012",
            realtimeSupported = false,
            timetableSupported = true,
            coverageStatus = CoverageStatus.UNAVAILABLE,
            unsupportedReason = "JR西日本は公式APIを外部非公開としており、無断スクレイピングを禁止しています（承認済ゲートウェイ設定または公式Web案内で補完）。"
        ),
        buildLine(
            lineId = "jrw.KyotoLine",
            operatorId = "op_jr_west",
            lineName = "JR京都線・神戸線",
            lineCode = "A",
            lineColorHex = "#0072BC",
            realtimeSupported = false,
            timetableSupported = true,
            coverageStatus = CoverageStatus.UNAVAILABLE,
            unsupportedReason = "JR西日本は公式APIを外部非公開としています（承認済プロキシ設定または公式Web画面で閲覧可）。"
        ),
        buildLine(
            lineId = "jrc.TokaidoLine",
            operatorId = "op_jr_central",
            lineName = "JR東海道本線（名古屋圏）",
            lineCode = "CA",
            lineColorHex = "#F5761A",
            realtimeSupported = false,
            timetableSupported = true,
            coverageStatus = CoverageStatus.UNAVAILABLE,
            unsupportedReason = "JR東海在来線在線情報は公式API非公開・規約保護対象です（承認済プロキシ設定または公式Webで閲覧可）。"
        ),
        buildLine(
            lineId = "jrh.HakodateChitose",
            operatorId = "op_jr_hokkaido",
            lineName = "JR北海道 函館線・千歳線",
            lineCode = "H",
            lineColorHex = "#009944",
            realtimeSupported = false,
            timetableSupported = true,
            coverageStatus = CoverageStatus.UNAVAILABLE,
            unsupportedReason = "JR北海道はWeb運行情報サイトでの無断転載・電磁媒体加工を禁止しています（承認済プロキシ設定または公式Webで閲覧可）。"
        ),
        buildLine(
            lineId = "jrs.YosanLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 予讃線",
            lineCode = "Y",
            lineColorHex = "#009AD6",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.DosanLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 土讃線",
            lineCode = "D",
            lineColorHex = "#E9546B",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.KotokuLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 高徳線",
            lineCode = "T",
            lineColorHex = "#28B876",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.TokushimaLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 徳島線",
            lineCode = "B",
            lineColorHex = "#FF8C00",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.MugiLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 牟岐線",
            lineCode = "M",
            lineColorHex = "#8A2BE2",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.NarutoLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 鳴門線",
            lineCode = "N",
            lineColorHex = "#FF1493",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.YodoLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 予土線",
            lineCode = "G",
            lineColorHex = "#20B2AA",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        ),
        buildLine(
            lineId = "jrs.UchikoLine",
            operatorId = "op_jr_shikoku",
            lineName = "JR四国 内子線",
            lineCode = "U",
            lineColorHex = "#DAA520",
            realtimeSupported = true,
            timetableSupported = true,
            coverageStatus = CoverageStatus.IMPLEMENTED
        )
    )

    private fun buildLine(
        lineId: String,
        operatorId: String,
        lineName: String,
        lineCode: String,
        lineColorHex: String,
        realtimeSupported: Boolean,
        timetableSupported: Boolean,
        coverageStatus: CoverageStatus,
        unsupportedReason: String? = null
    ): RailwayLine {
        val lineStations = stations.filter { lineId in it.lineIds }
        return RailwayLine(
            lineId = lineId,
            operatorId = operatorId,
            lineName = lineName,
            lineCode = lineCode,
            lineColorHex = lineColorHex,
            stationIds = lineStations.map { it.stationId },
            routeGeometry = lineStations.map { LatLngPoint(it.latitude, it.longitude) },
            realtimeSupported = realtimeSupported,
            timetableSupported = timetableSupported,
            coverageStatus = coverageStatus,
            unsupportedReason = unsupportedReason
        )
    }

    fun findOperator(operatorId: String): RailwayOperator? =
        operators.firstOrNull { it.operatorId == operatorId || it.odptOperatorId == operatorId }

    fun findLine(lineId: String): RailwayLine? =
        lines.firstOrNull { it.lineId == lineId }

    fun linesForOperator(operatorId: String): List<RailwayLine> =
        lines.filter { it.operatorId == operatorId }

    fun stationsForLine(lineId: String): List<RailwayStation> =
        stations.filter { lineId in it.lineIds }

    fun findStationByIdOrName(idOrName: String): RailwayStation? {
        val clean = idOrName.trim()
        val tail = clean.substringAfterLast(".")
        return stations.firstOrNull {
            it.stationId == clean ||
                it.stationId.endsWith(".$tail") ||
                it.stationName == clean ||
                it.stationName == tail
        }
    }
}
