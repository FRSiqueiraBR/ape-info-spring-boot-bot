package br.com.frsiqueira.apeinfospringbootbot.entity;

import lombok.Data;

import javax.persistence.*;

@Entity(name = "user")
public @Data class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "chat_id")
    private String chatId;
    @Column(name = "name")
    private String name;

    public User() {
    }

    public User(String userId, String chatId, String name) {
        this.userId = userId;
        this.chatId = chatId;
        this.name = name;
    }
}
