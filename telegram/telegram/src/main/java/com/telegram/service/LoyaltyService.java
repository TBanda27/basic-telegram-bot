package com.telegram.service;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.Customer;
import com.telegram.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final CustomerRepository customerRepository;
    private final BarbershopConfig config;

    public void awardBookingPoints(Customer customer, boolean isFirstBooking) {
        int points = config.getLoyalty().getPointsPerBooking();

        if (isFirstBooking) {
            points += config.getLoyalty().getFirstBookingBonus();
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        customer.setLifetimeLoyaltyPoints(customer.getLifetimeLoyaltyPoints() + points);
        customerRepository.save(customer);

        log.info("Awarded {} points to customer {}", points, customer.getTelegramChatId());
    }

    public void incrementCompletedBookings(Customer customer) {
        customer.setCompletedBookings(customer.getCompletedBookings() + 1);
        customerRepository.save(customer);
    }

    public void incrementTotalBookings(Customer customer) {
        customer.setTotalBookings(customer.getTotalBookings() + 1);
        customerRepository.save(customer);
    }

    public void incrementCancelledBookings(Customer customer) {
        customer.setCancelledBookings(customer.getCancelledBookings() + 1);
        customerRepository.save(customer);
    }

    public Optional<Integer> checkMilestone(Customer customer) {
        List<Integer> milestones = config.getLoyalty().getMilestones();
        int completedBookings = customer.getCompletedBookings();

        if (milestones.contains(completedBookings)) {
            return Optional.of(completedBookings);
        }

        return Optional.empty();
    }

    public String buildPointsAwardedMessage(int points, boolean isFirstBooking, int totalPoints) {
        StringBuilder message = new StringBuilder();
        message.append("🎉 Points Earned!\n\n");

        if (isFirstBooking) {
            message.append(String.format("✨ First booking bonus: +%d points\n",
                    config.getLoyalty().getFirstBookingBonus()));
            message.append(String.format("💈 Booking points: +%d points\n",
                    config.getLoyalty().getPointsPerBooking()));
        } else {
            message.append(String.format("💈 Booking points: +%d points\n", points));
        }

        message.append(String.format("\n💰 Total points: %d", totalPoints));

        return message.toString();
    }

    public String buildMilestoneMessage(int milestone) {
        String reward = getMilestoneReward(milestone);

        return String.format("""
                🏆 Congratulations!

                You've reached %d completed bookings!

                %s

                Thank you for your loyalty! 🙏
                """,
                milestone,
                reward);
    }

    private String getMilestoneReward(int milestone) {
        return switch (milestone) {
            case 5 -> "🎁 You've unlocked: Free beard trim on your next visit!";
            case 10 -> "🎁 You've unlocked: 10% off your next booking!";
            case 25 -> "🎁 You've unlocked: Free haircut upgrade!";
            case 50 -> "🎁 You've unlocked: 25% off your next booking!";
            case 100 -> "🎁 You've unlocked: One FREE haircut! You're a VIP!";
            default -> "🎁 Thank you for being a valued customer!";
        };
    }

    public String buildLoyaltyStatusMessage(Customer customer) {
        int nextMilestone = getNextMilestone(customer.getCompletedBookings());
        int bookingsToNext = nextMilestone - customer.getCompletedBookings();

        return String.format("""
                💎 Your Loyalty Status

                💰 Current Points: %d
                ✂️ Completed Bookings: %d
                🏆 Lifetime Points: %d

                📊 Next milestone: %d bookings (%d to go!)
                """,
                customer.getLoyaltyPoints(),
                customer.getCompletedBookings(),
                customer.getLifetimeLoyaltyPoints(),
                nextMilestone,
                bookingsToNext);
    }

    private int getNextMilestone(int completedBookings) {
        List<Integer> milestones = config.getLoyalty().getMilestones();

        for (int milestone : milestones) {
            if (milestone > completedBookings) {
                return milestone;
            }
        }

        return milestones.get(milestones.size() - 1);
    }
}
