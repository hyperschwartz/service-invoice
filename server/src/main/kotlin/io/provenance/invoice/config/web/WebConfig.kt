package io.provenance.invoice.config.web

import io.provenance.invoice.config.app.ServiceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig {
    @Bean
    fun webRequestInterceptorConfig(props: ServiceProperties): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addInterceptors(registry: InterceptorRegistry) {
            super.addInterceptors(registry)
            registry.addInterceptor(WebRequestLoggingInterceptor())
        }

        override fun addCorsMappings(registry: CorsRegistry) {
            // Allow CORS from localhost:3000 on all routes during development.  Allows the invoice frontend to properly
            // communicate with the backend in a local environment
            if (props.environment == "development") {
                registry.addMapping("/**").allowedOrigins("http://localhost:3000")
            }
        }
    }
}
