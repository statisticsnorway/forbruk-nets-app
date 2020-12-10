FROM eu.gcr.io/prod-bip/alpine-jdk15-buildtools:master-7744b1c6a23129ceaace641d6d76d0a742440b58 as build

# Add the service itself
COPY ./target/forbruk-nets-app.jar /usr/share/forbruk/

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "/usr/share/forbruk/forbruk-nets-app.jar"]

EXPOSE 8080
