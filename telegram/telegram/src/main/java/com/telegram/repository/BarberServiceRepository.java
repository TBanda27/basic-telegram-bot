package com.telegram.repository;

import com.telegram.entity.BarberService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    List<BarberService> findByActiveTrueOrderByDisplayOrder();

    Optional<BarberService> findBySlug(String slug);
}
