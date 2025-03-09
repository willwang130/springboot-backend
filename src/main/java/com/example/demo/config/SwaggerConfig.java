package com.example.demo.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("产品管理 API")
                        .version("1.0")
                        .description("""
                                该 API 提供产品管理的功能，包括增删改查. \n
                                👤 Zixun Wang
                                📧 [wangzx636@163.com](mailto:wangzx636@163.com)
                                🌍 [github.com](https://github.com)
                                """)
                        .contact(new Contact()
                                .name("Zixun Wang"))
                )
                .servers(List.of(
                        new Server().url("http://localhost").description("本地开发环境"),
                        new Server().url("http://localhost/api").description("生产环境")
                ));
    }
}
