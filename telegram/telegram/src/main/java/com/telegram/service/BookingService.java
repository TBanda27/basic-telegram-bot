package com.telegram.service;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.Booking;
import com.telegram.entity.Booking.BookingStatus;
import com.telegram.entity.BarberService;
import com.telegram.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BarbershopConfig config;

    public Booking createBooking(Long customerChatId, BarberService service, LocalDate date, LocalTime time) {
        BigDecimal price = service.getPrice();
        BigDecimal depositAmount = price
                .multiply(BigDecimal.valueOf(config.getDepositPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal remainingBalance = price.subtract(depositAmount);

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .customerChatId(customerChatId)
                .serviceId(service.getId())
                .bookingDate(date)
                .startTime(time)
                .endTime(time.plusMinutes(service.getDurationMinutes()))
                .status(BookingStatus.CONFIRMED)
                .depositAmount(depositAmount)
                .depositPaid(false)
                .remainingBalance(remainingBalance)
                .dayBeforeReminderSent(false)
                .oneHourReminderSent(false)
                .build();

        return bookingRepository.save(booking);
    }

    public Optional<Booking> findByBookingCode(String code) {
        return bookingRepository.findByBookingCode(code);
    }

    public List<Booking> getCustomerActiveBookings(Long customerChatId) {
        List<Booking> pending = bookingRepository.findByCustomerChatIdAndStatus(customerChatId, BookingStatus.PENDING);
        List<Booking> confirmed = bookingRepository.findByCustomerChatIdAndStatus(customerChatId, BookingStatus.CONFIRMED);

        List<Booking> allActive = new java.util.ArrayList<>(pending);
        allActive.addAll(confirmed);
        return allActive;
    }

    public boolean isSlotAvailable(LocalDate date, LocalTime time) {
        boolean confirmedExists = bookingRepository.existsByBookingDateAndStartTimeAndStatus(date, time, BookingStatus.CONFIRMED);
        boolean pendingExists = bookingRepository.existsByBookingDateAndStartTimeAndStatus(date, time, BookingStatus.PENDING);
        return !confirmedExists && !pendingExists;
    }

    public Booking cancelBooking(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    private String generateBookingCode() {
        String code;
        do {
            code = "BK" + (1000 + new Random().nextInt(9000));
        } while (bookingRepository.findByBookingCode(code).isPresent());
        return code;
    }
}
