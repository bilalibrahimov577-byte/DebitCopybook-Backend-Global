package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.RecurringDebtEntity;
import com.example.DebitCopybook.model.enums.RecurringDueState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class RecurringDebtScheduleCalculator {

    private RecurringDebtScheduleCalculator() {}

    public record Snapshot(LocalDate nextDueDate,
                           BigDecimal amountDueNow,
                           BigDecimal overdueAmount,
                           BigDecimal nextPaymentAmount,
                           RecurringDueState state) {}

    public static Snapshot calculate(RecurringDebtEntity e, LocalDate today) {
        BigDecimal total = e.getTotalAmount();
        BigDecimal paid = e.getPaidAmount();
        BigDecimal inst = e.getInstallmentAmount();

        if (paid.compareTo(total) >= 0) {
            return new Snapshot(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    RecurringDueState.COMPLETED);
        }

        int totalPeriods = total.divide(inst, 0, RoundingMode.CEILING).intValue();
        int elapsed = Math.max(0, Math.min(totalPeriods,
                periodsElapsed(e.getFirstDueDate(), e.getDueDay(), today)));

        BigDecimal amountDueNow = nonNegative(cumulative(total, inst, elapsed).subtract(paid));

        // Növbəti ödənilməmiş dövrün nömrəsi (1-dən başlayır)
        int nextIdx = paid.divideToIntegralValue(inst).intValue() + 1;
        LocalDate nextDueDate = e.getFirstDueDate().plusMonths(nextIdx - 1L);

        if (amountDueNow.signum() == 0) {
            BigDecimal nextPayment = cumulative(total, inst, nextIdx).subtract(paid);
            return new Snapshot(nextDueDate, BigDecimal.ZERO, BigDecimal.ZERO, nextPayment,
                    RecurringDueState.OK);
        }

        // Buradan sonra elapsed >= 1 (ödənməli məbləğ var)
        LocalDate latestDue = e.getFirstDueDate().plusMonths(elapsed - 1L);
        BigDecimal carried = nonNegative(cumulative(total, inst, elapsed - 1).subtract(paid));

        if (today.isAfter(latestDue)) {
            // son ödəniş günü keçib, hələ ödənməyib
            return new Snapshot(nextDueDate, amountDueNow, amountDueNow, amountDueNow,
                    RecurringDueState.OVERDUE);
        }
        if (carried.signum() > 0) {
            // bu gün yeni hissənin günüdür, amma əvvəlki aydan gecikmə var
            return new Snapshot(nextDueDate, amountDueNow, carried, amountDueNow,
                    RecurringDueState.OVERDUE);
        }
        return new Snapshot(nextDueDate, amountDueNow, BigDecimal.ZERO, amountDueNow,
                RecurringDueState.DUE_TODAY);
    }

    // Bu günə qədər (bu gün daxil) neçə ödəniş günü çatıb
    private static int periodsElapsed(LocalDate firstDue, int dueDay, LocalDate today) {
        if (today.isBefore(firstDue)) return 0;
        int months = (today.getYear() - firstDue.getYear()) * 12
                + (today.getMonthValue() - firstDue.getMonthValue());
        return months + (today.getDayOfMonth() >= dueDay ? 1 : 0);
    }

    // idx dövrünə qədər ödənməli ümumi məbləğ (son dövrdə qalıq: min(total, ...))
    private static BigDecimal cumulative(BigDecimal total, BigDecimal inst, int idx) {
        return total.min(inst.multiply(BigDecimal.valueOf(idx)));
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        return v.signum() < 0 ? BigDecimal.ZERO : v;
    }
}