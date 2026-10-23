package com.exam.server;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 接口文档基本信息。 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI examOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("考试管理系统 API")
                        .description("以班级/角色为组织单元，独立题库 + 灵活组卷 + 规范考务")
                        .version("0.1.0")
                        .license(new License().name("实训项目")));
    }
}
