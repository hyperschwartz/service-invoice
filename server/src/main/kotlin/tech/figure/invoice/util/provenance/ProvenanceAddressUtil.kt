package tech.figure.invoice.util.provenance

import io.provenance.scope.util.Bech32
import java.security.MessageDigest

object ProvenanceAddressUtil {
    private const val MARKER_ENCODING_PREFIX: String = "marker/"
    private const val MARKER_ENCODING_TRUNCATE_LENGTH: Int = 20

    /**
     * Takes a source denomination for a marker and an account address from the target blockchain environment and
     * computes a marker address.
     *
     * @param denom The globally-unique marker denomination.
     * @param accountAddress An address for an account in the provennce blockchain environment. Should be prefixed with
     *                       pb or tp, generally.
     */
    fun generateMarkerAddressForDenomFromSource(
        denom: String,
        accountAddress: String,
    ): String = generateMarkerAddressForDenom(
        denom = denom,
        hrp = Bech32.decode(accountAddress).hrp,
    )

    /**
     * Takes a source denomination for a marker and an hrp and computes a marker address.
     *
     * @param denom The globally-unique marker denomination.
     * @param hrp The "human readable prefix" to the address bech.
     */
    fun generateMarkerAddressForDenom(
        denom: String,
        hrp: String,
    ): String = MessageDigest
        .getInstance("SHA-256")
        .digest("$MARKER_ENCODING_PREFIX$denom".toByteArray())
        .take(MARKER_ENCODING_TRUNCATE_LENGTH)
        .toByteArray()
        .let { truncatedBytes -> Bech32.encode(hrp = hrp, eightBitData = truncatedBytes) }
}
