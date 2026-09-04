# 1. Gunakan sistem operasi dasar alpine yang sangat ringan dengan Java 21
FROM eclipse-temurin:21-jre-alpine

# 2. Buat folder /app di dalam kontainer
WORKDIR /app

# 3. Salin fail .jar hasil build Maven ke dalam kontainer
COPY target/*.jar agen46-backend.jar

# 4. Instruksi untuk menyalakan aplikasi saat kontainer dijalankan
ENTRYPOINT ["java", "-jar", "agen46-backend.jar"]
