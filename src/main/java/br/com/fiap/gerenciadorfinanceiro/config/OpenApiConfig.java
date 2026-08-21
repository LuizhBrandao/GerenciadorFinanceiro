package br.com.fiap.gerenciadorfinanceiro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gerenciador Financeiro REST API")
                        .version("v1.0.0")
                        .description("API RESTful para controle financeiro, gestão de contas, categorias, transações, orçamentos e relatórios, aplicando boas práticas de POO, SOLID e Design Patterns.")
                        .contact(new Contact()
                                .name("FIAP - Pós Tech")
                                .email("contato@fiap.com.br")
                                .url("https://postech.fiap.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
