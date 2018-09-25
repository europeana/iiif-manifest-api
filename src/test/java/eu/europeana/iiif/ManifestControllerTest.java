package eu.europeana.iiif;

import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.EdmDateUtils;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.web.ManifestController;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String TIMESTAMP_UPDATE = "Wed, 28 Oct 2015 07:28:00 GMT";
    private static final String TIMESTAMP_AFTER  = "Tue, 12 Jul 2016 11:07:32 GMT";
    private static final String TIMESTAMP_BEFORE = "Wed, 18 Apr 2012 04:54:16 GMT";
    private static final String SAME_ETAG_HEADER = "10a4cd320ab0cd3c47a0708df23d67df3e4b4ee7a66443ece353fea383557cd7";
    private static final String FAUX_ETAG_HEADER = "ca3d67df3ee77ece353fd070203c47a43ea383558df10a4ab0cd2ce4b46d7643";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManifestService manifestService;

    @Before
    public void setup() throws Exception {
        // mock v2 and v3 manifest responses
        ManifestV2 manifest2 = new ManifestV2("/1/2", "/1/2");
        ManifestV3 manifest3 = new ManifestV3("/1/2", "/1/2");
        given(manifestService.getRecordJson("/1/2", "test")).willReturn("testJson");
        given(manifestService.getRecordJson("/1/2", "test", null)).willReturn("testJson");
        given(manifestService.generateManifestV2(eq("testJson"), anyBoolean(), any())).willReturn(manifest2);
        given(manifestService.generateManifestV3(eq("testJson"), anyBoolean(), any())).willReturn(manifest3);
        given(manifestService.serializeManifest(manifest2)).willReturn(JSONLD_V2_OUTPUT);
        given(manifestService.serializeManifest(manifest3)).willReturn(JSONLD_V3_OUTPUT);
        given(manifestService.getSHA256Hash(any())).willReturn(SAME_ETAG_HEADER);
        given(manifestService.getTimestampUpdate()).willReturn(EdmDateUtils.headerStringToDate(TIMESTAMP_UPDATE));
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
                .andExpect(header().string("eTag", notNullValue()))
                .andExpect(content().json(JSONLD_V2_OUTPUT));

        // then try v3
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                .header("Accept", "application/ld+json;profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\""))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")))
                .andExpect(header().string("eTag", notNullValue()))
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
     * Test if the controller also returns the proper Content-type if the version is passed through the 'format' GET
     * parameter instead of via the Accept Header (fixed in #EA-978_fix+EA-1200-changes)
     * @throws Exception
     */
    @Test
    public void testContentTypeHeaderWithFormat() throws Exception {
        // first try format=2
        this.mockMvc.perform(get("/presentation/1/2/manifest")
                                     .param("wskey", "test")
                                     .param("format", "2")
                                     .header("Accept", "application/ld+json"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")))
                    .andExpect(header().string("eTag", notNullValue()))
                    .andExpect(content().json(JSONLD_V2_OUTPUT));

        // then try format=3
        this.mockMvc.perform(get("/presentation/1/2/manifest")
                                     .param("wskey", "test")
                                     .param("format", "3")
                                     .header("Accept", "application/ld+json"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")))
                    .andExpect(header().string("eTag", notNullValue()))
                    .andExpect(content().json(JSONLD_V3_OUTPUT)).andDo(print());
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
                .andExpect(header().string("Access-Control-Allow-Origin", anyOf(is("test"), is("*"))))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Allow")));
    }

    /**
     * Check if the If-Modified-Since header is handled properly
     * @throws Exception
     */
    @Test
    public void testManifestIfModifiedSince() throws Exception {
        // test after last-modified date
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")
                    .header("If-Modified-Since", TIMESTAMP_AFTER))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string("Last-Modified", equalTo(TIMESTAMP_UPDATE)));
        // test before last-modified date
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")
                    .header("If-Modified-Since", TIMESTAMP_BEFORE))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Last-Modified", equalTo(TIMESTAMP_UPDATE)));
    }

    /**
     * Check if the If-None-Match header is handled properly
     * @throws Exception
     */
    @Test
    public void testManifestIfNoneMatch() throws Exception {
        // retrieve eTag value
        MvcResult result = this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\""))
                    .andReturn();
        String eTag = StringUtils.remove(result.getResponse().getHeader("eTag"), "\"");

        // supply the same eTag value, working around quoting hell, expect HTTP 304
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")
                    .header("If-None-Match", eTag))
                    .andExpect(status().isNotModified())
                    .andExpect(header().string("eTag", equalTo("\"" + SAME_ETAG_HEADER + "\"")));

        // supply another eTag value will result in HTTP 200
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V2+"\"")
                    .header("If-None-Match", FAUX_ETAG_HEADER))
                    .andExpect(status().isOk())
                    .andExpect(header().string("eTag", equalTo("\"" + SAME_ETAG_HEADER + "\"")));
    }


    /**
     * Check if the If-Match header is handled properly
     * @throws Exception
     */
    @Test
    public void testManifestIfMatch() throws Exception {
        // retrieve eTag value
        MvcResult result = this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\""))
                    .andReturn();
        String eTag = StringUtils.remove(result.getResponse().getHeader("eTag"), "\"");

        // supply the same eTag value, working around quoting hell, expect HTTP 200
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")
                    .header("If-Match", eTag))
                    .andExpect(status().isOk())
                    .andExpect(header().string("eTag", equalTo("\"" + SAME_ETAG_HEADER + "\"")));

        // supply another eTag value will result in HTTP 412
        this.mockMvc.perform(get("/presentation/1/2/manifest").param("wskey", "test")
                    .header("Accept", "application/json; profile=\""+Definitions.MEDIA_TYPE_IIIF_V3+"\"")
                    .header("If-Match", FAUX_ETAG_HEADER))
                    .andExpect(status().isPreconditionFailed())
                    .andExpect(header().string("eTag", equalTo("\"" + SAME_ETAG_HEADER + "\"")));
    }
}
