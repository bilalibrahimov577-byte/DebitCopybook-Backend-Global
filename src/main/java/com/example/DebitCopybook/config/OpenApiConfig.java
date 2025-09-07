package com.example.DebitCopybook.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Borc Dəftəri API", version = "v1"))
@SecurityScheme(
        name = "X-API-KEY", // Header-in adı
        type = SecuritySchemeType.APIKEY, // Təhlükəsizlik növü
        in = SecuritySchemeIn.HEADER, // Açarın harada göndiriləcəyi
        description = "Bütün sorğular üçün təhlükəsizlik açarını daxil edin"
)
public class OpenApiConfig {
}