package com.telegram.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "barbershop")
public class BarbershopConfig {

    private String name;
    private String address;
    private String phoneNumber;
    private String eircode;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private DayOfWeek closedDays;
    private Integer slotIntervalMinutes;
    private Integer minAdvanceBookingHours;
    private Integer bookingWindowDays;
    private Integer depositPercentage;

    private Loyalty loyalty = new Loyalty();

    @Data
    public static class Loyalty {
        private Integer pointsPerBooking;
        private Integer firstBookingBonus;
        private Integer birthdayBonusPoints;
        private String birthdayDiscountCode;
        private Integer birthdayDiscountPercent;
        private List<Integer> milestones;
    }
}
