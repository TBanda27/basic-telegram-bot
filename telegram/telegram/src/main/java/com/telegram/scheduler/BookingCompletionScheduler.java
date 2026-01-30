package com.telegram.scheduler;

import com.telegram.entity.Booking;
import com.telegram.entity.Booking.BookingStatus;
import com.telegram.entity.Customer;
import com.telegram.repository.BookingRepository;
import com.telegram.service.CustomerService;
import com.telegram.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;
    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;

    // Run every 30 minutes
    @Scheduled(fixedRate = 1800000)
    public void completeBookings() {
        log.info("Running booking completion job");

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        // Get confirmed bookings from today that have ended
        List<Booking> bookingsToComplete = bookingRepository
                .findByBookingDateAndStatus(today, BookingStatus.CONFIRMED)
                .stream()
                .filter(booking -> booking.getEndTime().isBefore(currentTime))
                .toList();

        // Also get confirmed bookings from past days
        List<Booking> pastBookings = bookingRepository
                .findByBookingDateAndStatus(today.minusDays(1), BookingStatus.CONFIRMED);

        log.info("Found {} bookings to complete", bookingsToComplete.size() + pastBookings.size());

        for (Booking booking : bookingsToComplete) {
            completeBooking(booking);
        }

        for (Booking booking : pastBookings) {
            completeBooking(booking);
        }
    }

    private void completeBooking(Booking booking) {
        try {
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setCompletedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            Optional<Customer> customerOpt = customerService.findByChatlId(booking.getCustomerChatId());

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                boolean isFirstBooking = customer.getCompletedBookings() == 0;

                // Increment completed bookings
                loyaltyService.incrementCompletedBookings(customer);

                // Award points silently
                loyaltyService.awardBookingPoints(customer, isFirstBooking);
            }

            log.info("Completed booking {}", booking.getBookingCode());
        } catch (Exception e) {
            log.error("Failed to complete booking {}: {}", booking.getBookingCode(), e.getMessage());
        }
    }
}
