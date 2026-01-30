package com.telegram.service;

import com.telegram.entity.BarberService;
import com.telegram.repository.BarberServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarberServiceService {

    private final BarberServiceRepository barberServiceRepository;

    public List<BarberService> getActiveServices() {
        return barberServiceRepository.findByActiveTrueOrderByDisplayOrder();
    }

    public Optional<BarberService> findBySlug(String slug) {
        return barberServiceRepository.findBySlug(slug);
    }

    public Optional<BarberService> findById(Long id) {
        return barberServiceRepository.findById(id);
    }
}
