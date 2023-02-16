plugins {
    java
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.google.cloud.tools.jib") version "3.2.1"
    id("com.sedmelluq.jdaction") version "1.0.99-beta3"
}

group = "io.github.chalkyjeans"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://m2.chew.pro/snapshots")
    maven("https://jcenter.bintray.com")
    maven("https://m2.duncte123.dev/releases")
}

dependencies {
    implementation("com.github.DV8FromTheWorld:JDA:v5.0.0-beta.3")
    implementation("com.github.MinnDevelopment:discord-webhooks:e7e5db7d2d")
    implementation("com.github.chalkyjeans:Lavalink-Client:d848eaf32a") {
        exclude(mapOf("group" to "net.dv8tion", "module" to "JDA"))
    }
    implementation("pw.chew:jda-chewtils:2.0-SNAPSHOT")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.springframework.boot:spring-boot-starter") {
        exclude(mapOf("group" to "ch.qos.logback"))
    }
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

jib {
    val user = System.getenv("DOCKER_USERNAME")
    val pass = System.getenv("DOCKER_PASSWORDTan")
    from {
        image = "amazoncorretto:19"
        auth {
            username = user
            password = pass
        }
    }
    to {
        image = "chalkyjeans/taboo-bot:latest"
        auth {
            username = user
            password = pass
        }
    }
    container {
        ports = listOf("7000")
        workingDirectory = "/taboo-bot"
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
