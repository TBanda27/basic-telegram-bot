package com.telegram.service;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.BarberService;
import com.telegram.entity.Booking;
import com.telegram.entity.Booking.BookingStatus;
import com.telegram.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final BookingRepository bookingRepository;
    private final BarberServiceService barberServiceService;
    private final BarbershopConfig config;

    public List<Booking> getBookingsForDayBeforeReminder() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return bookingRepository.findByStatusAndDayBeforeReminderSentFalseAndBookingDate(
                BookingStatus.CONFIRMED, tomorrow);
    }

    public List<Booking> getBookingsForOneHourReminder() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime oneHourLater = now.plusHours(1);

        return bookingRepository.findByStatusAndOneHourReminderSentFalseAndBookingDateAndStartTimeBetween(
                BookingStatus.CONFIRMED, today, now, oneHourLater);
    }

    public String buildDayBeforeReminderMessage(Booking booking) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Optional<BarberService> serviceOpt = barberServiceService.findById(booking.getServiceId());
        String serviceName = serviceOpt.map(BarberService::getName).orElse("Your appointment");

        return String.format("""
                👋 Reminder!

                You have an appointment tomorrow.

                💈 %s
                📅 %s
                ⏰ %s
                📍 %s

                💰 Amount due: €%.2f

                See you tomorrow!
                """,
                serviceName,
                booking.getBookingDate().format(dateFormatter),
                booking.getStartTime().format(timeFormatter),
                config.getAddress(),
                booking.getRemainingBalance());
    }

    public String buildOneHourReminderMessage(Booking booking) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        Optional<BarberService> serviceOpt = barberServiceService.findById(booking.getServiceId());
        String serviceName = serviceOpt.map(BarberService::getName).orElse("Your appointment");

        return String.format("""
                ⏰ Your appointment is in 1 hour!

                💈 %s at %s
                📍 %s

                💰 Please bring €%.2f

                See you soon!
                """,
                serviceName,
                booking.getStartTime().format(timeFormatter),
                config.getAddress(),
                booking.getRemainingBalance());
    }

    public void markDayBeforeReminderSent(Booking booking) {
        booking.setDayBeforeReminderSent(true);
        bookingRepository.save(booking);
    }

    public void markOneHourReminderSent(Booking booking) {
        booking.setOneHourReminderSent(true);
        bookingRepository.save(booking);
    }
}
