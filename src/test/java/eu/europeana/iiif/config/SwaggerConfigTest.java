package eu.europeana.iiif.config;


import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test to check if Swagger is setup fine
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:iiif-test.properties")
@WebMvcTest(SwaggerConfig.class)
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
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"));
    }

    /**
     * Test if Swagger UI is available
     */
    @Ignore("TODO: Find out why this doesn't pass")
    @Test
    public void testSwaggerUI() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
