package com.telegram.repository;

import com.telegram.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBirthdayDayAndBirthdayMonth(Integer day, Integer month);
}
