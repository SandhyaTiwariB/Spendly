package com.spendly.service;

import com.spendly.dto.*;
import com.spendly.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ParentControlAndSocialFinanceTest {

    @Autowired
    private ParentControlService parentControlService;

    @Autowired
    private SplitBillService splitBillService;

    @Autowired
    private LendingObligationService lendingObligationService;

    @Autowired
    private AuthService authService;

    @Test
    void testAuthLoginAndRegistration() {
        // Test existing seeded user
        AuthResponse loginRes = authService.login(new AuthRequest("vrinda@spendly.io", "Spendly@2026"));
        assertTrue(loginRes.isSuccess());
        assertEquals("Vrinda Atluri", loginRes.getFullName());

        // Test new user registration
        RegisterRequest registerReq = new RegisterRequest("Test User", "testuser@spendly.io", "Password@123", "Password@123");
        AuthResponse regRes = authService.register(registerReq);
        assertTrue(regRes.isSuccess());
        assertNotNull(regRes.getToken());
    }

    @Test
    void testParentControlTogglesAndAuditLogging() {
        // Update category rule: disable Shopping
        ParentRule rule = parentControlService.updateRule("Aarav's account", "Shopping", new ParentRuleUpdateRequest(false, new BigDecimal("2000.00")));
        assertFalse(rule.isEnabled());

        // Verify rule is reflected in isCategoryAllowed
        assertFalse(parentControlService.isCategoryAllowed("Aarav's account", "Shopping"));

        // Verify audit log recorded the change
        Page<ParentActivityLog> logs = parentControlService.getActivityLogs("Aarav's account", "CATEGORY_PERMISSION_CHANGED", null, null, null, null, null, PageRequest.of(0, 10));
        assertTrue(logs.getTotalElements() > 0);
        assertTrue(logs.getContent().stream().anyMatch(l -> l.getDescription().contains("Shopping")));
    }

    @Test
    void testSplitBillsCreationAndSettlement() {
        SplitBillRequest request = new SplitBillRequest();
        request.setTitle("Weekend Dinner");
        request.setTotalAmount(new BigDecimal("1500.00"));
        request.setBillDate(LocalDate.now());
        request.setParticipants(List.of(
                new SplitBillRequest.ParticipantItem("Rahul", new BigDecimal("500.00"), false),
                new SplitBillRequest.ParticipantItem("Aadya", new BigDecimal("500.00"), false),
                new SplitBillRequest.ParticipantItem("Vrinda (You)", new BigDecimal("500.00"), true)
        ));

        SplitBill created = splitBillService.createBill(request);
        assertNotNull(created.getId());
        assertEquals(3, created.getParticipants().size());

        // Settle Rahul
        Long rahulId = created.getParticipants().stream().filter(p -> p.getName().equals("Rahul")).findFirst().get().getId();
        SplitBill settled = splitBillService.settleParticipant(created.getId(), rahulId);
        assertTrue(settled.getParticipants().stream().filter(p -> p.getId().equals(rahulId)).findFirst().get().isSettled());

        // Verify net balance calculation
        Map<String, Object> netMap = splitBillService.calculateNetBalance();
        assertNotNull(netMap.get("netBalance"));
    }

    @Test
    void testLendingAndRepayments() {
        LendingRequest request = new LendingRequest(
                "Sneha",
                ObligationType.LENT,
                new BigDecimal("3000.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "Travel advance"
        );

        LendingObligation obligation = lendingObligationService.createObligation(request);
        assertNotNull(obligation.getId());
        assertEquals(new BigDecimal("3000.00"), obligation.getBalance());

        // Partial repayment
        RepaymentRequest rep = new RepaymentRequest(new BigDecimal("1000.00"), LocalDate.now(), "Part 1 repayment");
        LendingObligation updated = lendingObligationService.recordRepayment(obligation.getId(), rep);
        assertEquals(new BigDecimal("2000.00"), updated.getBalance());

        // Settle remaining in full
        LendingObligation settled = lendingObligationService.markSettled(obligation.getId());
        assertEquals(BigDecimal.ZERO, settled.getBalance());
        assertEquals(ObligationStatus.SETTLED, settled.getStatus());
    }
}
