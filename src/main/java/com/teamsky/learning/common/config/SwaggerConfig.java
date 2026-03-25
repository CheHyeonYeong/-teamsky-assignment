package com.teamsky.learning.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TeamSky Learning Platform API")
                        .description("단원별 문제 풀이 및 풀이 이력 조회 API")
                        .version("v1.0.0"));
    }
}
