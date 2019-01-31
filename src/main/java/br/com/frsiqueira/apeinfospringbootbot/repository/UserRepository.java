package br.com.frsiqueira.apeinfospringbootbot.repository;

import br.com.frsiqueira.apeinfospringbootbot.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String> {

    Optional<List<User>> findByUserIdAndChatId(String userId, String chatId);
}
