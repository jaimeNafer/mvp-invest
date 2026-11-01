package io.github.jaimenafer.lastro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
public class TestLocalApplication {
    public static void main(String[] args) {
        SpringApplication.from(LastroApplication::main).with(Database.class).run(args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Database {
        @Bean @ServiceConnection
        MySQLContainer mysql() {
            return new MySQLContainer("mysql:8.0.42")
                .withLabel("io.github.jaimenafer.lastro.mode", "development-disposable")
                .withLabel("io.github.jaimenafer.lastro.workspace", java.nio.file.Path.of("").toAbsolutePath().normalize().toString().replace('\\', '/'));
        }
    }
}
