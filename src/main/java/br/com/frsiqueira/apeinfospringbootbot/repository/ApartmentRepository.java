package br.com.frsiqueira.apeinfospringbootbot.repository;

import br.com.frsiqueira.apeinfospringbootbot.entity.Apartment;
import org.springframework.data.repository.CrudRepository;

public interface ApartmentRepository extends CrudRepository<Apartment, String> {
}
