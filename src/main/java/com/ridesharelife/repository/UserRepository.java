package com.ridesharelife.repository;
import com.ridesharelife.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> { boolean existsByEmailIgnoreCase(String email); }
