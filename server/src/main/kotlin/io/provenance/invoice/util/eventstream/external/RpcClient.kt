package io.provenance.invoice.util.eventstream.external

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.Param
import feign.RequestLine
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder

interface RpcClient {

    @RequestLine("GET /")
    fun blockchainInfo(request: BlockchainInfoRequest): RpcResponse<BlockchainInfoResponse>

    @RequestLine("GET /")
    fun block(request: BlockRequest): RpcResponse<BlockResponse>

    @RequestLine("GET /")
    fun blockResults(request: BlockResultsRequest): RpcResponse<BlockResultsResponse>

    @RequestLine("GET /")
    fun abciInfo(request: RpcRequest = RpcRequest("abci_info")): RpcResponse<AbciInfoMetaResponse>

    @RequestLine("GET /tx?hash=0x{hash}")
    fun getTransaction(@Param("hash") hash: String): RpcResponse<TxResultMetaResponse>

    class Builder(
        private val url: String,
        private val objectMapper: ObjectMapper
    ) {
        fun build(): RpcClient = Feign.builder()
            .encoder(JacksonEncoder(objectMapper))
            .decoder(JacksonDecoder(objectMapper))
            .target(RpcClient::class.java, url)
    }
}

fun RpcClient.fetchTransaction(txHash: String): TxResultMetaResponse =
    getTransaction(txHash).let {
        val errorMessage = it.error?.run { "$message - $data" }
        when {
            errorMessage != null && it.result?.txResult == null -> throw TransactionNotFoundException(errorMessage)
            errorMessage != null -> throw TransactionQueryException(errorMessage)
            else -> it.result!!
        }
    }

fun RpcClient.fetchBlocksWithTransactions(minHeight: Long, maxHeight: Long): List<Long> =
    fetchBlockchainInfo(minHeight, maxHeight).blockMetas.filter { it.numTxs > 0 }.map { it.header.height }

fun RpcClient.fetchBlockchainInfo(minHeight: Long, maxHeight: Long): BlockchainInfoResponse =
    blockchainInfo(BlockchainInfoRequest(minHeight, maxHeight)).result!!

fun RpcClient.fetchBlock(height: Long): BlockResponse = block(BlockRequest(height)).result!!

fun RpcClient.fetchBlockResults(height: Long): BlockResultsResponse = blockResults(BlockResultsRequest(height)).result!!

fun RpcClient.fetchAbciInfo(): AbciInfoResponse = abciInfo().result!!.response
