package com.example.approval;

import com.example.approval.engine.ApprovalEngine;
import com.example.approval.model.ApprovalScenario;
import com.example.approval.model.ApprovalStep;
import com.example.approval.repository.H2ApprovalRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2ApprovalRepositoryTest {
    private final ApprovalEngine engine = new ApprovalEngine();

    @Test
    void loadsAllSixCasesFromH2AndBuildsExpectedChains() throws Exception {
        try (H2ApprovalRepository repository = new H2ApprovalRepository()) {
            assertChain(repository, "1", List.of("Burak"));
            assertChain(repository, "2", List.of("Deniz", "Fatma"));
            assertChain(repository, "3", List.of("Deniz", "Fatma"));
            assertChain(repository, "4", List.of("Fatma", "Deniz", "Elif"));
            assertChain(repository, "5", List.of("Fatma"));
            assertChain(repository, "6", List.of("Elif"));
        }
    }

    @Test
    void managerCorrectionAffectsOnlyNewlyLoadedSnapshot() throws Exception {
        try (H2ApprovalRepository repository = new H2ApprovalRepository()) {
            ApprovalScenario beforeCorrection = repository.loadScenario("6");
            List<String> existingChain = approvers(beforeCorrection);

            repository.updateManager("Deniz", "Fatma");
            ApprovalScenario afterCorrection = repository.loadScenario("6");

            assertEquals(List.of("Elif"), existingChain);
            assertEquals(List.of("Fatma"), approvers(afterCorrection));
        }
    }

    @Test
    void rendersTablesAsAsciiGrids() throws Exception {
        try (H2ApprovalRepository repository = new H2ApprovalRepository()) {
            String employees = repository.renderTable("employees");

            assertTrue(employees.toLowerCase().contains("name"));
            assertTrue(employees.contains("Ayşe"));
            assertTrue(employees.contains("+"));
        }
    }

    private void assertChain(H2ApprovalRepository repository, String scenarioId,
                             List<String> expected) {
        assertEquals(expected, approvers(repository.loadScenario(scenarioId)));
    }

    private List<String> approvers(ApprovalScenario scenario) {
        return engine.buildChain(scenario.request(), scenario.ruleSet(),
                        scenario.organization(), scenario.leaveCalendar())
                .stream()
                .map(ApprovalStep::approver)
                .toList();
    }
}
