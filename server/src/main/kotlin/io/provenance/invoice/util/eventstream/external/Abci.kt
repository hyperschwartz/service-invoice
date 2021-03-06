package io.provenance.invoice.util.eventstream.external

import com.fasterxml.jackson.annotation.JsonProperty

data class AbciInfoMetaResponse(
    val response: AbciInfoResponse
)

data class AbciInfoResponse(
    val data: String,
    @JsonProperty("last_block_height") val lastBlockHeight: Long,
    @JsonProperty("last_block_app_hash") val lastBlockAppHash: String
)
