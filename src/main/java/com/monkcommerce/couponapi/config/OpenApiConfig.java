package com.monkcommerce.couponapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Monk Commerce Coupon API")
                        .description("REST API for managing and applying discount coupons (cart-wise, product-wise, BxGy)")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("https://example.com"));
    }
}


