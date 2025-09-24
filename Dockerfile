# Estágio 1: Build da aplicação com Maven
# Usamos uma imagem que já vem com JDK 21 e Maven
FROM maven:3.9-eclipse-temurin-21 AS builder

# Define o diretório de trabalho dentro do contêiner
WORKDIR /build

# Copia o pom.xml primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o resto do código fonte
COPY src ./src

# Executa o build do Maven para gerar o arquivo .jar
RUN mvn clean package -DskipTests

# Estágio 2: Criação da imagem final de execução
# Usamos uma imagem leve, apenas com o necessário para rodar Java (JRE)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o .jar gerado no estágio anterior para a imagem final
COPY --from=builder /build/target/*.jar app.jar

# Expõe a porta 8080 para que possamos acessá-la de fora do contêiner
EXPOSE 8080

# Comando para iniciar a aplicação quando o contêiner for executado
ENTRYPOINT ["java", "-jar", "app.jar"]