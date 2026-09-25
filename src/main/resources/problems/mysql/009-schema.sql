DROP TABLE IF EXISTS subscriptions;

CREATE TABLE subscriptions (
    id INT PRIMARY KEY,
    customer VARCHAR(100) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    started_at DATE NOT NULL
);

INSERT INTO subscriptions (id, customer, plan, started_at) VALUES
    (1, '김철수', 'BASIC',   '2026-01-15'),
    (2, '이영희', 'PREMIUM', '2025-12-01'),
    (3, '박민수', 'BASIC',   '2026-03-10'),
    (4, '정수연', 'ENTERPRISE', '2024-08-20'),
    (5, '최지훈', 'PREMIUM', '2026-05-05');
