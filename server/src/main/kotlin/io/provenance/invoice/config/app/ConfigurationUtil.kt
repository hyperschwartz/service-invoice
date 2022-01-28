package io.provenance.invoice.config.app

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.protobuf.util.JsonFormat.TypeRegistry
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import feign.Feign
import feign.Logger
import feign.Request
import feign.RequestInterceptor
import feign.RequestTemplate
import feign.Response
import feign.RetryableException
import feign.Retryer
import feign.codec.ErrorDecoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import io.provenance.invoice.config.web.AppHeaders
import io.provenance.metadata.v1.MsgWriteContractSpecificationRequest
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteRecordSpecificationRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteScopeSpecificationRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import mu.KLogging

// Shared functionality to allow configurations to be generated in non-server environments (testing)
object ConfigurationUtil {
    /**
     * Generates the default Feign configurations for a builder to take a properly-declared interface and convert it to
     * a Feign REST client.
     *
     * @param mapper The object mapper to use for encoding/decoding JSON as per the requests' expected results.
     * @param apiKey OPTIONAL.  If provided, an apikey header will be added to all requests.
     */
    fun getDefaultFeignBuilder(
        mapper: ObjectMapper,
        apiKey: String? = null,
    ): Feign.Builder = Feign.builder()
            // 1 sec connection timeout, 1 min read timeout
        .options(Request.Options(1000, 60000))
        .logLevel(Logger.Level.BASIC)
        .logger(FeignLogger())
            // 500ms minimum retry period,  2min max retry period, 10 max attempts
        .retryer(Retryer.Default(500, 120000, 10))
        .decode404()
        .encoder(JacksonEncoder(mapper))
        .decoder(JacksonDecoder(mapper))
        .errorDecoder(FeignErrorDecoder())
        .also { builder ->
            apiKey?.also { a -> builder.requestInterceptor(ApiKeyRequestInterceptor(a)) }
        }

    private fun getObjectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .registerModule(ProtobufModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun getProvenanceTypeRegistry(): TypeRegistry = TypeRegistry.newBuilder()
        .add(MsgWriteContractSpecificationRequest.getDescriptor())
        .add(MsgWriteScopeSpecificationRequest.getDescriptor())
        .add(MsgWriteScopeRequest.getDescriptor())
        .add(MsgWriteSessionRequest.getDescriptor())
        .add(MsgWriteRecordSpecificationRequest.getDescriptor())
        .add(MsgWriteRecordRequest.getDescriptor())
        .build()

    // This object should be used across the application anywhere spring is not accessible.  Otherwise, just inject the
    // Spring bean to get it
    val DEFAULT_OBJECT_MAPPER: ObjectMapper by lazy { getObjectMapper() }

    val DEFAULT_PROVENANCE_TYPE_REGISTRY: TypeRegistry by lazy { getProvenanceTypeRegistry() }
}

class FeignLogger : Logger() {
    private companion object : KLogging()

    override fun log(configKey: String?, format: String?, vararg args: Any?) {
        logger.info(String.format(methodTag(configKey) + format, *args))
    }
}

// Grants us some nice displays for error output
class FeignErrorDecoder : ErrorDecoder.Default() {
    override fun decode(methodKey: String?, response: Response?): Exception =
        when (response?.status()) {
            502 -> RetryableException("502: Bad Gateway", response.request().httpMethod(), null)
            503 -> RetryableException("503: Service Unavailable", response.request().httpMethod(), null)
            504 -> RetryableException("504: Gateway Timeout", response.request().httpMethod(), null)
            else -> super.decode(methodKey, response)
        }
}

/**
 * Automatically applies the api key to each request made with the feign REST client.
 */
class ApiKeyRequestInterceptor(private val apiKey: String): RequestInterceptor {
    override fun apply(template: RequestTemplate?) {
        template?.header(AppHeaders.API_KEY, apiKey)
    }
}
