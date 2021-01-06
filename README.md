# Forbruk-nets-app
Collect and handle csv-files containing transactions from NETS

## Boot-up
To run this locally, you need a private-key file holding the nets secret and refer to this in an application-dev.yaml.
Do also use another topic (google.storage.provider.topic) e.g. test-transactions or test-transactions-<init> in dev-properties.
In e.g. IntelliJ, use commandline 'spring-boot:run -Dspring-boot.run.profiles=dev'

## Features
The application uses spring-scheduling. At scheduled time, it connects to Nets and collects all names of file in a spesific directory.
For each filename, it gets the file as a fileinputstream which uses RawdataProducer to create and publish encrypted messages - one message per line in file.
The messages are published to the topic given in propertiesfile.
After each successful handeled file, the name of file ist stored in a table.

## Google Cloud Platform

### Storage
The nets-transactions are stored in a Google Cloud bucket named nets-rawdata-transactions
### SQL
The name of successfully handeled file, is saved in Google Cloud SQL named nets-sqlinstance-forbruk
### Secrets
There are two secrets used in connection to Nets - NETS_PASSPHRASE and NETS_SECRET
Thare are two secrets used in encryption of raawdata-messages - forbruk_nets_encryption_key and forbruk_nets_encryption_salt
(Berglas is used to retrieve secrets.)




