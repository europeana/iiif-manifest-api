package eu.europeana.iiif;

import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.web.ManifestController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Main application and configuration
 *
 * @author Patrick Ehlert
 * Created on 6-12-2017
 */
@SpringBootApplication
@PropertySource("classpath:iiif.properties")
@PropertySource(value = "classpath:iiif.user.properties", ignoreResourceNotFound = true)
public class IiifApplication extends SpringBootServletInitializer {

	//TODO figure out why corsConfigurer below doesn't work (however @CrossOrigin annotation on the controller method does work)

	/**
	 * Setup CORS for manifest requests
	 * @return
	 */
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("*").allowedOrigins("*").maxAge(1000);
			}
		};
	}

	/**
	 * Manifest service that does all the 'dirty work'; retrieving records, converting to data, serializing to json-ld
	 * @return
	 */
	@Bean
	public ManifestService manifestService() {
		return new ManifestService();
	}

	/**
	 * Rest controller that handles manifest requests
	 * @return
	 */
	@Bean
	public ManifestController manifestController() {
		return new ManifestController(manifestService());
	}

	/**
	 * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
	 * @param args
	 */
	@SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
	public static void main(String[] args) {
		SpringApplication.run(IiifApplication.class, args);
	}



}
