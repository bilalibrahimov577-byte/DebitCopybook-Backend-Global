package com.example.DebitCopybook.dao.entity;

import com.example.DebitCopybook.model.enums.HistoryEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Bu import vacibdir

import java.math.BigDecimal;
import java.time.LocalDateTime; // LocalDate yerinə LocalDateTime daha dəqiq olar

@Entity
@Table(name = "debt_history") // Cədvəl adları adətən kiçik hərflərlə və alt xətt ilə yazılır
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hansı borca aid olduğunu göstərmək üçün DebtEntity ilə əlaqə qururuq.
    // Bu, "çoxun birə" (many-to-one) əlaqəsidir: bir borcun çoxlu tarixçə qeydi ola bilər.
    @ManyToOne(fetch = FetchType.LAZY) // LAZY seçirik ki, performansı yaxşılaşdıraq
    @JoinColumn(name = "debt_id", nullable = false) // Verilənlər bazasında sütunun adı "debt_id" olacaq
    private DebtEntity debt;

    // Hadisənin növünü saxlamaq üçün Enum istifadə edirik. Bu, ən təmiz yoldur.
    @Enumerated(EnumType.STRING) // Verilənlər bazasında "CREATED", "UPDATED" kimi mətn şəklində saxlanacaq
    @Column(nullable = false)
    private HistoryEventType eventType;

    // Hadisənin detallı açıqlaması
    // Məsələn: "Borc yaradıldı", "10.00 AZN ödənildi", "Son tarix dəyişdirildi"
    @Column(nullable = false)
    private String description;

    // Əgər hadisə bir ödənişdirsə, ödəniş məbləğini burada saxlayırıq.
    // Digər hadisələr üçün bu sahə null ola bilər.
    private BigDecimal amount;

    // Hadisənin baş verdiyi dəqiq tarix və saat
    // @CreationTimestamp annotasiyası sayəsində bu sahə avtomatik olaraq
    // qeyd yaradılan anın tarixi ilə doldurulacaq. Bizim əl ilə set etməyimizə ehtiyac yoxdur.
  //  @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime eventDate;

}