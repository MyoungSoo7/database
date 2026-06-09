DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id INT PRIMARY KEY,
    customer VARCHAR(100) NOT NULL,
    amount INT NOT NULL,
    ordered_at DATETIME NOT NULL
);

INSERT INTO orders (id, customer, amount, ordered_at) VALUES
    (1, '김철수', 15000, '2026-01-12 10:23:00'),
    (2, '이영희', 28000, '2026-02-05 14:11:00'),
    (3, '박민수', 33000, '2026-02-19 09:45:00'),
    (4, '정수연', 12000, '2026-03-03 16:30:00'),
    (5, '최지훈', 41000, '2026-03-21 11:00:00');
