# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies first (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Build the application
COPY src ./src
RUN mvn package -DskipTests -B -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create upload directories and non-root user
RUN addgroup -S lms && adduser -S lms -G lms \
 && mkdir -p /app/uploads/avatars /app/uploads/covers /app/uploads/videos /app/uploads/pdfs /app/uploads/blog-covers \
 && chown -R lms:lms /app/uploads \
 && mkdir -p /tmp/spring-uploads && chown lms:lms /tmp/spring-uploads

COPY --from=builder /app/target/*.jar app.jar
RUN chown lms:lms /app/app.jar

USER lms

VOLUME /app/uploads

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "-Djava.io.tmpdir=/tmp/spring-uploads", "app.jar"]
