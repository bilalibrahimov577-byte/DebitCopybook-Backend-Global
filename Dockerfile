# Java 21 olan hazır bir mühit götürürük
FROM eclipse-temurin:21-jre-jammy

# Qutunun içində /app adlı bir iş qovluğu yaradırıq
WORKDIR /app

# Əvvəlcə build üçün lazım olan faylları kopyalayırıq
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew faylına icra icazəsi veririk
RUN chmod +x ./gradlew

# Bütün qalan proyekt fayllarını kopyalayırıq
COPY src src

# Proqramı build edirik (testləri buraxırıq)
RUN ./gradlew build -x test

# Proqramın 8080 portunda işlədiyini bildiririk
EXPOSE 8080

# Və ən sonda, proqramı işə salmaq üçün əmri veririk
ENTRYPOINT ["java","-jar","build/libs/DebitCopybook-0.0.1-SNAPSHOT.jar"]