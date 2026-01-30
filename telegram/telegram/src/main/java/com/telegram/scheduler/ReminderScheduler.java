package com.telegram.scheduler;

import com.telegram.entity.Booking;
import com.telegram.service.MessageSender;
import com.telegram.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final MessageSender messageSender;

    // Run every day at 6 PM
    @Scheduled(cron = "0 0 18 * * *")
    public void sendDayBeforeReminders() {
        log.info("Running day-before reminder job");

        List<Booking> bookings = reminderService.getBookingsForDayBeforeReminder();
        log.info("Found {} bookings for day-before reminder", bookings.size());

        for (Booking booking : bookings) {
            try {
                String message = reminderService.buildDayBeforeReminderMessage(booking);
                messageSender.sendMessage(booking.getCustomerChatId(), message);
                reminderService.markDayBeforeReminderSent(booking);
                log.info("Sent day-before reminder for booking {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to send day-before reminder for booking {}: {}",
                        booking.getBookingCode(), e.getMessage());
            }
        }
    }

    // Run every 10 minutes
    @Scheduled(fixedRate = 600000)
    public void sendOneHourReminders() {
        log.info("Running one-hour reminder job");

        List<Booking> bookings = reminderService.getBookingsForOneHourReminder();
        log.info("Found {} bookings for one-hour reminder", bookings.size());

        for (Booking booking : bookings) {
            try {
                String message = reminderService.buildOneHourReminderMessage(booking);
                messageSender.sendMessage(booking.getCustomerChatId(), message);
                reminderService.markOneHourReminderSent(booking);
                log.info("Sent one-hour reminder for booking {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to send one-hour reminder for booking {}: {}",
                        booking.getBookingCode(), e.getMessage());
            }
        }
    }
}
