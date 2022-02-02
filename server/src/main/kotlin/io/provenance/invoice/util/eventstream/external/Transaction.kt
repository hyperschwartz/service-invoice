package io.provenance.invoice.util.eventstream.external

import com.fasterxml.jackson.annotation.JsonProperty

data class TxResultResponse(
    val code: Int?,
    val data: String?,
    val log: String,
    val info: String,
    val gasWanted: Long,
    val gasUsed: Long,
    val events: List<Event>
)

data class TxResultMetaResponse(
    val hash: String,
    val height: Long,
    val index: Int,
    @JsonProperty("tx_result") val txResult: TxResultResponse?,
    val tx: String,
    val data: String?
)
