package com.telegram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private Long telegramChatId;

    private String telegramUsername;

    private Integer birthdayDay;

    private Integer birthdayMonth;

    private Boolean birthdayPrompted = false;

    private Integer lastBirthdayRewardYear;

    private Integer loyaltyPoints = 0;

    private Integer lifetimeLoyaltyPoints = 0;

    private Integer totalBookings = 0;

    private Integer completedBookings = 0;

    private Integer cancelledBookings = 0;

    private Integer noShowBookings = 0;

    private Long preferredServiceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
