package com.example.approval.engine;

import com.example.approval.model.ApprovalStep;
import com.example.approval.model.Employee;
import com.example.approval.model.LeaveCalendar;
import com.example.approval.model.Organization;
import com.example.approval.model.Request;
import com.example.approval.model.RuleSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ApprovalEngine {
    public List<ApprovalStep> buildChain(Request request, RuleSet rules,
                                          Organization organization, LeaveCalendar leaveCalendar) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(rules);
        Objects.requireNonNull(organization);
        Objects.requireNonNull(leaveCalendar);

        List<Candidate> candidates = amountChain(request, rules, organization);
        // Kategori kontrolleri tutar zincirinden sonra çalışır ve zorunlu rolleri ekler.
        addCategoryRequirements(candidates, request, rules);

        // Burada requester kontrol ediliyor: kişi kendi talebini onaylayamaz.
        Map<String, List<String>> uniqueApprovers = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            // Burada izinli kişinin vekili, gerekirse vekilinin vekili bulunuyor.
            String approver = leaveCalendar.resolve(candidate.employee(), request.date());
            if (approver.equals(request.requester())) {
                continue;
            }
            uniqueApprovers.computeIfAbsent(approver, ignored -> new ArrayList<>())
                    .add(candidate.role());
        }
            // LinkedHashMap zincir sırasını korurken aynı kişiyi tek adımda toplar.
        return uniqueApprovers.entrySet().stream()
                .map(entry -> new ApprovalStep(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<Candidate> amountChain(Request request, RuleSet rules, Organization organization) {
        Employee requester = organization.employee(request.requester());
        String manager = requester.manager();
        if (manager == null || manager.isBlank()) {
            throw new IllegalStateException("requester has no manager: " + requester.name());
        }

        List<Candidate> chain = new ArrayList<>();
        chain.add(new Candidate(manager, "REQUESTER_MANAGER"));
        // Exclusive üst sınırlar compareTo ile uygulanıyor: 10.000 ve 100.000 bir sonraki banda girer.
        if (request.amount().compareTo(rules.mediumThreshold()) >= 0) {
            String owner = rules.costCenterOwners().get(request.costCenter());
            if (owner == null) {
                throw new IllegalArgumentException("unknown cost center: " + request.costCenter());
            }
            chain.add(new Candidate(owner, "COST_CENTER_OWNER"));
            chain.add(new Candidate(rules.financeManager(), "FINANCE_MANAGER"));
        }
        if (request.amount().compareTo(rules.highThreshold()) >= 0) {
            chain.add(new Candidate(rules.ceo(), "CEO"));
        }
        return chain;
    }

    private void addCategoryRequirements(List<Candidate> chain, Request request, RuleSet rules) {
        if ("Danışmanlık".equals(request.category())) {
            insertBeforeCeoOrAtEnd(chain, new Candidate(rules.financeManager(), "FINANCE_MANAGER"));
        }
        if ("Yazılım Lisansı".equals(request.category()) && rules.version().equals("v2")) {
            insertBeforeCeoOrAtEnd(chain, new Candidate(rules.technologyDirector(), "TECHNOLOGY_DIRECTOR"));
        }
    }

    private void insertBeforeCeoOrAtEnd(List<Candidate> chain, Candidate required) {
        int ceoIndex = -1;
        for (int index = 0; index < chain.size(); index++) {
            if (chain.get(index).role().equals("CEO")) {
                ceoIndex = index;
                break;
            }
        }
        if (ceoIndex < 0) {
            chain.add(required);
        } else {
            chain.add(ceoIndex, required);
        }
    }

    private record Candidate(String employee, String role) { }
}
