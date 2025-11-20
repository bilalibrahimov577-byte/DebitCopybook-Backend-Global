package com.example.DebitCopybook.service;

import com.example.DebitCopybook.dao.entity.DebtEntity;
import com.example.DebitCopybook.dao.entity.DebtHistoryEntity;
import com.example.DebitCopybook.dao.entity.DebtUpdateProposalEntity;
import com.example.DebitCopybook.dao.entity.UserEntity;
import com.example.DebitCopybook.dao.repository.DebtHistoryRepository;
import com.example.DebitCopybook.dao.repository.DebtRepository;
import com.example.DebitCopybook.dao.repository.DebtUpdateProposalRepository;
import com.example.DebitCopybook.dao.repository.UserRepository;
import com.example.DebitCopybook.exception.DebtNotFoundException;
import com.example.DebitCopybook.exception.InvalidRequestException;
import com.example.DebitCopybook.exception.UserNotFoundException;
import com.example.DebitCopybook.model.enums.DebtStatus;
import com.example.DebitCopybook.model.enums.HistoryEventType;
import com.example.DebitCopybook.model.enums.ProposalStatus;
import com.example.DebitCopybook.model.mapper.DebtMapper;
import com.example.DebitCopybook.model.request.DebtRequestDto;
import com.example.DebitCopybook.model.request.SharedDebtRequestDto;
import com.example.DebitCopybook.model.request.SharedDebtResponseRequestDto;
import com.example.DebitCopybook.model.request.UpdateProposalRequestDto;
import com.example.DebitCopybook.model.response.DebtResponseDto;
import com.example.DebitCopybook.model.response.ProposalResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedDebtService {

    private final UserRepository userRepository;
    private final DebtRepository debtRepository;
    private final DebtMapper debtMapper;
    private final DebtUpdateProposalRepository proposalRepository;
    private final DebtHistoryRepository debtHistoryRepository;


    // Mövcud DebtService-də olan getCurrentUserId metodunu bura da əlavə edirik.
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserEntity)) {
            throw new IllegalStateException("Cari istifadəçi təyin olunmayıb.");
        }
        return ((UserEntity) authentication.getPrincipal()).getId();
    }

    // SharedDebtService.java

    @Transactional
    public DebtResponseDto createSharedDebtRequest(SharedDebtRequestDto requestDto) {
        Long requesterId = getCurrentUserId(); // Sorğunu göndərən (Mən)
        UserEntity requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("Sorğu göndərən istifadəçi tapılmadı."));

        // 1. Qarşı tərəfi onun borc ID-si ilə tapırıq.
        UserEntity counterparty = userRepository.findByDebtId(requestDto.getCounterpartyDebtId())
                .orElseThrow(() -> new UserNotFoundException("'" + requestDto.getCounterpartyDebtId() + "' ID-li istifadəçi tapılmadı."));

        // 2. İstifadəçinin özünə sorğu göndərməsinin qarşısını alırıq.
        if (requester.getId().equals(counterparty.getId())) {
            throw new IllegalArgumentException("İstifadəçi özünə borc sorğusu göndərə bilməz.");
        }

        // 3. Məlumatları DebtEntity-ə çeviririk.
        DebtRequestDto regularRequestDto = new DebtRequestDto();

        // ===== DƏYİŞİKLİK BURADADIR =====
        // `debtorName`-i artıq requestDto-dan yox, databazadan tapdığımız `counterparty`-nin adından götürürük!
        regularRequestDto.setDebtorName(counterparty.getName());

        // Qalan məlumatları köhnəsi kimi requestDto-dan götürürük
        regularRequestDto.setDebtAmount(requestDto.getDebtAmount());
        regularRequestDto.setDescription(requestDto.getDescription());
        regularRequestDto.setNotes(requestDto.getNotes());
        regularRequestDto.setDueYear(requestDto.getDueYear());
        regularRequestDto.setDueMonth(requestDto.getDueMonth());
        regularRequestDto.setIsFlexibleDueDate(requestDto.getIsFlexibleDueDate());

        DebtEntity debtEntity = debtMapper.mapRequestDtoToEntity(regularRequestDto);

        // --- ƏSAS MƏNTİQ ---
        debtEntity.setUser(requester); // Borcun sahibi (sorğunu göndərən)
        debtEntity.setCounterpartyUser(counterparty); // Borcun ikinci tərəfi
        debtEntity.setStatus(DebtStatus.PENDING_APPROVAL); // Status: TƏSDİQ GÖZLƏYƏN
        debtEntity.setRequestExpiryTime(LocalDateTime.now(ZoneOffset.ofHours(4)).plusSeconds(120));

        // 4. Bazada yadda saxlayırıq.
        DebtEntity savedDebt = debtRepository.save(debtEntity);

        // 5. Frontend-ə cavab qaytarırıq.
        return debtMapper.mapEntityToResponseDto(savedDebt);
    }

    @Transactional
    public DebtResponseDto respondToSharedDebtRequest(Long debtId, SharedDebtResponseRequestDto responseDto) {
        Long responderId = getCurrentUserId(); // Cavab verən istifadəçinin ID-si

        // 1. Sorğunu ID-sinə görə bazadan tapırıq.
        DebtEntity debtRequest = debtRepository.findById(debtId)
                .orElseThrow(() -> new DebtNotFoundException("Bu ID ilə borc sorğusu tapılmadı: " + debtId));

        // 2. ÇOX VACİB YOXLAMALAR:
        // a) Sorğunun statusu "Təsdiq Gözləyən" olmalıdır.
        if (debtRequest.getStatus() != DebtStatus.PENDING_APPROVAL) {
            throw new InvalidRequestException("Bu sorğuya artıq cavab verilib və ya etibarsızdır.");
        }

        // b) Cavab verən şəxsin, sorğunun göndərildiyi doğru adam olduğunu yoxlayırıq.
        if (!debtRequest.getCounterpartyUser().getId().equals(responderId)) {
            throw new SecurityException("Sizin bu sorğuya cavab vermək üçün icazəniz yoxdur.");
        }

        // c) Sorğunun vaxtının bitib-bitmədiyini yoxlayırıq (120 saniyə).
        if (debtRequest.getRequestExpiryTime().isBefore(LocalDateTime.now(ZoneOffset.ofHours(4)))) {
            // Vaxtı keçmiş sorğunu bazadan silirik ki, "zibil" yığılmasın.
            debtRepository.delete(debtRequest);
            throw new InvalidRequestException("Sorğunun cavab vermə müddəti (120 saniyə) bitib.");
        }


        // 3. Cavaba görə məntiqi icra edirik.
        if (responseDto.isAccepted()) {
            // --- ƏGƏR SORĞU QƏBUL EDİLİBSƏ ---
            debtRequest.setStatus(DebtStatus.CONFIRMED); // Statusu "Təsdiqlənmiş" olaraq dəyişirik.
            debtRequest.setRequestExpiryTime(null); // Bitmə vaxtını silirik, çünki artıq lazımsızdır.

            DebtEntity confirmedDebt = debtRepository.save(debtRequest);
            return debtMapper.mapEntityToResponseDto(confirmedDebt);
        } else {
            // --- ƏGƏR SORĞU RƏDD EDİLİBSƏ ---
            // Rədd edilmiş sorğu artıq lazımsızdır, ona görə bazadan tamamilə silirik.
            debtRepository.delete(debtRequest);

            // Frontend-ə borcun silindiyini bildirmək üçün xüsusi bir cavab qaytarırıq.
            // Bu, Flutter tərəfdə "Sorğu rədd edildi" mesajını göstərməyə kömək edəcək.
            return DebtResponseDto.builder()
                    .id(debtId)
                    .notes("Sorğu sizin tərəfinizdən rədd edildi və sistemdən silindi.")
                    .build();
        }
    }


    @Transactional
    public void createUpdateProposal(Long debtId, UpdateProposalRequestDto proposalDto) {
        Long proposerId = getCurrentUserId();
        UserEntity proposer = userRepository.findById(proposerId)
                .orElseThrow(() -> new UserNotFoundException("Təklif göndərən istifadəçi tapılmadı."));

        // 1. Dəyişdirilməsi təklif edilən əsas borcu tapırıq.
        DebtEntity debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new DebtNotFoundException("Bu ID ilə borc tapılmadı: " + debtId));

        // 2. TƏHLÜKƏSİZLİK YOXLAMALARI
        // a) Borc mütləq "CONFIRMED" statusunda olmalıdır.
        if (debt.getStatus() != DebtStatus.CONFIRMED) {
            throw new InvalidRequestException("Yalnız təsdiqlənmiş borclar üçün dəyişiklik təklif edilə bilər.");
        }

        // b) Təklifi göndərən şəxs borcun tərəflərindən biri olmalıdır.
        boolean isOwner = debt.getUser().getId().equals(proposerId);
        boolean isCounterparty = debt.getCounterpartyUser().getId().equals(proposerId);
        if (!isOwner && !isCounterparty) {
            throw new SecurityException("Sizin bu borc üçün dəyişiklik təklif etməyə icazəniz yoxdur.");
        }

        // 3. Yeni dəyişiklik təklifi obyektini yaradırıq.
        DebtUpdateProposalEntity proposal = new DebtUpdateProposalEntity();
        proposal.setDebt(debt);
        proposal.setProposerUser(proposer);
        proposal.setProposedAmount(proposalDto.getProposedAmount());
        proposal.setProposedNotes(proposalDto.getProposedNotes());
        proposal.setStatus(ProposalStatus.PENDING); // Status: TƏSDİQ GÖZLƏYƏN
        proposal.setRequestExpiryTime(LocalDateTime.now(ZoneOffset.ofHours(4)).plusSeconds(120)); // 120 saniyə ömrü var

        // 4. Təklifi bazada yadda saxlayırıq.
        proposalRepository.save(proposal);

        // Bu metod heç nə qaytarmır (void). Frontend-ə "200 OK" statusu getsə, deməli təklif uğurla yaradılıb.
    }


    @Transactional
    public DebtResponseDto respondToUpdateProposal(Long proposalId, SharedDebtResponseRequestDto responseDto) {
        Long responderId = getCurrentUserId(); // Təklifə cavab verən şəxs

        // 1. Təklifi ID-sinə görə bazadan tapırıq.
        DebtUpdateProposalEntity proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DebtNotFoundException("Bu ID ilə dəyişiklik təklifi tapılmadı: " + proposalId));

        DebtEntity debt = proposal.getDebt(); // Əlaqəli olduğu əsas borcu götürürük

        // 2. TƏHLÜKƏSİZLİK YOXLAMALARI
        // a) Təklifin statusu "PENDING" olmalıdır.
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new InvalidRequestException("Bu təklifə artıq cavab verilib və ya etibarsızdır.");
        }

        // b) Cavab verən şəxs, təklifi göndərən şəxs OLMAMALIDIR.
        if (proposal.getProposerUser().getId().equals(responderId)) {
            throw new SecurityException("İstifadəçi öz təklifini təsdiqləyə bilməz.");
        }

        // c) İcazə yoxlanışı.
        boolean isOwner = debt.getUser().getId().equals(responderId);
        boolean isCounterparty = debt.getCounterpartyUser().getId().equals(responderId);
        if (!isOwner && !isCounterparty) {
            throw new SecurityException("Sizin bu təklifə cavab vermək üçün icazəniz yoxdur.");
        }

        // d) Vaxt yoxlanışı (120 saniyə).
        if (proposal.getRequestExpiryTime().isBefore(LocalDateTime.now(ZoneOffset.ofHours(4)))) {
            proposal.setStatus(ProposalStatus.EXPIRED);
            proposalRepository.save(proposal);
            throw new InvalidRequestException("Təklifin cavab vermə müddəti (120 saniyə) bitib.");
        }

        // 3. Cavaba görə məntiqi icra edirik.
        if (responseDto.isAccepted()) {
            // --- ƏGƏR TƏKLİF QƏBUL EDİLİBSƏ ---

            // Dəyişiklikləri əsas borca tətbiq edirik (yadda saxlamamışdan əvvəl)
            BigDecimal oldAmount = debt.getDebtAmount(); // Tarixçə üçün köhnə məbləğ

            if (proposal.getProposedAmount() != null) {
                debt.setDebtAmount(proposal.getProposedAmount());
            }
            if (proposal.getProposedNotes() != null) {
                debt.setNotes(proposal.getProposedNotes());
            }

            // ===== ƏSAS DƏYİŞİKLİK BURADADIR: 0-A DÜŞƏNDƏ SİLİNMƏ =====
            // Əgər yeni məbləğ 0 və ya daha azdırsa
            if (debt.getDebtAmount().compareTo(BigDecimal.ZERO) <= 0) {

                // Təklifin statusunu dəyişirik (texniki olaraq lazımdır, amma onsuz da silinəcək)
                proposal.setStatus(ProposalStatus.ACCEPTED);

                // Borcu bazadan silirik.
                // Entity-lərin düzgün qurulubsa, bu əmr ona aid olan History-ni və Proposal-ları da siləcək.
                debtRepository.delete(debt);

                // Frontend-ə borcun bitdiyini bildirən boş bir cavab qaytarırıq
                return DebtResponseDto.builder()
                        .id(debt.getId())
                        .debtAmount(BigDecimal.ZERO)
                        .notes("Borc tam ödənildi və sistemdən silindi.")
                        .build();
            }
            // ============================================================

            // Əgər borc 0-dan böyükdürsə, adi qaydada davam edirik:
            proposal.setStatus(ProposalStatus.ACCEPTED);

            // Tarixçə mətni hazırlayırıq
            StringBuilder changesDescription = new StringBuilder("Dəyişiklik təsdiqləndi: \n");
            if (proposal.getProposedAmount() != null) {
                changesDescription.append("- Məbləğ ").append(oldAmount).append(" AZN-dən ")
                        .append(proposal.getProposedAmount()).append(" AZN-ə dəyişdirildi.\n");
            }
            if (proposal.getProposedNotes() != null) {
                changesDescription.append("- Qeyd dəyişdirildi.\n");
            }

            // Dəyişiklikləri borcun tarixçəsinə yazırıq
            DebtHistoryEntity historyEntry = DebtHistoryEntity.builder()
                    .debt(debt)
                    .eventType(HistoryEventType.UPDATED)
                    .description(changesDescription.toString())
                    .eventDate(LocalDateTime.now(ZoneOffset.ofHours(4)))
                    .build();

            debtHistoryRepository.save(historyEntry);
            debtRepository.save(debt);
            proposalRepository.save(proposal);

            return debtMapper.mapEntityToResponseDto(debt);

        } else {
            // --- ƏGƏR TƏKLİF RƏDD EDİLİBSƏ ---
            proposal.setStatus(ProposalStatus.REJECTED);
            proposalRepository.save(proposal);

            return debtMapper.mapEntityToResponseDto(debt);
        }
    }



    public List<DebtResponseDto> getConfirmedSharedDebts() {
        Long userId = getCurrentUserId();
        List<DebtEntity> debts = debtRepository.findConfirmedSharedDebtsForUser(userId);
        return debtMapper.mapEntityListToResponseDtoList(debts);
    }

    // Cari istifadəçiyə göndərilmiş və təsdiq gözləyən sorğuları qaytarır
    public List<DebtResponseDto> getPendingRequestsForMe() {
        Long userId = getCurrentUserId();
        List<DebtEntity> debts = debtRepository.findPendingRequestsForUser(userId);
        return debtMapper.mapEntityListToResponseDtoList(debts);
    }

    // Cari istifadəçinin göndərdiyi və hələ cavablandırılmamış sorğuları qaytarır
    public List<DebtResponseDto> getPendingRequestsISent() {
        Long userId = getCurrentUserId();
        List<DebtEntity> debts = debtRepository.findPendingRequestsSentByUser(userId);
        return debtMapper.mapEntityListToResponseDtoList(debts);
    }


    // --- IMPORTLARI UNUTMA ---
    // import com.example.DebitCopybook.model.response.ProposalResponseDto;

    // Mənə gələn dəyişiklik təkliflərini gətir
    public List<ProposalResponseDto> getPendingUpdateProposalsForMe() {
        Long currentUserId = getCurrentUserId();
        List<DebtUpdateProposalEntity> entities = proposalRepository.findIncomingProposals(currentUserId, ProposalStatus.PENDING);

        return entities.stream()
                .map(this::mapProposalEntityToResponseDto) // <-- Mapper dəyişdi
                .toList();
    }

    // Mənim göndərdiyim dəyişiklik təkliflərini gətir
    public List<ProposalResponseDto> getPendingUpdateProposalsISent() {
        Long currentUserId = getCurrentUserId();
        List<DebtUpdateProposalEntity> entities = proposalRepository.findOutgoingProposals(currentUserId, ProposalStatus.PENDING);

        return entities.stream()
                .map(this::mapProposalEntityToResponseDto) // <-- Mapper dəyişdi
                .toList();
    }

    // Köməkçi metod: Entity -> Response DTO çevrilməsi
    private ProposalResponseDto mapProposalEntityToResponseDto(DebtUpdateProposalEntity entity) {
        return ProposalResponseDto.builder()
                .id(entity.getId())
                .debtId(entity.getDebt().getId())
                .proposerName(entity.getProposerUser().getName()) // Təklifi göndərənin adı
                .originalAmount(entity.getDebt().getDebtAmount()) // Borcun indiki məbləği
                .proposedAmount(entity.getProposedAmount())       // Təklif olunan
                .originalNotes(entity.getDebt().getNotes())       // Borcun indiki qeydi
                .proposedNotes(entity.getProposedNotes())         // Təklif olunan
                .build();
    }

}
