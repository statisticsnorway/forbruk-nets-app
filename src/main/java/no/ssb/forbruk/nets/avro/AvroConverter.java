package no.ssb.forbruk.nets.avro;

import no.ssb.avro.convert.csv.CsvToRecords;
import no.ssb.forbruk.nets.sftp.SftpFileTransfer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AvroConverter {

    private static final Logger logger = LoggerFactory.getLogger(AvroConverter.class);
    private Schema schema;

    public AvroConverter() {
    }

    public AvroConverter(String schemaName) {
        try {
            this.schema = getSchema(schemaName);
            logger.info("schema: {}", schema.toString());
        } catch (IOException e) {
            logger.info("Error in creating schema for {}: {}", schemaName, e.toString());
        }
    }

    public List<GenericRecord> convertCsvToAvro(String filename, String schemaName, String delimiter) throws IOException {
        this.schema = getSchema(schemaName);
        return convertCsvToAvro(filename, delimiter);
    }

    public List<GenericRecord> convertCsvToAvro(String filename, String delimiter) throws IOException {
        InputStream csvInputStream = getCsvFile(filename);
        return convertCsvToAvro(csvInputStream, delimiter);
    }

    public List<GenericRecord> convertCsvToAvro(InputStream csvInputStream, String delimiter) throws IOException {
        logger.info("csvInputStream bytes available: {}", csvInputStream.available());
        List<GenericRecord> records = new ArrayList<>();
//        logger.info("call avroconverter for stream {}", new String(csvInputStream.readAllBytes()));
        try (CsvToRecords csvToRecords = new CsvToRecords(csvInputStream, schema, Map.of("delimiters", delimiter))) {
            csvToRecords.forEach(records::add);
        }
        logger.info("records converted: {}", records.size());
        return records;
    }

    private Schema getSchema(String schemaFileName) throws IOException {
        return new Schema.Parser().parse(getClass().getClassLoader().getResourceAsStream(schemaFileName));
    }

    private InputStream getCsvFile(String fileName) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }

    public Schema getSchema() {
        return this.schema;
    }
}
