package br.com.frsiqueira.apeinfospringbootbot.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "apartment")
public @Data class Apartment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "address")
    private String address;
    @Column (name = "release_date")
    private Date releaseDate;
}
