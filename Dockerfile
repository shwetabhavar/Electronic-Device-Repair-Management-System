FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src /app/src
COPY web /app/web

RUN javac src/Main.java

EXPOSE 8082

CMD ["java", "-cp", "src", "Main"]