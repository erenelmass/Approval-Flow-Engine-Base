package com.example.approval.repository;

import com.example.approval.model.ApprovalScenario;
import com.example.approval.model.Employee;
import com.example.approval.model.LeaveCalendar;
import com.example.approval.model.Organization;
import com.example.approval.model.Request;
import com.example.approval.model.RuleSet;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class H2ApprovalRepository implements AutoCloseable {
    private static final List<String> TABLE_NAMES = List.of(
            "employees",
            "cost_center_owners",
            "rule_sets",
            "approval_scenarios",
            "leave_delegations"
    );
    private static final Set<String> TABLE_NAME_SET = Set.copyOf(TABLE_NAMES);

    private final Connection connection;

    public H2ApprovalRepository() {
        try {
            String databaseName = "approval_engine_" + System.nanoTime();
            connection = DriverManager.getConnection(
                    "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1", "sa", "");
            initialize();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("could not initialize H2 database", exception);
        }
    }

    public ApprovalScenario loadScenario(String scenarioId) {
        try {
            ScenarioRow scenario = findScenario(scenarioId);
            return new ApprovalScenario(
                    new Request(scenario.id(), scenario.requester(), scenario.amount(),
                            scenario.costCenter(), scenario.category(), scenario.date()),
                    findRuleSet(scenario.ruleVersion()),
                    findOrganization(),
                    findLeaveCalendar());
        } catch (SQLException exception) {
            throw new IllegalStateException("could not load scenario " + scenarioId, exception);
        }
    }

    public void updateManager(String employee, String manager) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE employees SET manager = ? WHERE name = ?")) {
            statement.setString(1, manager);
            statement.setString(2, employee);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("unknown employee: " + employee);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("could not update manager", exception);
        }
    }

    public List<String> tableNames() {
        return TABLE_NAMES;
    }

    public String renderTable(String tableName) {
        validateTableName(tableName);

        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName);
             ResultSet result = statement.executeQuery()) {
            ResultSetMetaData metaData = result.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String[]> rows = new ArrayList<>();
            int[] widths = new int[columnCount];

            for (int column = 1; column <= columnCount; column++) {
                widths[column - 1] = metaData.getColumnLabel(column).length();
            }

            while (result.next()) {
                String[] row = new String[columnCount];
                for (int column = 1; column <= columnCount; column++) {
                    String value = result.getString(column);
                    row[column - 1] = value == null ? "NULL" : value;
                    widths[column - 1] = Math.max(widths[column - 1], row[column - 1].length());
                }
                rows.add(row);
            }

            StringBuilder output = new StringBuilder();
            appendBorder(output, widths);
            appendRow(output, widths, columnNames(metaData, columnCount));
            appendBorder(output, widths);
            for (String[] row : rows) {
                appendRow(output, widths, row);
            }
            appendBorder(output, widths);
            return output.toString();
        } catch (SQLException exception) {
            throw new IllegalStateException("could not render table " + tableName, exception);
        }
    }

    private void initialize() throws SQLException, IOException {
        executeScript("/schema.sql");
        executeScript("/data.sql");
    }

    private void validateTableName(String tableName) {
        if (!TABLE_NAME_SET.contains(tableName)) {
            throw new IllegalArgumentException("unknown table: " + tableName);
        }
    }

    private void executeScript(String resource) throws SQLException, IOException {
        try (InputStream stream = H2ApprovalRepository.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("missing database resource: " + resource);
            }
            String script = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    try (var statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }

    private ScenarioRow findScenario(String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM approval_scenarios WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("unknown scenario: " + id);
                }
                return new ScenarioRow(result.getString("id"), result.getString("requester"),
                        result.getBigDecimal("amount"), result.getString("cost_center"),
                        result.getString("category"), result.getDate("request_date").toLocalDate(),
                        result.getString("rule_version"));
            }
        }
    }

    private RuleSet findRuleSet(String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM rule_sets WHERE version = ?")) {
            statement.setString(1, version);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("unknown rule set: " + version);
                }
                return new RuleSet(result.getString("version"), result.getDate("valid_from").toLocalDate(),
                        result.getBigDecimal("medium_threshold"), result.getBigDecimal("high_threshold"),
                        findCostCenterOwners(), result.getString("finance_manager"),
                        result.getString("technology_director"), result.getString("ceo"));
            }
        }
    }

    private Map<String, String> findCostCenterOwners() throws SQLException {
        Map<String, String> owners = new HashMap<>();
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT cost_center, owner_name FROM cost_center_owners")) {
            while (result.next()) {
                owners.put(result.getString("cost_center"), result.getString("owner_name"));
            }
        }
        return owners;
    }

    private Organization findOrganization() throws SQLException {
        Map<String, Employee> employees = new HashMap<>();
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM employees")) {
            while (result.next()) {
                employees.put(result.getString("name"), new Employee(result.getString("name"),
                        result.getString("title"), result.getString("department"),
                        result.getString("manager")));
            }
        }
        return new Organization(employees);
    }

    private LeaveCalendar findLeaveCalendar() throws SQLException {
        List<LeaveCalendar.Leave> leaves = new ArrayList<>();
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM leave_delegations")) {
            while (result.next()) {
                leaves.add(new LeaveCalendar.Leave(result.getString("employee"),
                        result.getDate("leave_from").toLocalDate(),
                        result.getDate("leave_to").toLocalDate(), result.getString("delegate_name")));
            }
        }
        return new LeaveCalendar(leaves);
    }

    private String[] columnNames(ResultSetMetaData metaData, int columnCount) throws SQLException {
        String[] names = new String[columnCount];
        for (int column = 1; column <= columnCount; column++) {
            names[column - 1] = metaData.getColumnLabel(column);
        }
        return names;
    }

    private void appendBorder(StringBuilder output, int[] widths) {
        output.append('+');
        for (int width : widths) {
            output.append("-".repeat(width + 2)).append('+');
        }
        output.append(System.lineSeparator());
    }

    private void appendRow(StringBuilder output, int[] widths, String[] values) {
        output.append('|');
        for (int index = 0; index < values.length; index++) {
            output.append(' ')
                    .append(padRight(values[index], widths[index]))
                    .append(' ')
                    .append('|');
        }
        output.append(System.lineSeparator());
    }

    private String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private record ScenarioRow(String id, String requester, BigDecimal amount, String costCenter,
                               String category, LocalDate date, String ruleVersion) {
    }
}
