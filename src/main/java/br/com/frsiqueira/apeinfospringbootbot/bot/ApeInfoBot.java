package br.com.frsiqueira.apeinfospringbootbot.bot;

import br.com.frsiqueira.apeinfospringbootbot.entity.Alert;
import br.com.frsiqueira.apeinfospringbootbot.entity.Apartment;
import br.com.frsiqueira.apeinfospringbootbot.entity.Payment;
import br.com.frsiqueira.apeinfospringbootbot.entity.User;
import br.com.frsiqueira.apeinfospringbootbot.service.*;
import br.com.frsiqueira.apeinfospringbootbot.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class ApeInfoBot extends TelegramLongPollingBot {
    private static final String LOGTAG = "APEINFOBOT";
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");

    private final MessageUtil messageUtil;
    private final ApartmentService apartmentService;
    private final UserService userService;
    private final AlertService alertService;
    private final PaymentService paymentService;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    @Autowired
    public ApeInfoBot(MessageUtil messageUtil, ApartmentService apartmentService, UserService userService, AlertService alertService, PaymentService paymentService) {
        this.messageUtil = messageUtil;
        this.apartmentService = apartmentService;
        this.userService = userService;
        this.alertService = alertService;
        this.paymentService = paymentService;

        this.getMainMenuKeyboard();
        this.startAlertTimers();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation()) {
                    if (this.isRemainingDaysCommand(message.getText())) {
                        execute(this.onDaysRemainingChosen(message));
                    } else if (this.isStartCommand(message.getText())) {
                        execute(this.onStartChosen(message));
                    } else if (this.isAlertCommand(message.getText())) {
                        execute(this.onAlertCommand(message));
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

    private void startAlertTimers() {
        TimerExecutor.getInstance().startExecutionEveryDayAt(new CustomTimerTask("First day alert", -1) {
            @Override
            public void execute() {
                sendAlerts();
            }
        }, 0, 0, 0);

        TimerExecutor.getInstance().startExecutionEveryDayAt(new CustomTimerTask("Second day alert", -1) {
            @Override
            public void execute() {
                sendAlerts();
            }
        }, 12, 0, 0);
    }

    private void sendAlerts() {
        BotLogger.info(LOGTAG, this.messageUtil.getMessage("alert.info"));

        List<Alert> allAlerts = this.alertService.findAll();

        for (Alert alert : allAlerts) {
            synchronized (Thread.currentThread()) {
                try {
                    Thread.currentThread().wait(35);
                } catch (InterruptedException e) {
                    BotLogger.severe(LOGTAG, e);
                }
            }

            try {
                Payment payment = this.findNextPayment();
                Long chatId = Long.valueOf(alert.getUser().getChatId());
                String replyMessage = createReplyMessageAlert(payment);

                SendMessage sendMessage = replyMessage(null, chatId, replyMessage);
                execute(sendMessage);
            } catch (TelegramApiRequestException e) {
                BotLogger.warn(LOGTAG, e);
                if (e.getApiResponse().contains("Can't access the chat") || e.getApiResponse().contains("Bot was blocked by the user")) {
                    this.alertService.delete(alert);
                }
            } catch (Exception e) {
                BotLogger.severe(LOGTAG, e);
            }
        }
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
        BotLogger.info(LOGTAG, this.messageUtil.getMessage("days-remaining.info"));

        return this.replyMessage(
                message.getMessageId(),
                message.getChatId(),
                this.generateRemainingDaysToRelease(this.remainingDays()));
    }

    private SendMessage onStartChosen(Message message) {
        this.saveUser(message.getFrom(), message.getChat());

        return this.replyMessage(
                message.getMessageId(),
                message.getChatId(),
                this.messageUtil.getMessage("start.welcome"));
    }

    private SendMessage onAlertCommand(Message message) {
        this.saveAlert(message);
        return this.replyMessage(
                message.getMessageId(),
                message.getChatId(),
                this.messageUtil.getMessage("alert.reply-message"));
    }

    private boolean isRemainingDaysCommand(String message) {
        return this.messageUtil.getMessage("options.days-remaining").equals(message);
    }

    private boolean isStartCommand(String message) {
        return this.messageUtil.getMessage("options.start").equals(message);
    }

    private boolean isAlertCommand(String message) {
        return this.messageUtil.getMessage("options.alert").equals(message);
    }

    private String generateRemainingDaysToRelease(Period period) {
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        StringBuilder message = new StringBuilder();
        message.append("Faltam ");

        if (years == 1) {
            message.append(years);
            message.append(" ano ");
        } else if (years != 0) {
            message.append(years);
            message.append(" anos ");
        }

        if (months == 1) {
            message.append(months);
            message.append(" mês ");
        } else if (months != 0) {
            message.append(months);
            message.append(" meses ");
        }

        if (days == 1) {
            message.append(days);
            message.append(" dia");
        } else if (days != 0) {
            message.append(days);
            message.append(" dias");
        }

        return message.toString();
    }

    private void saveUser(org.telegram.telegrambots.meta.api.objects.User from, Chat chat) {
        try {
            User user = new User(String.valueOf(chat.getId()), String.valueOf(from.getId()), from.getFirstName());

            if (this.userService.findUser(String.valueOf(from.getId()), String.valueOf(chat.getId())) == null) {
                this.userService.saveUser(user);
            } else {
                BotLogger.info(LOGTAG, this.messageUtil.getMessage("user.info.already-created"));
            }
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void saveAlert(Message message) {
        String userId = String.valueOf(message.getFrom().getId());
        String chatId = String.valueOf(message.getChatId());

        if (!this.alertAlreadySaved(userId, chatId)) {
            User user = this.userService.findUser(userId, chatId);
            this.alertService.save(new Alert(user));
        }
    }

    private boolean alertAlreadySaved(String userId, String chatId) {
        User user = this.userService.findUser(userId, chatId);

        return !Objects.isNull(this.alertService.findByUser(user));
    }

    private SendMessage replyMessage(Integer messageId, Long chatId, String message) {
        return new SendMessage()
                .enableMarkdown(true)
                .setReplyToMessageId(messageId)
                .setChatId(chatId)
                .setReplyMarkup(getMainMenuKeyboard())
                .setText(message);
    }

    private Payment findNextPayment() {
        return this.paymentService.findByPaidStatus(false)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String createReplyMessageAlert(Payment payment) {
        if (Objects.nonNull(payment) && Objects.nonNull(payment.getDate())) {
            return this.messageUtil.getMessage("alert.reply-message.success") + sdf.format(payment.getDate());
        } else {
            return this.messageUtil.getMessage("alert.reply-message.error");
        }
    }
}
