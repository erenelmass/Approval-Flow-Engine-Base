package com.example.approval.model;

public record ApprovalScenario(Request request, RuleSet ruleSet,
                                Organization organization, LeaveCalendar leaveCalendar) {
}
