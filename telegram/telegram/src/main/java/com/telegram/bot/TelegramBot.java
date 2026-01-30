package com.telegram.bot;

import com.telegram.config.TelegramBotConfig;
import com.telegram.entity.BarberService;
import com.telegram.entity.Booking;
import com.telegram.entity.Customer;
import com.telegram.service.AvailabilityService;
import com.telegram.service.BarberServiceService;
import com.telegram.service.BookingService;
import com.telegram.service.CustomerService;
import com.telegram.service.LoyaltyService;
import com.telegram.service.MessageSender;
import com.telegram.util.KeyboardHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramBotConfig telegramBotConfig;
    private final CustomerService customerService;
    private final BarberServiceService barberServiceService;
    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final LoyaltyService loyaltyService;
    private final KeyboardHelper keyboardHelper;
    private final MessageSender messageSender;

    public TelegramBot(TelegramBotConfig telegramBotConfig,
                       CustomerService customerService,
                       BarberServiceService barberServiceService,
                       BookingService bookingService,
                       AvailabilityService availabilityService,
                       LoyaltyService loyaltyService,
                       KeyboardHelper keyboardHelper,
                       MessageSender messageSender) {
        this.telegramBotConfig = telegramBotConfig;
        this.customerService = customerService;
        this.barberServiceService = barberServiceService;
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
        this.loyaltyService = loyaltyService;
        this.keyboardHelper = keyboardHelper;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        messageSender.setBot(this);
    }

    @Override
    public String getBotUsername() {
        return telegramBotConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return telegramBotConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage(), e);
        }
    }

    private void handleTextMessage(Update update) throws TelegramApiException {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();

        if (messageText.equals("/start")) {
            customerService.getOrCreateCustomer(chatId, username);
            execute(keyboardHelper.buildMainMenu(chatId));
        } else {
            execute(keyboardHelper.buildMainMenu(chatId));
        }
    }

    private void handleCallbackQuery(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();
        String[] parts = callbackData.split(":");

        String action = parts[0];

        switch (action) {
            case "menu" -> handleMenuCallback(chatId, parts);
            case "birthday", "birthday_month", "birthday_day", "birthday_confirm" -> handleBirthdayCallback(chatId, callbackData, parts);
            case "select_service" -> handleServiceSelection(chatId, parts);
            case "select_date" -> handleDateSelection(chatId, parts);
            case "select_time" -> handleTimeSelection(chatId, parts);
            case "confirm_booking" -> handleBookingConfirmation(chatId, parts);
            case "cancel_booking" -> handleCancelBooking(chatId, parts);
            case "confirm_cancel" -> handleConfirmCancel(chatId, parts);
            default -> execute(keyboardHelper.buildMainMenu(chatId));
        }
    }

    private void handleMenuCallback(Long chatId, String[] parts) throws TelegramApiException {
        String menuAction = parts[1];

        switch (menuAction) {
            case "main" -> execute(keyboardHelper.buildMainMenu(chatId));
            case "services" -> {
                List<BarberService> services = barberServiceService.getActiveServices();
                execute(keyboardHelper.buildServicesMenu(chatId, services));
            }
            case "help" -> execute(keyboardHelper.buildHelpMenu(chatId));
            case "about" -> execute(keyboardHelper.buildAboutMenu(chatId));
            case "book" -> handleBookAction(chatId);
            case "my_bookings" -> handleMyBookings(chatId);
            case "loyalty" -> handleLoyaltyStatus(chatId);
            default -> execute(keyboardHelper.buildMainMenu(chatId));
        }
    }

    private void handleBookAction(Long chatId) throws TelegramApiException {
        Customer customer = customerService.getOrCreateCustomer(chatId, null);

        if (!customer.getBirthdayPrompted()) {
            execute(keyboardHelper.buildBirthdayPrompt(chatId));
        } else {
            List<BarberService> services = barberServiceService.getActiveServices();
            execute(keyboardHelper.buildServiceSelection(chatId, services));
        }
    }

    private void handleBirthdayCallback(Long chatId, String callbackData, String[] parts) throws TelegramApiException {
        if (callbackData.equals("birthday:add")) {
            execute(keyboardHelper.buildBirthdayMonthSelection(chatId));
        } else if (callbackData.equals("birthday:skip")) {
            Customer customer = customerService.getOrCreateCustomer(chatId, null);
            customer.setBirthdayPrompted(true);
            customerService.save(customer);

            List<BarberService> services = barberServiceService.getActiveServices();
            execute(keyboardHelper.buildServiceSelection(chatId, services));
        } else if (parts[0].equals("birthday_month")) {
            String month = parts[1];
            execute(keyboardHelper.buildBirthdayDaySelection(chatId, month));
        } else if (parts[0].equals("birthday_day")) {
            int day = Integer.parseInt(parts[1]);
            String month = parts[2];
            execute(keyboardHelper.buildBirthdayConfirmation(chatId, day, month));
        } else if (parts[0].equals("birthday_confirm")) {
            int day = Integer.parseInt(parts[1]);
            String month = parts[2];
            int monthValue = java.time.Month.valueOf(month.toUpperCase()).getValue();

            Customer customer = customerService.getOrCreateCustomer(chatId, null);
            customer.setBirthdayDay(day);
            customer.setBirthdayMonth(monthValue);
            customer.setBirthdayPrompted(true);
            customerService.save(customer);

            List<BarberService> services = barberServiceService.getActiveServices();
            execute(keyboardHelper.buildServiceSelection(chatId, services));
        }
    }

    private void handleServiceSelection(Long chatId, String[] parts) throws TelegramApiException {
        String serviceSlug = parts[1];
        List<LocalDate> availableDates = availabilityService.getAvailableDates();

        if (availableDates.isEmpty()) {
            execute(keyboardHelper.buildNoAvailableDatesMessage(chatId));
            return;
        }

        execute(keyboardHelper.buildDateSelection(chatId, availableDates, serviceSlug));
    }

    private void handleDateSelection(Long chatId, String[] parts) throws TelegramApiException {
        LocalDate date = LocalDate.parse(parts[1]);
        String serviceSlug = parts[2];

        // Validate date is not in the past
        if (date.isBefore(LocalDate.now())) {
            execute(keyboardHelper.buildDateExpiredMessage(chatId));
            return;
        }

        List<LocalTime> availableSlots = availabilityService.getAvailableTimeSlots(date);

        if (availableSlots.isEmpty()) {
            execute(keyboardHelper.buildNoAvailableSlotsMessage(chatId, serviceSlug));
            return;
        }

        execute(keyboardHelper.buildTimeSelection(chatId, availableSlots, date, serviceSlug));
    }

    private void handleTimeSelection(Long chatId, String[] parts) throws TelegramApiException {
        LocalTime time = LocalTime.parse(parts[1], java.time.format.DateTimeFormatter.ofPattern("HH-mm"));
        LocalDate date = LocalDate.parse(parts[2]);
        String serviceSlug = parts[3];

        Optional<BarberService> serviceOpt = barberServiceService.findBySlug(serviceSlug);
        if (serviceOpt.isPresent()) {
            execute(keyboardHelper.buildBookingSummary(chatId, serviceOpt.get(), date, time));
        } else {
            execute(keyboardHelper.buildMainMenu(chatId));
        }
    }

    private void handleBookingConfirmation(Long chatId, String[] parts) throws TelegramApiException {
        LocalTime time = LocalTime.parse(parts[1], java.time.format.DateTimeFormatter.ofPattern("HH-mm"));
        LocalDate date = LocalDate.parse(parts[2]);
        String serviceSlug = parts[3];

        // Validate date is not in the past
        if (date.isBefore(LocalDate.now())) {
            execute(keyboardHelper.buildDateExpiredMessage(chatId));
            return;
        }

        // Validate time is not in the past for today's bookings
        if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            execute(keyboardHelper.buildDateExpiredMessage(chatId));
            return;
        }

        Optional<BarberService> serviceOpt = barberServiceService.findBySlug(serviceSlug);

        if (serviceOpt.isPresent() && bookingService.isSlotAvailable(date, time)) {
            BarberService service = serviceOpt.get();
            Booking booking = bookingService.createBooking(chatId, service, date, time);

            // Increment total bookings for customer
            Customer customer = customerService.getOrCreateCustomer(chatId, null);
            loyaltyService.incrementTotalBookings(customer);

            execute(keyboardHelper.buildBookingConfirmed(chatId, booking, service));
        } else {
            execute(keyboardHelper.buildSlotUnavailableMessage(chatId));
        }
    }

    private void handleMyBookings(Long chatId) throws TelegramApiException {
        List<Booking> bookings = bookingService.getCustomerActiveBookings(chatId);

        Map<Long, BarberService> serviceMap = new HashMap<>();
        for (Booking booking : bookings) {
            if (!serviceMap.containsKey(booking.getServiceId())) {
                barberServiceService.findById(booking.getServiceId())
                        .ifPresent(service -> serviceMap.put(service.getId(), service));
            }
        }

        execute(keyboardHelper.buildMyBookings(chatId, bookings, serviceMap));
    }

    private void handleCancelBooking(Long chatId, String[] parts) throws TelegramApiException {
        String bookingCode = parts[1];
        Optional<Booking> bookingOpt = bookingService.findByBookingCode(bookingCode);

        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            Optional<BarberService> serviceOpt = barberServiceService.findById(booking.getServiceId());

            if (serviceOpt.isPresent()) {
                execute(keyboardHelper.buildCancelConfirmation(chatId, booking, serviceOpt.get()));
                return;
            }
        }

        execute(keyboardHelper.buildMainMenu(chatId));
    }

    private void handleConfirmCancel(Long chatId, String[] parts) throws TelegramApiException {
        String bookingCode = parts[1];
        Optional<Booking> bookingOpt = bookingService.findByBookingCode(bookingCode);

        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            if (booking.getCustomerChatId().equals(chatId)) {
                bookingService.cancelBooking(booking);

                // Increment cancelled bookings for customer
                Customer customer = customerService.getOrCreateCustomer(chatId, null);
                loyaltyService.incrementCancelledBookings(customer);

                execute(keyboardHelper.buildCancellationSuccess(chatId, bookingCode));
                return;
            }
        }

        execute(keyboardHelper.buildMainMenu(chatId));
    }

    private void handleLoyaltyStatus(Long chatId) throws TelegramApiException {
        Customer customer = customerService.getOrCreateCustomer(chatId, null);
        execute(keyboardHelper.buildLoyaltyStatus(chatId, customer));
    }
}
