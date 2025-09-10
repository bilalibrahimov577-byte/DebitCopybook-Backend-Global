package com.example.DebitCopybook.service; // Paketi öz proyektinin adına uyğun dəyişdir

import com.example.DebitCopybook.dao.entity.UserEntity; // Sizin UserEntity adınız
import io.jsonwebtoken.Claims;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // BILAL, bu açar application.yml-də təyin edilmiş JWT_SECRET_KEY olacaq.
    // Bu açar olmadan JWT tokenləri imzalamaq və doğrulamaq İŞLƏMƏYƏCƏK.
    @Value("${jwt.secret-key}")
    private String secretKey;

    // BILAL, bu müddət application.yml-də təyin edilmiş JWT_EXPIRATION olacaq.
    // Bu müddət tokenin nə qədər etibarlı olacağını müəyyən edir.
    @Value("${jwt.expiration}")
    private long jwtExpiration; // Milli saniyə ilə

    // BILAL, bu metod daxil olmuş istifadəçi üçün JWT token yaradır.
    // Bunu əlavə etməsək Flutter tətbiqinə token göndərilməyəcək və istifadəçi daxil ola bilməyəcək.
    public String generateToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId()); // Tokenin daxilində istifadəçi ID-sini saxlayırıq
        claims.put("email", user.getEmail()); // Tokenin daxilində email-i saxlayırıq
        claims.put("name", user.getName()); // Tokenin daxilində adı da saxlaya bilərik
        // Əlavə məlumatlar əlavə edə bilərsiniz (məsələn, rollar)
        return buildToken(claims, user.getEmail(), jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject, // Burda email istifadə edirik
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(subject) // Tokenin subyekti (bizim halımızda istifadəçinin email-i)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Tokenin yaradılma vaxtı
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Tokenin bitmə vaxtı
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Tokeni gizli açarımızla imzalayırıq
                .compact(); // Tokeni string formasına çeviririk
    }

    // BILAL, bu metod JWT tokenin etibarlı olub-olmadığını yoxlayır.
    // Bunu əlavə etməsək, hər gələn istəyin tokenini doğrulaya bilməyəcəyik və təhlükəsizlik İŞLƏMƏYƏCƏK.
    public boolean isTokenValid(String token, UserEntity userDetails) {
        final String usernameInToken = extractUsername(token); // Tokendəki istifadəçi adını (emaili) çıxarırıq
        return (usernameInToken.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date()); // Tokenin vaxtının bitib-bitmədiyini yoxlayırıq
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration); // Tokenin bitmə vaxtını çıxarırıq
    }

    // BILAL, bu metod tokendən istifadəçi adını (emaili) çıxarır.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // Subject olaraq email saxladığımız üçün onu çıxarırıq
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignInKey()) // Tokeni doğrulamaq üçün gizli açarımızdan istifadə edirik
                .build()
                .parseClaimsJws(token) // Tokeni parse edirik
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // Gizli açarı Base64-dən decode edirik
        return Keys.hmacShaKeyFor(keyBytes); // HMAC-SHA256 üçün açar yaradırıq
    }
}