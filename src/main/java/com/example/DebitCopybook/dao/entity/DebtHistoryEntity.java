package com.example.DebitCopybook.dao.entity;

import com.example.DebitCopybook.model.enums.HistoryEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "debt_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private DebtEntity debt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoryEventType eventType;


    @Column(nullable = false)
    private String description;

    private BigDecimal amount;


    @Column(nullable = false, updatable = false)
    private LocalDateTime eventDate;

}