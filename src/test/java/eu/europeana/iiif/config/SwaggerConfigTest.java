package eu.europeana.iiif.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JUnit test to check if Swagger is setup fine
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SwaggerConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test if the api-docs endpoint is available and if CORS is enabled for it
     */
    @Test
    public void testApiDocEndpoint() throws Exception {
        mockMvc.perform(get("/v2/api-docs")
                .header(HttpHeaders.ORIGIN, "https://test.com"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))

                // check if api info is loaded properly and contains minimally contact email and license information
                .andExpect(jsonPath("$.info.contact").exists())
                .andExpect(jsonPath("$.info.license.name").value("EUPL 1.2"));
    }

    /**
     * Test if Swagger UI is available
     */
//    @Ignore("TODO: Find out why this doesn't pass")
//    @Test
//    public void testSwaggerUI() throws Exception {
//        mockMvc.perform(get("/swagger-ui/"))
//                .andExpect(status().is(HttpStatus.OK.value()));
//    }

}
