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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ApeInfoBot extends TelegramLongPollingBot {
    private static final String LOGTAG = "APEINFOBOT";
    private final MessageUtil messageUtil;
    private final ApartmentService apartmentService;
    private final UserService userService;
    private final AlertService alertService;
    private final PaymentService paymentService;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");
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
                        execute(this.onAlertChosen(message));
                    } else if (this.isMarkAsPaidCommand(message.getText())) {
                        execute(this.onMarkAsPaidChosen(message));
                    } else if (this.isNextPaymentCommand(message.getText())) {
                        execute(this.onNextPaymentChosen(message));
                    } else if (this.isListNotPaid(message.getText())) {
                        execute(this.onListNotPaid(message));
                    } else if (this.isListPaid(message.getText())) {
                        execute(this.onListPaid(message));
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
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(this.messageUtil.getMessage("options.mark-as-paid"));
        keyboardSecondRow.add(this.messageUtil.getMessage("options.next-payment"));
        KeyboardRow keyboardThirdRow = new KeyboardRow();
        keyboardThirdRow.add(this.messageUtil.getMessage("options.list-not-paid"));
        keyboardThirdRow.add(this.messageUtil.getMessage("options.list-paid"));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        keyboard.add(keyboardThirdRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private void startAlertTimers() {
        TimerExecutor.getInstance().startExecutionEveryDayAt(new CustomTimerTask("Alert", -1) {
            @Override
            public void execute() {
                sendAlerts();
            }
        }, 8, 0, 0);
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
                Payment nextPayment = this.findNextPayment();
                Long chatId = Long.valueOf(alert.getUser().getChatId());

                if (this.isOneDayBeforeOrDayPayment(nextPayment)) {
                    String replyMessage = createReplyMessageAlert(nextPayment);

                    SendMessage sendMessage = replyMessage(null, chatId, replyMessage);
                    execute(sendMessage);
                }
            } catch (TelegramApiRequestException e) {
                BotLogger.warn(LOGTAG, e);
                if (this.isChatBlockedOrDeleted(e)) {
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
        return this.replyMessage(message.getMessageId(), message.getChatId(), this.generateRemainingDaysToRelease(this.remainingDays()));
    }

    private SendMessage onStartChosen(Message message) {
        this.saveUser(message.getFrom(), message.getChat());

        String replyMessage = this.messageUtil.getMessage("reply-message.welcome");
        return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
    }

    private SendMessage onAlertChosen(Message message) {
        try {
            this.saveAlert(message);
            String replyMessage = this.messageUtil.getMessage("reply-message.alert");

            return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
            String errorMessage = this.messageUtil.getMessage("reply-message.alert.error");

            return this.replyMessage(message.getMessageId(), message.getChatId(), errorMessage);
        }
    }

    private SendMessage onMarkAsPaidChosen(Message message) {
        try {
            Payment payment = this.saveNextPaymentAsPaid();
            Object[] params = new Object[]{payment.getParcel().toString(), sdf.format(payment.getDate())};
            String replyMessage = this.messageUtil.getMessage("reply-message.mark-as-paid", params);

            return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
            String errorMessage = this.messageUtil.getMessage("reply-message.mark-as-paid.error");

            return this.replyMessage(message.getMessageId(), message.getChatId(), errorMessage);
        }
    }

    private SendMessage onNextPaymentChosen(Message message) {
        try {
            Payment payment = this.paymentService.findNextPaymentByPaidStatus(false);
            Object[] params = new Object[]{payment.getParcel().toString(), sdf.format(payment.getDate())};
            String replyMessage = this.messageUtil.getMessage("reply-message.next-payment", params);

            return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
            String errorMessage = this.messageUtil.getMessage("reply-message.next-payment.error");

            return this.replyMessage(message.getMessageId(), message.getChatId(), errorMessage);
        }
    }

    private SendMessage onListNotPaid(Message message) {
        try{
            String replyMessagePaymentTemplate = "reply-message.list-not-paid";
            String replyMessageEmptyListTemplate = "reply-message.all-paid";
            List<Payment> payments = this.paymentService.findByPaidStatus(false);

            String replyMessage = this.replyMessagePaymentList(replyMessagePaymentTemplate, replyMessageEmptyListTemplate, payments);

            return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
            String errorMessage = this.messageUtil.getMessage("reply-message.list-not-paid.error");

            return this.replyMessage(message.getMessageId(), message.getChatId(), errorMessage);
        }
    }

    private SendMessage onListPaid(Message message) {
        try {
            String replyMessagePaymentTemplate = "reply-message.list-paid";
            String replyMessageEmptyListTemplate = "reply-message.all-not-paid";
            List<Payment> payments = this.paymentService.findByPaidStatus(true);

            String replyMessage = this.replyMessagePaymentList(replyMessagePaymentTemplate, replyMessageEmptyListTemplate, payments);

            return this.replyMessage(message.getMessageId(), message.getChatId(), replyMessage);
        } catch (Exception e) {
            BotLogger.error(LOGTAG, e);
            String errorMessage = this.messageUtil.getMessage("reply-message.list-paid.error");

            return this.replyMessage(message.getMessageId(), message.getChatId(), errorMessage);
        }
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

    private boolean isMarkAsPaidCommand(String message) {
        return this.messageUtil.getMessage("options.mark-as-paid").equals(message);
    }

    private boolean isNextPaymentCommand(String message) {
        return this.messageUtil.getMessage("options.next-payment").equals(message);
    }

    private boolean isListNotPaid(String message) {
        return this.messageUtil.getMessage("options.list-not-paid").equals(message);
    }

    private boolean isListPaid(String message) {
        return this.messageUtil.getMessage("options.list-paid").equals(message);
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

    private Payment saveNextPaymentAsPaid() {
        return this.paymentService.saveNextPaymentAsPaid();
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

    private boolean isChatBlockedOrDeleted(TelegramApiRequestException error) {
        return error.getApiResponse().contains("alert.chat-not-found") ||
                error.getApiResponse().contains("alert.chat-blocked-deleted");
    }

    private boolean isOneDayBeforeOrDayPayment(Payment payment) {
        Date paymentDate = payment.getDate();
        Date today = new Date();
        long differenceInDays = this.getDateDiff(paymentDate, today);

        return differenceInDays >= 1 || differenceInDays == 0;
    }

    private long getDateDiff(Date date1, Date date2) {
        long diffInMillis = date2.getTime() - date1.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private String replyMessagePaymentList(String replyMessage, String replyMessageEmptyList, List<Payment> payments) {
        if (Objects.nonNull(payments)) {
            return payments.stream()
                    .map(payment -> {
                        Object[] params = new Object[]{formatParcel(payment), sdf.format(payment.getDate())};
                        return messageUtil.getMessage(replyMessage, params);
                    })
                    .collect(Collectors.joining("\n"));
        } else {
            return this.messageUtil.getMessage(replyMessageEmptyList);
        }
    }

    private String formatParcel(Payment payment) {
        if (Objects.nonNull(payment.getParcel())) {
            return payment.getParcel().toString();
        } else {
            return this.messageUtil.getMessage("payment.parcel.unique");
        }
    }
}
