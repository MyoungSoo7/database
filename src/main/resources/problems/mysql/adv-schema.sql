DROP TABLE IF EXISTS cart_orders;
DROP TABLE IF EXISTS org_units;

-- 주문 한 건의 상품 목록을 JSON 배열로 저장
CREATE TABLE cart_orders (
    id INT PRIMARY KEY,
    customer VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL,     -- 'PAID' 또는 'CANCELLED'
    items JSON NOT NULL              -- [{"sku": ..., "qty": ..., "price": ...}, ...]
);

INSERT INTO cart_orders (id, customer, status, items) VALUES
(1, '김민준', 'PAID',      '[{"sku": "PEN", "qty": 3, "price": 1500}, {"sku": "NOTE", "qty": 2, "price": 3000}]'),
(2, '이서연', 'PAID',      '[{"sku": "MUG", "qty": 1, "price": 12000}]'),
(3, '박도윤', 'CANCELLED', '[{"sku": "PEN", "qty": 10, "price": 1500}, {"sku": "MUG", "qty": 2, "price": 12000}]'),
(4, '최지우', 'PAID',      '[{"sku": "NOTE", "qty": 5, "price": 3000}, {"sku": "PEN", "qty": 1, "price": 1500}, {"sku": "BAG", "qty": 1, "price": 45000}]'),
(5, '김민준', 'PAID',      '[{"sku": "MUG", "qty": 2, "price": 11000}]'),
(6, '정하준', 'PAID',      '[]'),
(7, '강서아', 'PAID',      '[{"sku": "PEN", "qty": 4, "price": 1400}, {"sku": "NOTE", "qty": 1, "price": 3000}]');

-- 조직 계층 (parent_id → org_units.id, 최상위는 NULL)
CREATE TABLE org_units (
    id INT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    parent_id INT NULL,
    headcount INT NOT NULL
);

INSERT INTO org_units (id, name, parent_id, headcount) VALUES
(1,  '본사',       NULL, 3),
(2,  '개발본부',   1,    2),
(3,  '영업본부',   1,    1),
(4,  '경영지원실', 1,    4),
(5,  '백엔드팀',   2,    6),
(6,  '프론트엔드팀', 2,  4),
(7,  '국내영업팀', 3,    5),
(8,  '해외영업팀', 3,    3),
(9,  '결제파트',   5,    3),
(10, '검색파트',   5,    2),
(11, '디자인시스템운영파트', 6, 2),
(12, '인사팀',     4,    2);
