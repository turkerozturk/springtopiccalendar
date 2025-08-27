package com.turkerozturk.dtt.repository;

import com.turkerozturk.dtt.entity.CategoryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryProfileRepository extends JpaRepository<CategoryProfile, Long> {
    Optional<CategoryProfile> findByName(String name);
}

