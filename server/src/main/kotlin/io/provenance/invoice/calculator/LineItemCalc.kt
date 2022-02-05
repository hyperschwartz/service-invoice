package io.provenance.invoice.calculator

import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.util.extension.toBigDecimalI
import io.provenance.invoice.util.extension.toUuidI
import java.math.BigDecimal
import java.util.UUID

data class LineItemCalc(
    val uuid: UUID,
    val name: String,
    val description: String,
    val quantity: Int,
    val price: BigDecimal,
    val total: BigDecimal,
) {
    companion object {
        fun fromProto(lineItem: LineItem): LineItemCalc {
            val price = lineItem.price.toBigDecimalI()
            return LineItemCalc(
                uuid = lineItem.lineUuid.toUuidI(),
                name = lineItem.name,
                description = lineItem.description,
                quantity = lineItem.quantity,
                price = price,
                total = lineItem.quantity.toBigDecimal() * price,
            )
        }
    }
}
