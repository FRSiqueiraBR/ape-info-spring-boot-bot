package br.com.frsiqueira.apeinfospringbootbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class ApeInfoSpringBootBotApplication {

    public static void main(String[] args) {
        ApiContextInitializer.init();

        SpringApplication.run(ApeInfoSpringBootBotApplication.class, args);
    }

}

