package com.musicstore;

import com.musicstore.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class MusicStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicStoreApplication.class, args);
    }
}
