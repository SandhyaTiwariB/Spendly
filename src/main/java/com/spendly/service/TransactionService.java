package com.spendly.service;

import com.spendly.dto.TransactionPatchRequest;
import com.spendly.dto.TransactionRequest;
import com.spendly.dto.TransactionResponse;
import com.spendly.entity.Transaction;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;
import com.spendly.exception.ResourceNotFoundException;
import com.spendly.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategorizationService categorizationService;
    private final ParentControlService parentControlService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategorizationService categorizationService,
                              ParentControlService parentControlService) {
        this.transactionRepository = transactionRepository;
        this.categorizationService = categorizationService;
        this.parentControlService = parentControlService;
    }

    public List<TransactionResponse> getTransactions(String query, String category, LocalDate startDate, LocalDate endDate) {
        List<Transaction> list;
        if (startDate != null && endDate != null) {
            list = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(startDate, endDate);
            if (category != null && !category.trim().isEmpty()) {
                list = list.stream().filter(t -> t.getCategory().equalsIgnoreCase(category.trim())).collect(Collectors.toList());
            }
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                list = list.stream().filter(t -> t.getDescription().toLowerCase().contains(q) || t.getCategory().toLowerCase().contains(q)).collect(Collectors.toList());
            }
        } else if ((category != null && !category.trim().isEmpty()) || (query != null && !query.trim().isEmpty())) {
            list = transactionRepository.searchTransactions(
                    category != null && !category.trim().isEmpty() ? category.trim() : null,
                    query != null && !query.trim().isEmpty() ? query.trim() : null
            );
        } else {
            list = transactionRepository.findAllByOrderByTransactionDateDescIdDesc();
        }

        return list.stream().map(TransactionResponse::fromEntity).collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
        return TransactionResponse.fromEntity(tx);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Transaction tx = new Transaction();
        tx.setTransactionDate(request.getTransactionDate());
        tx.setDescription(request.getDescription().trim());
        tx.setAmount(request.getAmount());
        tx.setDirection(request.getDirection());
        tx.setReferenceNumber(request.getReferenceNumber());
        tx.setRunningBalance(request.getRunningBalance());

        boolean isTransfer = Boolean.TRUE.equals(request.getIsTransfer());

        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            tx.setCategory(request.getCategory().trim());
            tx.setManualCategory(true);
            if (isTransfer || request.getCategory().equalsIgnoreCase("Transfer")) {
                tx.setType(TransactionType.TRANSFER);
                tx.setTransfer(true);
            } else if (request.getDirection() == TransactionDirection.CREDIT || request.getCategory().equalsIgnoreCase("Income")) {
                tx.setType(TransactionType.INCOME);
                tx.setTransfer(false);
            } else {
                tx.setType(TransactionType.EXPENSE);
                tx.setTransfer(false);
            }
        } else {
            CategorizationService.CategoryResult res = categorizationService.categorize(request.getDescription(), request.getDirection());
            tx.setCategory(res.getCategory());
            tx.setType(res.getType());
            tx.setTransfer(res.isTransfer() || isTransfer);
            tx.setManualCategory(false);
        }

        String fingerprint = generateFingerprint(tx.getTransactionDate(), tx.getDescription(), tx.getAmount(), tx.getDirection().name(), tx.getReferenceNumber());
        tx.setFingerprint(fingerprint);

        if (tx.getType() == TransactionType.EXPENSE && !parentControlService.isCategoryAllowed(null, tx.getCategory())) {
            parentControlService.recordBlockedTransaction(
                    "Aarav's account",
                    tx.getDescription(),
                    tx.getAmount(),
                    tx.getCategory(),
                    "Category '" + tx.getCategory() + "' is currently restricted under Parent Control rules"
            );
            throw new IllegalArgumentException("Transaction blocked by Parent Control: Spending in '" + tx.getCategory() + "' is currently restricted.");
        }

        Transaction saved = transactionRepository.save(tx);
        return TransactionResponse.fromEntity(saved);
    }

    @Transactional
    public TransactionResponse patchTransaction(Long id, TransactionPatchRequest patch) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        if (patch.getDescription() != null && !patch.getDescription().trim().isEmpty()) {
            tx.setDescription(patch.getDescription().trim());
        }

        if (patch.getIsTransfer() != null) {
            tx.setTransfer(patch.getIsTransfer());
            if (patch.getIsTransfer()) {
                tx.setType(TransactionType.TRANSFER);
                tx.setCategory("Transfer");
            }
        }

        if (patch.getCategory() != null && !patch.getCategory().trim().isEmpty()) {
            String newCat = patch.getCategory().trim();
            tx.setCategory(newCat);
            tx.setManualCategory(true);

            if (newCat.equalsIgnoreCase("Transfer")) {
                tx.setType(TransactionType.TRANSFER);
                tx.setTransfer(true);
            } else if (newCat.equalsIgnoreCase("Income")) {
                tx.setType(TransactionType.INCOME);
                tx.setTransfer(false);
            } else {
                tx.setType(TransactionType.EXPENSE);
                tx.setTransfer(false);
            }
        }

        Transaction updated = transactionRepository.save(tx);
        return TransactionResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + id);
        }
        transactionRepository.deleteById(id);
    }

    private String generateFingerprint(Object date, String desc, Object amount, String direction, String ref) {
        try {
            String raw = String.valueOf(date) + "|" +
                         (desc != null ? desc.trim().toLowerCase() : "") + "|" +
                         String.valueOf(amount) + "|" +
                         String.valueOf(direction) + "|" +
                         (ref != null ? ref.trim() : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
