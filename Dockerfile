FROM azul/zulu-openjdk:15

# Add the service itself
COPY ./target/forbruk-nets-app.jar /usr/share/forbruk/

ENTRYPOINT ["java", "-jar", "/usr/share/forbruk/forbruk-nets-app.jar"]

EXPOSE 8080
