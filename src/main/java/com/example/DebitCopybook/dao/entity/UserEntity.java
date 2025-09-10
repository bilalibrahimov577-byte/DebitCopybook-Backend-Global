package com.example.DebitCopybook.dao.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set; // Yeni import: Rolları saxlamaq üçün Set istifadə edəcəyik
import java.util.stream.Collectors; // Yeni import

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    // BILAL, bu hissəni əlavə etməsək rollar saxlana bilməyəcək. MÜTLƏQDİR.
    // Rol üçün bir Enum və ya String istifadə edə bilərik. Sadəlik üçün String istifadə edirik.
    @ElementCollection(fetch = FetchType.EAGER) // Rollar adətən istifadəçi ilə birlikdə yüklənir
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles; // İstifadəçinin rolları (məsələn, "ROLE_USER", "ROLE_ADMIN")

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // BILAL, əvvəlki kodda sadəcə "ROLE_USER" qaytarılırdı. İndi isə dinamik rolları qaytarırıq. MÜTLƏQDİR.
        // Bu, Spring Security-yə istifadəçinin hansı səlahiyyətlərə malik olduğunu bildirir.
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}