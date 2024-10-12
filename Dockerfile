
FROM openjdk:latest
COPY ./target/classes/Grp5Assessment /tmp/Grp5Assessment
WORKDIR /tmp
ENTRYPOINT ["java", "Grp5Assessment.Main"]
