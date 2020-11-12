package no.ssb.forbruk.nets.avro;

import no.ssb.avro.convert.csv.CsvParserSettings;
import no.ssb.avro.convert.csv.CsvToRecords;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class AvroConverterTest {
    private static final Logger logger = LoggerFactory.getLogger(AvroConverterTest.class);

    private AvroConverter avroConverter = new AvroConverter("testNetsTransaction.avsc");

    @Test
    public void testAvroConverterFile() {
        List<GenericRecord> records = new ArrayList<>();
        try {
            logger.info("call avroconverter for testNetsResponse.csv");
            records = avroConverter.convertCsvToAvro("testNetsResponse.csv", ";");
            logger.info("File converted: {}", records);

        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotEquals(0, records.size());
        assertEquals("999999999999", records.get(0).get("KORTINNEH_KONTONR"));
    }
//
//    @Test
//    public void csvWithFunkyColumnNames_convertToGenericRecords_withExplicitlyNamedColumns() throws Exception {
//        String scenario = "with-column-renames";
//        InputStream csvInputStream = data(scenario + ".csv");
//        Schema schema = schema(scenario + ".avsc");
//        List<GenericRecord> records = new ArrayList<>();
//        CsvParserSettings csvParserSettings = new CsvParserSettings().headers(List.of(
//                "renamedCol1Name",
//                "renamedCol2Name",
//                "renamedCol3Name"));
//
//        try (CsvToRecords csvToRecords = new CsvToRecords(csvInputStream, schema, csvParserSettings)) {
//            csvToRecords.forEach(records::add);
//        }
//
//        assertThat(records.size()).isEqualTo(2);
//        assertThat(records.get(0).get("renamedCol1Name")).isEqualTo("Hey ho");
//        assertThat(records.get(0).get("renamedCol2Name")).isEqualTo("42");
//        assertThat(records.get(0).get("renamedCol3Name")).isEqualTo("foo");
//    }
}
