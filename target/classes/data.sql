INSERT INTO employees VALUES
    ('Ayşe', 'Uzman', 'IT', 'Burak', FALSE),
    ('Burak', 'Müdür', 'IT', 'Deniz', FALSE),
    ('Deniz', 'Direktör', 'Teknoloji', 'Elif', FALSE),
    ('Can', 'Uzman', 'Finans', 'Fatma', FALSE),
    ('Fatma', 'Müdür', 'Finans', 'Elif', FALSE),
    ('Elif', 'CEO', 'Yönetim', NULL, TRUE);

INSERT INTO cost_center_owners VALUES
    ('IT-OPS', 'Burak'),
    ('PROJE-X', 'Deniz');

INSERT INTO rule_sets VALUES
    ('v1', DATE '2026-01-01', 10000, 100000, 'Fatma', 'Deniz', 'Elif'),
    ('v2', DATE '2026-03-15', 15000, 150000, 'Fatma', 'Deniz', 'Elif');

INSERT INTO approval_scenarios VALUES
    ('1', 'Ayşe', 8000, 'IT-OPS', 'Kırtasiye', DATE '2026-03-05', 'v1'),
    ('2', 'Ayşe', 45000, 'IT-OPS', 'Genel', DATE '2026-03-12', 'v1'),
    ('3', 'Burak', 60000, 'IT-OPS', 'Genel', DATE '2026-03-12', 'v1'),
    ('4', 'Can', 120000, 'IT-OPS', 'Danışmanlık', DATE '2026-03-13', 'v1'),
    ('5', 'Ayşe', 14000, 'IT-OPS', 'Yazılım Lisansı', DATE '2026-03-16', 'v2'),
    ('6', 'Deniz', 8000, 'IT-OPS', 'Genel', DATE '2026-03-18', 'v2');

INSERT INTO leave_delegations VALUES
    ('Burak', DATE '2026-03-10', DATE '2026-03-20', 'Deniz'),
    ('Deniz', DATE '2026-03-15', DATE '2026-03-25', 'Fatma');
