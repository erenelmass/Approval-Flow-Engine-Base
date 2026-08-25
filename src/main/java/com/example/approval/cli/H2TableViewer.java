package com.example.approval.cli;

import com.example.approval.repository.H2ApprovalRepository;

public final class H2TableViewer {
    private H2TableViewer() {
    }

    public static void main(String[] args) throws Exception {
        try (H2ApprovalRepository repository = new H2ApprovalRepository()) {
            if (args.length == 0) {
                for (String tableName : repository.tableNames()) {
                    System.out.println(tableName);
                    System.out.println(repository.renderTable(tableName));
                }
                return;
            }

            for (String tableName : args) {
                System.out.println(tableName);
                System.out.println(repository.renderTable(tableName));
            }
        }
    }
}
