package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.RecurringDebtEntity;
import com.example.DebitCopybook.dao.entity.RecurringDebtPaymentEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.RecurringDebtPaymentRepository;
import com.example.DebitCopybook.dao.repository.RecurringDebtRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.exception.RecurringDebtException;
import com.example.DebitCopybook.model.enums.RecurringDebtStatus;
import com.example.DebitCopybook.model.request.RecurringDebtRequestDto;
import com.example.DebitCopybook.model.response.RecurringDebtPaymentResponseDto;
import com.example.DebitCopybook.model.response.RecurringDebtResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.DebitCopybook.constants.DebtConstants.DEBT_TO_ME_DESCRIPTION;
import static com.example.DebitCopybook.constants.DebtConstants.MY_DEBT_DESCRIPTION;

@Service
@RequiredArgsConstructor
public class RecurringDebtService {

    private static final ZoneId BAKU_ZONE = ZoneId.of("Asia/Baku");
    private static final int MAX_PERIODS = 360;

    private final RecurringDebtRepository recurringDebtRepository;
    private final RecurringDebtPaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserEntity)) {
            throw new IllegalStateException("Cari istifadəçi təyin olunmayıb.");
        }
        return ((UserEntity) authentication.getPrincipal()).getId();
    }

    private LocalDate today() {
        return LocalDate.now(BAKU_ZONE);
    }

    private BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public RecurringDebtResponseDto create(RecurringDebtRequestDto dto) {
        Long userId = getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DebtNotFoundException("İstifadəçi tapılmadı ID: " + userId));

        // Limit: mövcud sadə borclar + aktiv təkrarlanan borclar (DebtService-ə toxunmadan)
        boolean hasSub = subscriptionService.hasActiveSubscription(userId);
        int limit = (user.isAdmin() || hasSub) ? 100 : 3;
        long count = debtRepository.countByUserId(userId)
                + recurringDebtRepository.countByUserIdAndStatus(userId, RecurringDebtStatus.ACTIVE);
        if (count >= limit) {
            throw new RecurringDebtException(
                    "Siz artıq borc limitinə çatmısınız. Daha çox borc əlavə etmək üçün premiuma keçin.");
        }

        String direction = dto.getDirection().trim();
        if (!MY_DEBT_DESCRIPTION.equals(direction) && !DEBT_TO_ME_DESCRIPTION.equals(direction)) {
            throw new RecurringDebtException("Borcun növü yanlışdır.");
        }

        BigDecimal total = money(dto.getTotalAmount());
        BigDecimal installment = money(dto.getInstallmentAmount());
        if (total.signum() <= 0 || installment.signum() <= 0) {
            throw new RecurringDebtException("Məbləğ 0 manatdan çox olmalıdır.");
        }
        if (installment.compareTo(total) > 0) {
            throw new RecurringDebtException("Hissə məbləği ümumi məbləğdən çox ola bilməz.");
        }
        int periods = total.divide(installment, 0, RoundingMode.CEILING).intValue();
        if (periods > MAX_PERIODS) {
            throw new RecurringDebtException("Ödəniş sayı çox böyükdür (maksimum " + MAX_PERIODS + " ay).");
        }

        // İlk ödəniş tarixi: seçilən gün bu ay hələ keçməyibsə bu ay, keçibsə gələn ay
        LocalDate today = today();
        int dueDay = dto.getDueDay();
        LocalDate firstDue = today.getDayOfMonth() <= dueDay
                ? today.withDayOfMonth(dueDay)
                : today.plusMonths(1).withDayOfMonth(dueDay);

        RecurringDebtEntity entity = RecurringDebtEntity.builder()
                .userId(userId)
                .debtorName(dto.getDebtorName().trim())
                .direction(direction)
                .totalAmount(total)
                .paidAmount(money(BigDecimal.ZERO))
                .installmentAmount(installment)
                .dueDay(dueDay)
                .firstDueDate(firstDue)
                .status(RecurringDebtStatus.ACTIVE)
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now(BAKU_ZONE))
                .build();

        return toResponse(recurringDebtRepository.save(entity), today);
    }

    @Transactional(readOnly = true)
    public List<RecurringDebtResponseDto> getAll() {
        Long userId = getCurrentUserId();
        LocalDate today = today();
        return recurringDebtRepository.findAllByUserIdOrderByStatusAscCreatedAtDesc(userId).stream()
                .map(e -> toResponse(e, today))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringDebtResponseDto getById(Long id) {
        return toResponse(findOwned(id), today());
    }

    @Transactional
    public RecurringDebtResponseDto pay(Long id, BigDecimal rawAmount) {
        if (rawAmount == null) {
            throw new RecurringDebtException("Ödəniş məbləği boş ola bilməz.");
        }
        BigDecimal amount = money(rawAmount);
        if (amount.signum() <= 0) {
            throw new RecurringDebtException("Ödəniş məbləği müsbət olmalıdır.");
        }

        Long userId = getCurrentUserId();
        RecurringDebtEntity entity = recurringDebtRepository.findByIdAndUserIdForUpdate(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));

        if (entity.getStatus() == RecurringDebtStatus.COMPLETED) {
            throw new RecurringDebtException("Bu borc artıq tam ödənilib.");
        }

        BigDecimal remaining = entity.getTotalAmount().subtract(entity.getPaidAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new RecurringDebtException("Ödəniş məbləği qalan borcdan (" + remaining + " AZN) çox ola bilməz.");
        }

        BigDecimal newPaid = entity.getPaidAmount().add(amount);
        BigDecimal remainingAfter = entity.getTotalAmount().subtract(newPaid);
        LocalDateTime now = LocalDateTime.now(BAKU_ZONE);

        entity.setPaidAmount(newPaid);
        if (remainingAfter.signum() == 0) {
            entity.setStatus(RecurringDebtStatus.COMPLETED);
            entity.setCompletedAt(now);
        }
        recurringDebtRepository.save(entity);

        paymentRepository.save(RecurringDebtPaymentEntity.builder()
                .recurringDebtId(entity.getId())
                .amount(amount)
                .remainingAfter(remainingAfter)
                .paidAt(now)
                .build());

        return toResponse(entity, today());
    }

    @Transactional(readOnly = true)
    public List<RecurringDebtPaymentResponseDto> getPayments(Long id) {
        findOwned(id);
        return paymentRepository.findAllByRecurringDebtIdOrderByPaidAtDesc(id).stream()
                .map(p -> RecurringDebtPaymentResponseDto.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .remainingAfter(p.getRemainingAfter())
                        .paidAt(p.getPaidAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        RecurringDebtEntity entity = findOwned(id);
        paymentRepository.deleteAllByRecurringDebtId(entity.getId());
        recurringDebtRepository.delete(entity);
    }

    private RecurringDebtEntity findOwned(Long id) {
        Long userId = getCurrentUserId();
        return recurringDebtRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DebtNotFoundException("Borc ID " + id + " ilə tapılmadı..."));
    }

    private RecurringDebtResponseDto toResponse(RecurringDebtEntity e, LocalDate today) {
        RecurringDebtScheduleCalculator.Snapshot s = RecurringDebtScheduleCalculator.calculate(e, today);
        BigDecimal remaining = e.getTotalAmount().subtract(e.getPaidAmount());
        if (remaining.signum() < 0) remaining = BigDecimal.ZERO;

        return RecurringDebtResponseDto.builder()
                .id(e.getId())
                .debtorName(e.getDebtorName())
                .direction(e.getDirection())
                .totalAmount(e.getTotalAmount())
                .paidAmount(e.getPaidAmount())
                .remainingAmount(remaining)
                .installmentAmount(e.getInstallmentAmount())
                .dueDay(e.getDueDay())
                .nextDueDate(s.nextDueDate())
                .amountDueNow(s.amountDueNow())
                .overdueAmount(s.overdueAmount())
                .nextPaymentAmount(s.nextPaymentAmount())
                .dueState(s.state().name())
                .status(e.getStatus().name())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }
}