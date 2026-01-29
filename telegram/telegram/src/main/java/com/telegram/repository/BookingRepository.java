package com.telegram.repository;

import com.telegram.entity.Booking;
import com.telegram.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByCustomerChatIdAndStatus(Long customerChatId, BookingStatus status);

    List<Booking> findByBookingDateAndStatus(LocalDate date, BookingStatus status);

    List<Booking> findByBookingDateAndStatusAndStartTimeBetween(
            LocalDate date,
            BookingStatus status,
            LocalTime startTime,
            LocalTime endTime
    );

    List<Booking> findByStatusAndDayBeforeReminderSentFalseAndBookingDate(
            BookingStatus status,
            LocalDate date
    );

    List<Booking> findByStatusAndOneHourReminderSentFalseAndBookingDateAndStartTimeBetween(
            BookingStatus status,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );

    boolean existsByBookingDateAndStartTimeAndStatus(
            LocalDate date,
            LocalTime startTime,
            BookingStatus status
    );
}
