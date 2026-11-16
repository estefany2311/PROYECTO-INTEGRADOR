# T-307: imagen del módulo Docente para desplegar en Render, Railway o Docker.
# Construir desde backend/usuario/docente:
#   docker build -t docente-api .
#   docker run --rm -p 8081:8081 --env-file .env docente-api

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# dependencias primero: si solo cambia el código, esta capa se reutiliza
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# usuario sin privilegios
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/*.jar app.jar
USER spring

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
