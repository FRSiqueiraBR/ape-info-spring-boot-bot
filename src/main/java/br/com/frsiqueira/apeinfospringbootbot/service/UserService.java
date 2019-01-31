package br.com.frsiqueira.apeinfospringbootbot.service;

import br.com.frsiqueira.apeinfospringbootbot.entity.User;
import br.com.frsiqueira.apeinfospringbootbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.logging.BotLogger;

@Service
public class UserService {
    private static final String LOGTAG = "USERSERVICE";

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(User user) {
        BotLogger.info(LOGTAG, "Salvando novo user...");
        this.userRepository.save(user);
    }

    public User findUser(String userId, String chatId) {
        BotLogger.info(LOGTAG, "Buscando user");

        return this.userRepository.findByUserIdAndChatId(userId, chatId)
                .flatMap(users -> users.stream().findFirst())
                .orElse(null);
    }
}
