package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // --> YENİ İMPORT
import java.security.SecureRandom;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random; // --> YENİ İMPORT
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Value("${admin.email}")
    private String adminEmail;


//
//    private String generateUniqueDebtId() {
//        Random random = new Random();
//        while (true) {
//            int number = random.nextInt(9000) + 1000;
//            String debtId = String.format("%02d-%02d", number / 100, number % 100);
//
//            // Bu ID-nin bazada başqası tərəfindən istifadə olunmadığını yoxlayırıq
//            if (!userRepository.existsByDebtId(debtId)) {
//                return debtId; // Əgər ID boşdadırsa, onu qaytarırıq
//            }
//            // Əgər ID tutulubsa, dövr (while) davam edir və yeni bir ID yaranır
//        }
//    }
    // --> YENİ KOD BİTDİ



    private String generateUniqueDebtId() {
        // Kriptoqrafik cəhətdən daha təhlükəsiz Generator
        SecureRandom random = new SecureRandom();
        int maxAttempts = 20;

        // 1. MƏRHƏLƏ: 4 Rəqəmli ID cəhd et (1000 - 9999)
        for (int i = 0; i < maxAttempts; i++) {
            int number = random.nextInt(9000) + 1000;
            String debtId = String.format("%02d-%02d", number / 100, number % 100);

            if (!userRepository.existsByDebtId(debtId)) {
                return debtId;
            }
        }

        // 2. MƏRHƏLƏ: Əgər 20 cəhdə 4 rəqəmli boş ID tapılmadısa (dolubsa), 5 Rəqəmliyə keç (10000 - 99999)
        for (int i = 0; i < maxAttempts; i++) {
            int number = random.nextInt(90000) + 10000;
            // Məsələn: 12345 -> "123-45" formatında
            String debtId = String.format("%03d-%02d", number / 100, number % 100);

            if (!userRepository.existsByDebtId(debtId)) {
                return debtId;
            }
        }

        // 3. İSTİSNA HALI: Nadir halda həm 4, həm 5 rəqəmlilər dolu olarsa və ya paralel sorğu münaqişəsi olarsa
        throw new IllegalStateException("Unikal Debt ID yaradıla bilmədi. Xahiş olunur yenidən cəhd edin.");
    }



    @Transactional // --> BU ANNOTASİYA VACİBDİR!
    public UserEntity findOrCreateUser(GoogleIdToken.Payload payload) {
        String googleId = payload.getSubject();
        Optional<UserEntity> existingUserOpt = userRepository.findByGoogleId(googleId);

        if (existingUserOpt.isPresent()) {
            UserEntity existingUser = existingUserOpt.get();

            // --> YENİ KOD BAŞLADI: Mövcud istifadəçinin debtId-sini yoxlayırıq
            if (existingUser.getDebtId() == null || existingUser.getDebtId().trim().isEmpty()) {
                existingUser.setDebtId(generateUniqueDebtId());
                return userRepository.save(existingUser); // Yenilənmiş istifadəçini yadda saxlayıb qaytarırıq
            }
            // --> YENİ KOD BİTDİ

            return existingUser; // Əgər ID-si varsa, heçnə etmədən qaytarırıq
        } else {
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            UserEntity newUser = new UserEntity();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setName(name);

            Set<String> roles = new HashSet<>();
            if (userRepository.count() == 0 || adminEmail.equals(email)) {
                roles.add("ROLE_ADMIN");
            }
            roles.add("ROLE_USER");
            newUser.setRoles(roles);

            // --> YENİ KOD BAŞLADI: Yeni istifadəçi üçün debtId yaradırıq
            newUser.setDebtId(generateUniqueDebtId());
            // --> YENİ KOD BİTDİ

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