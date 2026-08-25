package com.example.approval;

import com.example.approval.engine.ApprovalEngine;
import com.example.approval.model.ApprovalStep;
import com.example.approval.model.Employee;
import com.example.approval.model.LeaveCalendar;
import com.example.approval.model.Organization;
import com.example.approval.model.Request;
import com.example.approval.model.RuleSet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApprovalEngineTest {
    private static final LocalDate MARCH_5 = LocalDate.of(2026, 3, 5);
    private static final LocalDate MARCH_12 = LocalDate.of(2026, 3, 12);
    private static final LocalDate MARCH_13 = LocalDate.of(2026, 3, 13);
    private static final LocalDate MARCH_16 = LocalDate.of(2026, 3, 16);
    private final ApprovalEngine engine = new ApprovalEngine();
    private final Organization organization = organization();
    private final RuleSet v1 = RuleSet.standardV1(Map.of("IT-OPS", "Burak", "PROJE-X", "Deniz"));
    private final RuleSet v2 = RuleSet.standardV2(Map.of("IT-OPS", "Burak", "PROJE-X", "Deniz"));

    @Test
    void scenario1_usesRequesterManagerBelowThreshold() {
        // Case 1: 10.000 TL altındaki taleplerde yalnızca talep sahibinin yöneticisi onaycı olmalıdır.
        // Ayşe'nin yöneticisi Burak olduğu için beklenen zincir tek kişiden oluşur.
        assertNames(request("1", "Ayşe", "8000", "Kırtasiye", MARCH_5), v1,
                List.of("Burak"), LeaveCalendar.empty());
    }

    @Test
    void scenario2_buildsManagerOwnerFinanceChain() {
        // Case 2: v1 eşiklerine göre 45.000 TL, 10.000-100.000 TL aralığındadır.
        // Zincir yönetici -> masraf merkezi sahibi -> finans müdürü olmalıdır.
        // Ayşe'nin yöneticisi Burak aynı zamanda IT-OPS sahibi olduğu için deduplication uygulanır.
        assertNames(request("2", "Ayşe", "45000", "Genel", MARCH_12), v1,
                List.of("Burak", "Fatma"), LeaveCalendar.empty());
    }

    @Test
    void scenario3_skipsRequesterAndDeduplicatesTheirOwnerRole() {
        // Case 3: Talep sahibi Burak, IT-OPS masraf merkezi sahibidir.
        // Burak kendi talebini onaylayamayacağı için yönetici adımı atlanır;
        // aynı kişi olan masraf merkezi sahibi rolü de tek bir onay adımı olarak değerlendirilir.
        assertNames(request("3", "Burak", "60000", "Genel", MARCH_12), v1,
                List.of("Deniz", "Fatma"), LeaveCalendar.empty());
    }

    @Test
    void scenario4_keepsRuleSnapshotAndDeduplicatesRepeatedFinanceManager() {
        // Case 4: Talep 13 Mart'ta başladığı için v1 kural seti kullanılmaya devam eder.
        // 120.000 TL, CEO adımını gerektirir; Danışmanlık kategorisi de finans müdürünü zorunlu kılar.
        // Can'ın yöneticisi Fatma zaten finans müdürü olduğundan bu rol iki kez eklenmez.
        assertNames(request("4", "Can", "120000", "Danışmanlık", MARCH_13), v1,
                List.of("Fatma", "Burak", "Elif"), LeaveCalendar.empty());
    }

    @Test
    void scenario5_reSubmittedRequestUsesV2AndSoftwareLicenseRequirement() {
        // Case 5: Reddedilen talep yeniden gönderildiğinde yeni gönderim tarihi esas alınır.
        // 16 Mart tarihi v2'yi seçer; 14.000 TL temel eşikte yönetici zincirini oluşturur.
        // Yazılım Lisansı kuralı teknoloji direktörünü zorunlu kılar ve Burak izinli olduğu için
        // vekili Deniz'e yönlendirilir.
        assertNames(request("5", "Ayşe", "14000", "IT-OPS", MARCH_16), v2,
            List.of("Deniz"), LeaveCalendar.of(
                new LeaveCalendar.Leave("Burak", MARCH_16, MARCH_16, "Deniz")));
    }

    @Test
    void scenario6_existingChainRetainsOrganizationSnapshotAfterCorrection() {
        // Case 6: Akış başladıktan sonra organizasyon şeması değişse bile mevcut talebin
        // daha önce üretilmiş zinciri değişmemelidir. Bu nedenle mevcut zincir Elif'i korur.
        // Yeni talep ise düzeltilmiş organizasyon verisini kullanır ve Fatma'ya yönlenir.
        Request request = request("6", "Deniz", "8000", "IT-OPS", LocalDate.of(2026, 3, 18));
        LeaveCalendar leaveCalendar = LeaveCalendar.of(
            new LeaveCalendar.Leave("Deniz", LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 25), "Fatma"));
        List<ApprovalStep> beforeCorrection = engine.buildChain(request, v2, organization, leaveCalendar);

        Organization corrected = organizationWithDenizManagerFatma();
        assertEquals(List.of("Elif"), beforeCorrection.stream().map(ApprovalStep::approver).toList());
        assertEquals(List.of("Fatma"), engine.buildChain(
            request("6-new", "Deniz", "8000", "IT-OPS", LocalDate.of(2026, 3, 20)),
            v2, corrected, leaveCalendar).stream().map(ApprovalStep::approver).toList());
        assertEquals("Burak", organization.employee("Ayşe").manager());
        assertEquals("Elif", organization.employee("Deniz").manager());
        assertEquals("Fatma", corrected.employee("Deniz").manager());
    }

    @Test
    void delegationIsRecursiveAndDetectsCycles() {
        // Edge case: Burak izinliyken vekili Deniz'e, Deniz de izinliyken kendi vekili Fatma'ya geçilir.
        // Böylece vekalet çözümlemesinin recursive çalıştığı doğrulanır.
        LeaveCalendar recursive = LeaveCalendar.of(
                new LeaveCalendar.Leave("Burak", MARCH_16, MARCH_16, "Deniz"),
                new LeaveCalendar.Leave("Deniz", MARCH_16, MARCH_16, "Fatma"));
        assertEquals("Fatma", recursive.resolve("Burak", MARCH_16));

        // Aynı vekalet zincirinde tekrar başlangıç kişisine dönülmesi sonsuz recursion oluşturabilir.
        // Motorun bu durumu fark edip açık bir hata vermesi beklenir.
        LeaveCalendar cyclic = LeaveCalendar.of(
                new LeaveCalendar.Leave("Burak", MARCH_16, MARCH_16, "Deniz"),
                new LeaveCalendar.Leave("Deniz", MARCH_16, MARCH_16, "Burak"));
        assertThrows(IllegalStateException.class, () -> cyclic.resolve("Burak", MARCH_16));
    }

    private void assertNames(Request request, RuleSet rules, List<String> expected,
                             LeaveCalendar leaveCalendar) {
        assertEquals(expected, engine.buildChain(request, rules, organization, leaveCalendar)
                .stream().map(ApprovalStep::approver).toList());
    }

    private static Request request(String id, String requester, String amount,
                                   String category, LocalDate date) {
        return new Request(id, requester, new BigDecimal(amount), "IT-OPS", category, date);
    }

    private static Organization organization() {
        return new Organization(Map.of(
                "Ayşe", new Employee("Ayşe", "Uzman", "IT", "Burak"),
                "Burak", new Employee("Burak", "Müdür", "IT", "Deniz"),
                "Deniz", new Employee("Deniz", "Direktör", "Teknoloji", "Elif"),
                "Can", new Employee("Can", "Uzman", "Finans", "Fatma"),
                "Fatma", new Employee("Fatma", "Müdür", "Finans", "Elif"),
                "Elif", new Employee("Elif", "CEO", "Yönetim", null)));
    }

    private static Organization organizationWithDenizManagerFatma() {
        Organization original = organization();
        Map<String, Employee> employees = new java.util.HashMap<>(original.employees());
        employees.put("Deniz", new Employee("Deniz", "Direktör", "Teknoloji", "Fatma"));
        return new Organization(employees);
    }
}
