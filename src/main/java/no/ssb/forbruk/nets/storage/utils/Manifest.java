package no.ssb.forbruk.nets.storage.utils;

import java.nio.charset.StandardCharsets;

public class Manifest {
    static MetadataContent.Builder metadataContentBuilder = new MetadataContent.Builder();

    public static byte[] generateManifest(String topic, String position, int contentLength,
                                          String[] headerColumns, String filename ) {

        metadataContentBuilder.topic(topic)
                .position(position)
                .resourceType("entry")
                .contentKey("entry")
                .source("nets")
                .dataset("bank-transactions")
                .tag("2018")
                .description("nets transactions 2018")
                .charset(StandardCharsets.UTF_8.displayName())
                .contentType("text/csv")
                .contentLength(contentLength)
                .markCreatedDate();

        // store csv/avro mapping
        metadataContentBuilder
                .sourcePath("mem://test-block")
                .sourceFile(filename)
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
