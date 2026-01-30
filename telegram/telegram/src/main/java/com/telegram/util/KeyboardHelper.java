package com.telegram.util;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.BarberService;
import com.telegram.entity.Booking;
import com.telegram.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class KeyboardHelper {

    private final BarbershopConfig config;

    public SendMessage buildMainMenu(Long chatId) {
        String text = "Welcome! 👋\n\nPlease select an option:";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Book Appointment", "menu:book"));
        keyboard.add(createButtonRow("💈 Our Services", "menu:services"));
        keyboard.add(createButtonRow("📋 My Bookings", "menu:my_bookings"));
        keyboard.add(createButtonRow("💎 Loyalty Points", "menu:loyalty"));
        keyboard.add(createButtonRow("❓ Help", "menu:help"));
        keyboard.add(createButtonRow("ℹ️ About", "menu:about"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildServicesMenu(Long chatId, List<BarberService> services) {
        StringBuilder text = new StringBuilder("💈 Our Services:\n\n");

        for (BarberService service : services) {
            text.append(String.format("✂️ %s\n   €%.2f • %d minutes\n\n",
                    service.getName(),
                    service.getPrice(),
                    service.getDurationMinutes()));
        }

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Book Now", "menu:book"));
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text.toString(), keyboard);
    }

    public SendMessage buildHelpMenu(Long chatId) {
        String text = """
                ❓ Help

                📅 Booking: Select 'Book Appointment' and follow the steps

                ❌ Cancellation: Go to 'My Bookings' and select cancel

                💰 Payment: %d%% deposit required, remaining balance due at shop

                🎂 Birthday: Add your birthday on first booking for special rewards

                📍 Location: %s
                """.formatted(config.getDepositPercentage(), config.getAddress());

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildAboutMenu(Long chatId) {
        String text = """
                ℹ️  %s


                📍 Address: %s
                
                📍 Eircode: %s
                
                ☎️ PhoneNumber: %s
                
                ⏰ Operating Hours: %s - %s
                
                📅 Closed: %s
                
                """.formatted(
                config.getName(),
                config.getAddress(),
                config.getEircode(),
                config.getPhoneNumber(),
                config.getOpeningTime(),
                config.getClosingTime(),
                config.getClosedDays());

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBirthdayPrompt(Long chatId) {
        String text = """
                🎂 Before we continue...

                We encourage you to add your birthday so we can offer you special discounts and rewards on your special day!

                This is a one-time prompt - we won't ask again.
                """;

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🎂 Add Birthday", "birthday:add"));
        keyboard.add(createButtonRow("➡️ Skip", "birthday:skip"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBirthdayMonthSelection(Long chatId) {
        String text = "🎂 Select your birth month:";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (Month month : Month.values()) {
            String monthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            row.add(createButton(monthName, "birthday_month:" + month.name().toLowerCase()));

            if (row.size() == 3) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        keyboard.add(createButtonRow("❌ Cancel", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBirthdayDaySelection(Long chatId, String month) {
        String text = "🎂 Select your birth day:";
        Month monthEnum = Month.valueOf(month.toUpperCase());
        int daysInMonth = monthEnum.maxLength();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            row.add(createButton(String.valueOf(day), "birthday_day:" + day + ":" + month));

            if (row.size() == 7) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        keyboard.add(createButtonRow("🔙 Back", "birthday:add"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBirthdayConfirmation(Long chatId, int day, String month) {
        String monthDisplay = Month.valueOf(month.toUpperCase()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String text = String.format("🎂 Your birthday is set to %d %s.\n\nThis cannot be changed later.", day, monthDisplay);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("✅ Confirm", "birthday_confirm:" + day + ":" + month));
        keyboard.add(createButtonRow("🔙 Change", "birthday:add"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildServiceSelection(Long chatId, List<BarberService> services) {
        String text = "💈 Select a service:";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (BarberService service : services) {
            String buttonText = String.format("%s - €%.2f", service.getName(), service.getPrice());
            keyboard.add(createButtonRow(buttonText, "select_service:" + service.getSlug()));
        }

        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildDateSelection(Long chatId, List<LocalDate> availableDates, String serviceSlug) {
        String text = "📅 Select a date:";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (LocalDate date : availableDates) {
            String buttonText = date.format(formatter);
            keyboard.add(createButtonRow(buttonText, "select_date:" + date + ":" + serviceSlug));
        }

        keyboard.add(createButtonRow("🔙 Back", "menu:book"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildTimeSelection(Long chatId, List<LocalTime> availableSlots, LocalDate date, String serviceSlug) {
        String text = "⏰ Select a time:";
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter callbackFormatter = DateTimeFormatter.ofPattern("HH-mm");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (LocalTime slot : availableSlots) {
            row.add(createButton(slot.format(displayFormatter), "select_time:" + slot.format(callbackFormatter) + ":" + date + ":" + serviceSlug));

            if (row.size() == 3) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        if (!row.isEmpty()) {
            keyboard.add(row);
        }

        keyboard.add(createButtonRow("🔙 Back", "select_service:" + serviceSlug));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBookingSummary(Long chatId, BarberService service, LocalDate date, LocalTime time) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        var depositAmount = service.getPrice()
                .multiply(java.math.BigDecimal.valueOf(config.getDepositPercentage()))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        var remainingBalance = service.getPrice().subtract(depositAmount);

        String text = String.format("""
                📋 Booking Summary

                💈 Service: %s
                📅 Date: %s
                ⏰ Time: %s

                💰 Total: €%.2f
                💳 Deposit (%d%%): €%.2f
                🏪 Due at shop: €%.2f
                """,
                service.getName(),
                date.format(dateFormatter),
                time.format(timeFormatter),
                service.getPrice(),
                config.getDepositPercentage(),
                depositAmount,
                remainingBalance);

        DateTimeFormatter callbackFormatter = DateTimeFormatter.ofPattern("HH-mm");
        String callbackData = "confirm_booking:" + time.format(callbackFormatter) + ":" + date + ":" + service.getSlug();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("✅ Confirm Booking", callbackData));
        keyboard.add(createButtonRow("❌ Cancel", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildBookingConfirmed(Long chatId, Booking booking, BarberService service) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String text = String.format("""
                ✅ Booking Confirmed!

                Your booking code: %s

                💈 %s
                📅 %s
                ⏰ %s
                📍 %s

                💰 Total: €%.2f (pay at shop)

                We'll send you a reminder before your appointment.
                """,
                booking.getBookingCode(),
                service.getName(),
                booking.getBookingDate().format(dateFormatter),
                booking.getStartTime().format(timeFormatter),
                config.getAddress(),
                service.getPrice());

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildMyBookings(Long chatId, List<Booking> bookings, java.util.Map<Long, BarberService> serviceMap) {
        if (bookings.isEmpty()) {
            String text = "📋 You have no upcoming bookings.";
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(createButtonRow("📅 Book Now", "menu:book"));
            keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));
            return buildMessage(chatId, text, keyboard);
        }

        StringBuilder text = new StringBuilder("📋 Your Bookings:\n\n");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Booking booking : bookings) {
            BarberService service = serviceMap.get(booking.getServiceId());
            String serviceName = service != null ? service.getName() : "Unknown Service";

            String statusText = booking.getStatus() == Booking.BookingStatus.PENDING
                    ? "⏳ Pending Deposit"
                    : "✅ Confirmed";

            text.append(String.format("🔖 %s\n💈 %s\n📅 %s at %s\nStatus: %s\n\n",
                    booking.getBookingCode(),
                    serviceName,
                    booking.getBookingDate().format(dateFormatter),
                    booking.getStartTime().format(timeFormatter),
                    statusText));

            keyboard.add(createButtonRow("❌ Cancel " + booking.getBookingCode(), "cancel_booking:" + booking.getBookingCode()));
        }

        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text.toString(), keyboard);
    }

    public SendMessage buildCancelConfirmation(Long chatId, Booking booking, BarberService service) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String text = String.format("""
                ⚠️ Cancel Booking?

                🔖 %s - %s
                📅 %s at %s

                Are you sure you want to cancel?
                """,
                booking.getBookingCode(),
                service.getName(),
                booking.getBookingDate().format(dateFormatter),
                booking.getStartTime().format(timeFormatter));

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("Yes, Cancel", "confirm_cancel:" + booking.getBookingCode()));
        keyboard.add(createButtonRow("No, Keep It", "menu:my_bookings"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildCancellationSuccess(Long chatId, String bookingCode) {
        String text = String.format("✅ Booking %s has been cancelled.", bookingCode);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildDateExpiredMessage(Long chatId) {
        String text = "⚠️ This date/time has already passed.\n\nPlease select a new date and time.";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Book Again", "menu:book"));
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildSlotUnavailableMessage(Long chatId) {
        String text = "⚠️ This time slot is no longer available.\n\nPlease select a different time.";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Book Again", "menu:book"));
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildNoAvailableDatesMessage(Long chatId) {
        String text = "😔 Sorry, there are no available dates at the moment.\n\nPlease try again later.";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildNoAvailableSlotsMessage(Long chatId, String serviceSlug) {
        String text = "😔 Sorry, there are no available time slots for this date.\n\nPlease select a different date.";

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Choose Another Date", "select_service:" + serviceSlug));
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    public SendMessage buildLoyaltyStatus(Long chatId, Customer customer) {
        int nextMilestone = getNextMilestone(customer.getCompletedBookings());
        int bookingsToNext = nextMilestone - customer.getCompletedBookings();

        String text = String.format("""
                💎 Your Loyalty Status

                💰 Current Points: %d
                ✂️ Completed Bookings: %d
                📊 Total Bookings: %d
                🏆 Lifetime Points: %d

                🎯 Next milestone: %d bookings (%d to go!)

                Keep booking to earn more rewards!
                """,
                customer.getLoyaltyPoints(),
                customer.getCompletedBookings(),
                customer.getTotalBookings(),
                customer.getLifetimeLoyaltyPoints(),
                nextMilestone,
                bookingsToNext);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createButtonRow("📅 Book Now", "menu:book"));
        keyboard.add(createButtonRow("🔙 Back to Menu", "menu:main"));

        return buildMessage(chatId, text, keyboard);
    }

    private int getNextMilestone(int completedBookings) {
        List<Integer> milestones = config.getLoyalty().getMilestones();

        for (int milestone : milestones) {
            if (milestone > completedBookings) {
                return milestone;
            }
        }

        return milestones.isEmpty() ? 5 : milestones.get(milestones.size() - 1);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private List<InlineKeyboardButton> createButtonRow(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return List.of(button);
    }

    private SendMessage buildMessage(Long chatId, String text, List<List<InlineKeyboardButton>> keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        return message;
    }
}
