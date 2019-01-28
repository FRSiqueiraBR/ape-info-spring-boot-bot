package br.com.frsiqueira.apeinfospringbootbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class ApeInfoSpringBootBotApplication {

    @PostConstruct
    public void registerBot() {
        ApiContextInitializer.init();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApeInfoSpringBootBotApplication.class, args);
    }

}

