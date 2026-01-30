package com.telegram.service;

import com.telegram.entity.Customer;
import com.telegram.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer getOrCreateCustomer(Long chatId, String username) {
        Optional<Customer> existing = customerRepository.findById(chatId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Customer customer = Customer.builder()
                .telegramChatId(chatId)
                .telegramUsername(username)
                .birthdayPrompted(false)
                .loyaltyPoints(0)
                .lifetimeLoyaltyPoints(0)
                .totalBookings(0)
                .completedBookings(0)
                .cancelledBookings(0)
                .noShowBookings(0)
                .build();

        return customerRepository.save(customer);
    }

    public Optional<Customer> findByChatlId(Long chatId) {
        return customerRepository.findById(chatId);
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }
}
