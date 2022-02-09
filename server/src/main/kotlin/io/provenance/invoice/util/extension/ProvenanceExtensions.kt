package io.provenance.invoice.util.extension

import io.provenance.invoice.util.enums.StringTypeConverterEnum
import io.provenance.invoice.util.eventstream.external.Event
import io.provenance.invoice.util.eventstream.external.StreamEvent
import io.provenance.invoice.util.eventstream.external.TxResultMetaResponse
import io.provenance.invoice.util.provenance.PayableContractKey
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.scope.util.MetadataAddress

inline fun <reified T: Any> StreamEvent.attributeValueOrNullI(key: PayableContractKey): T? = attributes
    .singleOrNull { it.key == key.contractName }
    ?.let { StringTypeConverterEnum.convertStringOrNull(it.value) }

inline fun <reified T: Any> StreamEvent.attributeValueI(key: PayableContractKey): T = attributes
    .singleOrNull { it.key == key.contractName }
    .checkNotNullI { "No single key of type [${key.contractName}] existed for event [${this.txHash}]" }
    .let { StringTypeConverterEnum.convertStringOrNull<T>(it.value) }
    .checkNotNullI { "Unable to convert key [${key.name}] in event [${this.txHash}] to type [${T::class.qualifiedName}]" }

fun MsgWriteScopeRequest.scopeIdI(): String = MetadataAddress.forScope(scopeUuid.toUuidI()).toString()

fun Event.toStreamEventI(txResponse: TxResultMetaResponse): StreamEvent = StreamEvent(
    height = txResponse.height,
    eventType = this.type,
    attributes = this.attributes,
    resultIndex = txResponse.index,
    txHash = txResponse.hash,
)
