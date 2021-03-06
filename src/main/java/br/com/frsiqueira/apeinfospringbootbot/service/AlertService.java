package br.com.frsiqueira.apeinfospringbootbot.service;

import br.com.frsiqueira.apeinfospringbootbot.entity.Alert;
import br.com.frsiqueira.apeinfospringbootbot.entity.User;
import br.com.frsiqueira.apeinfospringbootbot.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    @Autowired
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void save(Alert alert) {
        alertRepository.save(alert);
    }

    public Alert findByUser(User user) {
        return this.alertRepository.findByUser(user);
    }

    public List<Alert> findAll() {
        return this.alertRepository.findAll();
    }

    public void delete(Alert alert) {
        this.alertRepository.delete(alert);
    }
}
