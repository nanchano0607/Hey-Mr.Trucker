package com.example.capshop.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capshop.domain.user.AuthProvider;
import com.example.capshop.domain.user.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndPhone(String name, String phone);
    Optional<User> findByEmailAndPhone(String email, String phone);
    boolean existsByPhone(String phone);
    Optional<User> findByOauthProviderAndProviderUserId(AuthProvider oauthProvider, String providerUserId);
}
