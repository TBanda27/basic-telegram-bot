package com.telegram.service;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.Customer;
import com.telegram.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BirthdayService {

    private final CustomerRepository customerRepository;
    private final BarbershopConfig config;

    public List<Customer> getCustomersWithBirthdayToday() {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int month = today.getMonthValue();
        int currentYear = today.getYear();

        return customerRepository.findByBirthdayDayAndBirthdayMonth(day, month)
                .stream()
                .filter(customer -> customer.getLastBirthdayRewardYear() == null
                        || customer.getLastBirthdayRewardYear() < currentYear)
                .toList();
    }

    public String buildBirthdayMessage(Customer customer) {
        String name = customer.getTelegramUsername() != null
                ? customer.getTelegramUsername()
                : "Valued Customer";

        return String.format("""
                🎂 Happy Birthday, %s! 🎉

                Wishing you an amazing day from all of us at %s!

                As a birthday gift, we're giving you:
                🎁 %d bonus loyalty points
                💰 %d%% off your next visit!

                Use code: %s

                Book your birthday treat today!
                """,
                name,
                config.getName(),
                config.getLoyalty().getBirthdayBonusPoints(),
                config.getLoyalty().getBirthdayDiscountPercent(),
                config.getLoyalty().getBirthdayDiscountCode());
    }

    public void awardBirthdayReward(Customer customer) {
        int bonusPoints = config.getLoyalty().getBirthdayBonusPoints();

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + bonusPoints);
        customer.setLifetimeLoyaltyPoints(customer.getLifetimeLoyaltyPoints() + bonusPoints);
        customer.setLastBirthdayRewardYear(LocalDate.now().getYear());

        customerRepository.save(customer);
    }
}
