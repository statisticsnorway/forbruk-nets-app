package no.ssb.forbruk.nets.filehandle.storage.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;;

public class ManifestTest {
    private static final Logger logger = LoggerFactory.getLogger(ManifestTest.class);

    final static String expectedManifest = "[{" +
            "metadata: {" +
                "topic:\"testTopic\", position:\"pos1\", resource-type:\"entry\"," +
                "content-key:\"entry\", source:\"nets\", dataset:\"bank-transactions\", tag:\"2020\"," +
                "description:\"Some description\", charset:\"UTF-8\", content-type:\"text/csv\"," +
                "content-length:30" +
            "}," +
            "schema:{" +
                "source-path:\"mem://test-block\", source-fil:\"testFilnavn\", source-charset:\"UTF-8\"," +
                "delimiter:\";\", record-type:\"entry\"," +
                "fields:" +
                    "[{name:\"KOL1\", mapped-name:\"Kol1\", data-type:\"String\"}," +
                     "{name:\"KOL2\", mapped-name:\"Kol2\", data-type:\"String\"}," +
                     "{name:\"KOL3\", mapped-name:\"Kol3\", data-type:\"String\"}]" +
            "}" +
        "}]";

    static JSONObject expectedMantifestMetadataJson;
    static JSONObject expectedMantifestSchemaJson;

    @BeforeAll
    static void init()  throws JSONException {
        expectedMantifestMetadataJson = new JSONObject( "{" +
                        "topic:\"testTopic\", position:\"pos1\", resource-type:\"entry\"," +
                        "content-key:\"entry\", source:\"nets\", dataset:\"bank-transactions\", tag:\"2020\"," +
                        "description:\"Some description\", charset:\"UTF-8\", content-type:\"text/csv\"," +
                        "content-length:30, created-date: \"2020-12-01T12:17:39.633073Z\"" +
                        "}");
        expectedMantifestSchemaJson = new JSONObject(
                "{" +
                        "source-path:\"mem://test-block\", source-file:\"testFilnavn\", source-charset:\"UTF-8\"," +
                        "delimiter:\";\", record-type:\"entry\"," +
                        "fields:" +
                        "[{name:\"KOL1\", mapped-name:\"Kol1\", data-type:\"String\"}," +
                        "{name:\"KOL2\", mapped-name:\"Kol2\", data-type:\"String\"}," +
                        "{name:\"KOL3\", mapped-name:\"Kol3\", data-type:\"String\"}]" +
                        "}");
    }

    @Test
    void testManifestIsCreatedOk() throws JSONException{

        String manifest = Manifest.generateManifesJson("testTopic", "pos1", 30,
         "KOL1;KOL2;KOL3".split(";"), "testFilnavn" );

        JSONArray manifestJsonArray = new JSONArray(manifest);
        assertEquals(1, manifestJsonArray.length());
        JSONObject manifestJsonObject = manifestJsonArray.getJSONObject(0);
        assertNotNull(manifestJsonArray);

        assertNotNull(manifestJsonObject.get("schema"));
        assertNotNull(manifestJsonObject.get("metadata"));

        JSONAssert.assertEquals(new JSONObject(manifestJsonObject.get("schema").toString())
                , expectedMantifestSchemaJson, JSONCompareMode.LENIENT);
        // since date is set to sysdate, it has to be changed - the rest is left untouched.
        JSONAssert.assertEquals((new JSONObject(manifestJsonObject.get("metadata").toString())).put("created-date", "2020-12-01T12:17:39.633073Z")
                ,expectedMantifestMetadataJson, JSONCompareMode.LENIENT);


    }
}
