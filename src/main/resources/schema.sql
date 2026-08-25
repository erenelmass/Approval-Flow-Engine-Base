CREATE TABLE employees (
    name VARCHAR(100) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    manager VARCHAR(100),
    is_ceo BOOLEAN NOT NULL
);

CREATE TABLE cost_center_owners (
    cost_center VARCHAR(100) PRIMARY KEY,
    owner_name VARCHAR(100) NOT NULL
);

CREATE TABLE rule_sets (
    version VARCHAR(50) PRIMARY KEY,
    valid_from DATE NOT NULL,
    medium_threshold DECIMAL(18, 2) NOT NULL,
    high_threshold DECIMAL(18, 2) NOT NULL,
    finance_manager VARCHAR(100) NOT NULL,
    technology_director VARCHAR(100) NOT NULL,
    ceo VARCHAR(100) NOT NULL
);

CREATE TABLE approval_scenarios (
    id VARCHAR(50) PRIMARY KEY,
    requester VARCHAR(100) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    cost_center VARCHAR(100),
    category VARCHAR(100) NOT NULL,
    request_date DATE NOT NULL,
    rule_version VARCHAR(50) NOT NULL
);

CREATE TABLE leave_delegations (
    employee VARCHAR(100) NOT NULL,
    leave_from DATE NOT NULL,
    leave_to DATE NOT NULL,
    delegate_name VARCHAR(100),
    PRIMARY KEY (employee, leave_from, leave_to)
);
