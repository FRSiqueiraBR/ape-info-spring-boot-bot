package br.com.frsiqueira.apeinfospringbootbot.repository;

import br.com.frsiqueira.apeinfospringbootbot.entity.Payment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PaymentRepository extends CrudRepository<Payment, Integer> {

    List<Payment> findByPaidOrderByDate(boolean paid);
}
