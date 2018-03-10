package eu.europeana.iiif;

import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.web.ManifestController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests if the ManifestController returns the correct (version of) responses with the appropriate headers
 * @author Patrick Ehlert
 * Created on 06-03-2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:iiif-test.properties")
@WebMvcTest(ManifestController.class)
public class ManifestControllerTest {

    private static final String JSONLD_V2_OUTPUT = "{Manifest : JSONLD-V2}";
    private static final String JSONLD_V3_OUTPUT = "{Manifest : JSONLD-V3}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManifestService manifestService;

    @Before
    public void setup() throws Exception {
        // mock v2 and v3 manifest responses
        ManifestV2 manifest2 = new ManifestV2("/1/2");
        ManifestV3 manifest3 = new ManifestV3("/1/2");
        given(manifestService.getRecordJson("/1/2", "test")).willReturn("testJson");
        given(manifestService.getRecordJson("/1/2", "test", null)).willReturn("testJson");
        given(manifestService.generateManifestV2("testJson")).willReturn(manifest2);
        given(manifestService.generateManifestV3("testJson")).willReturn(manifest3);
        given(manifestService.serializeManifest((Object) manifest2)).willReturn(JSONLD_V2_OUTPUT);
        given(manifestService.serializeManifest((Object) manifest3)).willReturn(JSONLD_V3_OUTPUT);
    }

    /**
     * Basic manifest test (no parameters)
     * Default we expect a v2 manifest
     */
    @Test
    public void testManifest() throws Exception {
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(JSONLD_V2_OUTPUT));
    }

    /**
     * Test if (the correct) error is thrown if no api key is present
     */
    @Test
    public void testManifestNoApikey() throws Exception {
        this.mockMvc.perform(get("/presentation/1/2/manifest"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test if we handle accept headers properly
     * @throws Exception
     */
    @Test
    public void testManifestAcceptHeader() throws Exception {
        // first try v2 header
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\""))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")))
                .andExpect(content().json(JSONLD_V2_OUTPUT));

        // then try v3
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/ld+json;profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\""))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")))
                .andExpect(content().json(JSONLD_V3_OUTPUT));

        // check if we get v2 if there is an unknown, but otherwise valid accept
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/json;profile=X"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")))
                .andExpect(content().json(JSONLD_V2_OUTPUT));

        // finally check if we get a 406 for unsupported accept headers
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/xml"))
                .andExpect(status().isNotAcceptable());
    }

    /**
     * Check if we get the proper cross origin headers
     * @throws Exception
     */
    @Test
    public void testManifestCrossOrigin() throws Exception {
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")
                .header("Origin", "test"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", anyOf(is("test"), is("*"))));
    }
}
