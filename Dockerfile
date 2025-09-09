# --- STAGE 1: Build ---
# JDK olan bir mühit götürürük və ona "builder" adını veririk
FROM eclipse-temurin:21-jdk-jammy AS builder

# İş qovluğunu təyin edirik
WORKDIR /app

# Gradle fayllarını kopyalayırıq
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# İcra icazəsi veririk
RUN chmod +x ./gradlew

# Proyektin mənbə kodunu kopyalayırıq
COPY src src

# Proqramı build edirik (bu dəfə JDK olduğu üçün işləyəcək)
RUN ./gradlew build -x test

# --- STAGE 2: Run ---
# İndi daha kiçik, JRE olan bir mühit götürürük
FROM eclipse-temurin:21-jre-jammy

# İş qovluğunu təyin edirik
WORKDIR /app

# ƏN VACİB HİSSƏ: "builder" adlı birinci mühitdən
# build olunmuş .jar faylını bu yeni mühitə kopyalayırıq
COPY --from=builder /app/build/libs/*.jar app.jar

# Proqramın işləyəcəyi portu bildiririk
EXPOSE 8080

# Proqramı işə salırıq
ENTRYPOINT ["java","-jar","app.jar"]

# Cache temizleme meqsedi ile elave edildi - v1.4