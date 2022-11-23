package eu.europeana.iiif.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    private final BuildInfo buildInfo;

    /**
     * Initialize SpringDoc with API build information
     * @param buildInfo object for retrieving build information
     */
    public SpringDocConfig(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI().info(new Info().title(buildInfo.getAppName())
                        .description(buildInfo.getAppDescription())
                        .version(buildInfo.getAppVersion() + " (build " + buildInfo.getBuildNumber() + ")")
                        .contact(new Contact().name("API team").url("https://api.europeana.eu").email("api@europeana.eu"))
                        .termsOfService("https://www.europeana.eu/en/rights/api-terms-of-use")
                        .license(new License().name("EUPL 1.2").url("https://www.eupl.eu")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation")
                        .url("https://pro.europeana.eu/page/intro#general"));
    }

}
