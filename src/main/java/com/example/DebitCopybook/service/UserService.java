package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // Yeni: @Value annotasiyası üçün import
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    // BILAL, bu hissəni burada elan edirik. `application.yml`-dən dəyəri alacaq. MÜTLƏQDİR.
    // Bu, sənin admin emailini proqrama daxil etməyə imkan verir.
    @Value("${admin.email}")
    private String adminEmail;

    public UserEntity findOrCreateUser(GoogleIdToken.Payload payload) {
        String googleId = payload.getSubject();
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        Optional<UserEntity> existingUser = userRepository.findByGoogleId(googleId);

        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            // Yeni istifadəçi yarat
            UserEntity newUser = new UserEntity();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);

            Set<String> roles = new HashSet<>();

            // BILAL, "if" şərtini burada dəyişdiririk. MÜTLƏQDİR.
            // Bu, ya verilənlər bazasında ilk istifadəçidirsə, ya da onun emaili "admin.email" ilə eynidirsə,
            // istifadəçiyə ADMIN rolunu verir.
            if (userRepository.count() == 0 || adminEmail.equals(email)) {
                roles.add("ROLE_ADMIN");
            }
            // Bütün istifadəçilər default olaraq USER roluna sahib olacaq.
            roles.add("ROLE_USER");
            newUser.setRoles(roles);

            return userRepository.save(newUser);
        }
    }

    public Optional<UserEntity> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı: " + username));
    }
}