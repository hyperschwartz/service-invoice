package io.provenance.invoice.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import feign.Feign
import feign.Logger
import feign.Request
import feign.Response
import feign.RetryableException
import feign.Retryer
import feign.codec.ErrorDecoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import mu.KLogging

// Shared functionality to allow configurations to be generated in non-server environemnts (testing)
object ConfigurationUtil {
    fun getDefaultFeignBuilder(mapper: ObjectMapper): Feign.Builder = Feign.builder()
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

    fun getObjectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .registerModule(ProtobufModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    // This object should be used across the application anywhere spring is not accessible.  Otherwise, just inject the
    // Spring bean to get it
    val DEFAULT_OBJECT_MAPPER: ObjectMapper by lazy { getObjectMapper() }
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
