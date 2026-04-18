package com.thinkai.backend.repository;

import com.thinkai.backend.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    Optional<UserMemory> findByUserId(Long userId);
}