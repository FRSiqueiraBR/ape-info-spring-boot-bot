package br.com.frsiqueira.apeinfospringbootbot.repository;

import br.com.frsiqueira.apeinfospringbootbot.entity.Alert;
import br.com.frsiqueira.apeinfospringbootbot.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AlertRepository extends CrudRepository<Alert, Integer> {
    Alert findByUser(User user);
    List<Alert> findAll();
}
