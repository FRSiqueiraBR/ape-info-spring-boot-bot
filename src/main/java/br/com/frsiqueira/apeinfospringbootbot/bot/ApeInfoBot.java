package br.com.frsiqueira.apeinfospringbootbot.bot;

import br.com.frsiqueira.apeinfospringbootbot.entity.Apartment;
import br.com.frsiqueira.apeinfospringbootbot.service.ApartmentService;
import br.com.frsiqueira.apeinfospringbootbot.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ApeInfoBot extends TelegramLongPollingBot {
    private static final String LOGTAG = "APEINFOBOT";

    private final MessageUtil messageUtil;
    private final ApartmentService apartmentService;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    @Autowired
    public ApeInfoBot(MessageUtil messageUtil, ApartmentService apartmentService) {
        this.messageUtil = messageUtil;
        this.apartmentService = apartmentService;

        this.getMainMenuKeyboard();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    if (this.isRemainingDaysCommand(message.getText())) {
                        execute(this.onDaysRemainingChosen(message));
                    } else if(this.isStartCommand(message.getText())) {
                        execute(this.onStartChosen(message));
                    }
                }
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(this.messageUtil.getMessage("options.days-remaining"));
        keyboardFirstRow.add(this.messageUtil.getMessage("options.alert"));
        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private Period remainingDays() {
        Apartment apartment = this.apartmentService.findApartment();
        Date releaseDate = apartment.getReleaseDate();

        LocalDate today = LocalDate.now();
        LocalDate releaseLocalDate = Instant.ofEpochMilli(releaseDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return Period.between(today, releaseLocalDate);
    }

    private SendMessage onDaysRemainingChosen(Message message) {
        return new SendMessage()
                .enableMarkdown(true)
                .setReplyToMessageId(message.getMessageId())
                .setChatId(message.getChatId())
                .setReplyMarkup(getMainMenuKeyboard())
                .setText(generateRemainingDaysToRelease(remainingDays()));
    }

    private SendMessage onStartChosen(Message message) {
        return new SendMessage()
                .enableMarkdown(true)
                .setReplyToMessageId(message.getMessageId())
                .setChatId(message.getChatId())
                .setReplyMarkup(getMainMenuKeyboard())
                .setText("Bem vindo");
    }

    private boolean isRemainingDaysCommand(String message) {
        return this.messageUtil.getMessage("options.days-remaining").equals(message);
    }

    private boolean isStartCommand(String message) {
        return this.messageUtil.getMessage("options.start").equals(message);
    }

    private static String generateRemainingDaysToRelease(Period period) {
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        String message;

        if (days != 0 && months != 0 && years != 0) {
            message = "Faltam " + years + " anos, " + months + " meses e " + days + " dias";
        } else if (days != 0 && months != 0) {
            message = "Faltam " + months + " meses e " + days + " dias";
        } else if (days != 0 && years == 0) {
            message = "Faltam " + days + " dias";
        } else {
            message = "Hoje é a data de entrega!";
        }

        return message;
    }
}
