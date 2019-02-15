package br.com.frsiqueira.apeinfospringbootbot.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity(name = "payment")
public @Data class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "parcel")
    private Integer parcel;
    @Column(name = "date")
    private Date date;
    @Column(name = "type")
    private String type;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "paid")
    private boolean paid;

    public Payment() {
    }

    public Payment(Integer id, Integer parcel, Date date, String type, BigDecimal amount, boolean paid) {
        this.id = id;
        this.parcel = parcel;
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.paid = paid;
    }
}
