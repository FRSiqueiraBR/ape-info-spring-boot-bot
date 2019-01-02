package br.com.frsiqueira.apeinfospringbootbot.service;

import br.com.frsiqueira.apeinfospringbootbot.entity.Apartment;
import br.com.frsiqueira.apeinfospringbootbot.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;

    @Autowired
    public ApartmentService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    public Apartment findApartment() {
        return apartmentRepository.findAll().iterator().next();
    }
}
