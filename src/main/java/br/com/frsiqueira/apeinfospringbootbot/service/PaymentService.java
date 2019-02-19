package br.com.frsiqueira.apeinfospringbootbot.service;

import br.com.frsiqueira.apeinfospringbootbot.entity.Payment;
import br.com.frsiqueira.apeinfospringbootbot.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> findByPaidStatus(boolean paidStatus) {
        return this.paymentRepository.findByPaidOrderByDate(paidStatus);
    }

    public Payment findNextPaymentByPaidStatus(boolean paidStatus) {
        return this.paymentRepository.findByPaidOrderByDate(false)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Payment saveNextPaymentAsPaid() {
        Payment paymentPaidTrue = this.paymentRepository.findByPaidOrderByDate(false)
                .stream()
                .findFirst()
                .map(payment -> new Payment(payment.getId(), payment.getParcel(), payment.getDate(), payment.getType(), payment.getAmount(), true))
                .orElse(null);
        return this.paymentRepository.save(paymentPaidTrue);
    }
}
