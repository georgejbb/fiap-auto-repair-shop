# Estágio de build: compila e empacota a aplicação no layout fast-jar do Quarkus.
# Os testes não rodam aqui; rodam no pipeline de CI, que tem acesso ao Docker para o banco de teste.
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /build

# Dependências primeiro, para reaproveitar a camada em cache quando só o código muda.
COPY pom.xml .
COPY .mvn/jvm.config .mvn/jvm.config
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q package -DskipTests

# Estágio de execução: somente o JRE e o artefato, com usuário sem privilégio.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /build/target/quarkus-app/lib/ lib/
COPY --from=build --chown=app:app /build/target/quarkus-app/*.jar ./
COPY --from=build --chown=app:app /build/target/quarkus-app/app/ app/
COPY --from=build --chown=app:app /build/target/quarkus-app/quarkus/ quarkus/

USER app
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/q/health/live || exit 1

ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
