FROM azul/zulu-openjdk:15

# Add the service itself
COPY ./target/nets.jar /usr/share/nets/

ENTRYPOINT ["java", "-jar", "/usr/share/nets/nets.jar"]

EXPOSE 8080
