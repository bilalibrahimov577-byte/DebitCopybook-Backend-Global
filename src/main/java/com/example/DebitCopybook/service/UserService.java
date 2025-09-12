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


    @Value("${admin.email}")
    private String adminEmail;

//    public UserEntity findOrCreateUser(GoogleIdToken.Payload payload) {
//        String googleId = payload.getSubject();
//        String email = (String) payload.get("email");
//        String name = (String) payload.get("name");
//
//        Optional<UserEntity> existingUser = userRepository.findByGoogleId(googleId);
//
//        if (existingUser.isPresent()) {
//            return existingUser.get();
//        } else {
//
//            UserEntity newUser = new UserEntity();
//            newUser.setGoogleId(googleId);
//            newUser.setEmail(email);
//            newUser.setName(name);
//
//            Set<String> roles = new HashSet<>();
//
//
//            if (userRepository.count() == 0 || adminEmail.equals(email)) {
//                roles.add("ROLE_ADMIN");
//            }
//
//            roles.add("ROLE_USER");
//            newUser.setRoles(roles);
//
//            return userRepository.save(newUser);
//        }
//    }

    public UserEntity findOrCreateUser(GoogleIdToken.Payload payload) {
        String googleId = payload.getSubject();

        // Mövcud istifadəçini axtar
        Optional<UserEntity> existingUser = userRepository.findByGoogleId(googleId);

        // Əgər istifadəçi tapılıbsa, sadəcə onu qaytar
        if (existingUser.isPresent()) {
            return existingUser.get(); // Burda rolları yenidən təyin etmirik
        }

        // Əgər istifadəçi tapılmayıbsa, yeni istifadəçi yarat
        else {
            String email = (String) payload.get("email");
            String name = (String) payload.get("name");

            UserEntity newUser = new UserEntity();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);

            Set<String> roles = new HashSet<>();

            // Yalnız yeni istifadəçi yaradarkən rollar təyin edirik
            if (userRepository.count() == 0 || adminEmail.equals(email)) {
                roles.add("ROLE_ADMIN");
            }

            roles.add("ROLE_USER");
            newUser.setRoles(roles);

            // Yeni istifadəçini verilənlər bazasına yaz
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