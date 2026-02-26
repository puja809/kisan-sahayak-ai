package com.farmer.mandi.repository;

import com.farmer.mandi.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByStateName(String stateName);
    Optional<State> findByStateCode(String stateCode);
}
