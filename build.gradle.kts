plugins {
    java
    jacoco
}

group = "br.com.fiap.pos"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of("${project.property("javaVersion")}")
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))

    implementation("com.amazonaws:aws-lambda-java-core:${property("awsLambdaCoreVersion")}")
    implementation("com.amazonaws:aws-lambda-java-events:${property("awsLambdaEventsVersion")}")

    // Mesma stack de JWT do core (NimbusJwtEncoder / JwtClaimsSet / RsaKeyConverters), sem contexto Spring Boot
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("tools.jackson.core:jackson-databind")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}"))
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mesmos overrides de versão do tech-challenge-core (correções acima do BOM)
    constraints {
        implementation("tools.jackson.core:jackson-core:3.1.5")
        implementation("tools.jackson.core:jackson-databind:3.1.5")
        implementation("org.postgresql:postgresql:42.7.12")
        testImplementation("org.postgresql:postgresql:42.7.12")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

// Pacote no formato esperado pelo runtime java21 da Lambda: classes na raiz + dependências em lib/
val buildZip by tasks.registering(Zip::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Gera o pacote ZIP da Lambda."
    archiveFileName = "auth-lambda.zip"
    destinationDirectory = layout.buildDirectory.dir("distributions")
    from(tasks.compileJava)
    from(tasks.processResources)
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}

tasks.build {
    dependsOn(buildZip)
}
