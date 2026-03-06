package com.farmer.user.repository;

import com.farmer.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByFarmerId(String farmerId);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    List<User> findByIsActiveTrue();

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByFarmerId(String farmerId);

    List<User> findByRole(User.Role role);

    long countByRole(User.Role role);
}