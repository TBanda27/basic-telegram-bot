package com.telegram.scheduler;

import com.telegram.entity.Customer;
import com.telegram.service.BirthdayService;
import com.telegram.service.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayScheduler {

    private final BirthdayService birthdayService;
    private final MessageSender messageSender;

    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendBirthdayRewards() {
        log.info("Running birthday rewards job");

        List<Customer> customers = birthdayService.getCustomersWithBirthdayToday();
        log.info("Found {} customers with birthday today", customers.size());

        for (Customer customer : customers) {
            try {
                String message = birthdayService.buildBirthdayMessage(customer);
                messageSender.sendMessage(customer.getTelegramChatId(), message);
                birthdayService.awardBirthdayReward(customer);
                log.info("Sent birthday reward to customer {}", customer.getTelegramChatId());
            } catch (Exception e) {
                log.error("Failed to send birthday reward to customer {}: {}",
                        customer.getTelegramChatId(), e.getMessage());
            }
        }
    }
}
