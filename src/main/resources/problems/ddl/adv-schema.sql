-- DDL 고급 문제 스키마. 제출할 때마다 통째로 다시 적재된다. 외래키는 걸지 않는다.
DROP TABLE IF EXISTS invoice_lines;

CREATE TABLE invoice_lines (
    id INT PRIMARY KEY,
    invoice_no VARCHAR(20) NOT NULL,
    product VARCHAR(30) NOT NULL,
    unit_price INT NOT NULL,
    qty INT NOT NULL
);

INSERT INTO invoice_lines (id, invoice_no, product, unit_price, qty) VALUES
(1, 'INV-2501', '볼펜', 1500, 10),
(2, 'INV-2501', '노트', 3000, 4),
(3, 'INV-2502', '보조배터리', 32000, 1),
(4, 'INV-2502', 'USB 케이블', 8900, 3),
(5, 'INV-2503', '생수 2L', 1100, 24),
(6, 'INV-2503', '컵라면', 1300, 12),
(7, 'INV-2504', '이어폰', 45000, 2),
(8, 'INV-2505', '우산', 9800, 1);
