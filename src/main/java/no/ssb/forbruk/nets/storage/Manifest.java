package no.ssb.forbruk.nets.storage;

import java.nio.charset.StandardCharsets;

public class Manifest {
    static MetadataContent.Builder metadataContentBuilder = new MetadataContent.Builder();

    static byte[] generateManifest (String topic, String position, int contentLength, String[] headerColumns) {

        metadataContentBuilder.topic(topic)
                .position(position)
                .resourceType("entry")
                .contentKey("entry")
                .source("nets")
                .dataset("bank-transactions")
                .tag("2020")
                .description("Some description")
                .charset(StandardCharsets.UTF_8.displayName())
                .contentType("text/csv")
                .contentLength(contentLength)
                .markCreatedDate();

        // store csv/avro mapping
        metadataContentBuilder
                .sourcePath("mem://test-block")
                .sourceFile("CSV_DATA")
                .sourceCharset(StandardCharsets.UTF_8.name())
                .delimiter(";")
                .recordType(MetadataContent.RecordType.ENTRY);

        for (String headerColumn : headerColumns) {
            String avroColumn = MetadataContent.formatAsAvroColumn(headerColumn); // please notice: format CSV Header Column as Avro compatible column
            metadataContentBuilder.csvMapping(headerColumn, avroColumn);
        }

        return metadataContentBuilder.build().toJSON().getBytes();
    }
}
