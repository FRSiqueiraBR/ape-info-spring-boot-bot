package br.com.frsiqueira.apeinfospringbootbot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageUtil {
    private final MessageSource messageSource;

    @Autowired
    public MessageUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String message) {
        String messageProperty = message;

        try {
            messageProperty = this.messageSource.getMessage(messageProperty, null, Locale.getDefault());
        } catch (NoSuchMessageException ignored) {
        }

        return messageProperty;
    }

    public String getMessage(String message, Object[] parameters) {
        String messageProperty = message;

        try {
            messageProperty = this.messageSource.getMessage(messageProperty, parameters, Locale.getDefault());
        } catch (NoSuchMessageException ignored) {
        }

        return messageProperty;
    }
}
