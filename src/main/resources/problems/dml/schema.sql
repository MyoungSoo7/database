-- 데이터 변경(dml) 문제 공유 스키마. 제출할 때마다 통째로 다시 적재된다.
DROP TABLE IF EXISTS stock_moves;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS members_dml;
DROP TABLE IF EXISTS item_backup;
DROP TABLE IF EXISTS items;

CREATE TABLE items (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(10) NOT NULL
);

INSERT INTO items (id, name, category, price, stock, status) VALUES
(1, '볼펜', '문구', 1500, 120, 'ON_SALE'),
(2, '노트', '문구', 3000, 80, 'ON_SALE'),
(3, '형광펜', '문구', 1200, 0, 'SOLD_OUT'),
(4, '생수 2L', '식품', 1100, 200, 'ON_SALE'),
(5, '컵라면', '식품', 1300, 150, 'ON_SALE'),
(6, '초코바', '식품', 1700, 60, 'ON_SALE'),
(7, '보조배터리', '전자', 32000, 15, 'ON_SALE'),
(8, 'USB 케이블', '전자', 8900, 40, 'ON_SALE'),
(9, '이어폰', '전자', 45000, 0, 'SOLD_OUT'),
(10, '우산', '생활', 9800, 25, 'ON_SALE');

-- items 와 같은 구조의 빈 백업 테이블
CREATE TABLE item_backup (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    status VARCHAR(10) NOT NULL
);

CREATE TABLE members_dml (
    id INT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    grade VARCHAR(10) NOT NULL,
    point INT NOT NULL,
    last_login DATE NOT NULL,
    status VARCHAR(10) NOT NULL
);

INSERT INTO members_dml (id, name, grade, point, last_login, status) VALUES
(1, '김민준', 'SILVER', 7200, '2025-03-02', 'ACTIVE'),
(2, '이서연', 'GOLD', 3400, '2024-11-20', 'ACTIVE'),
(3, '박도윤', 'SILVER', 800, '2025-02-14', 'ACTIVE'),
(4, '최지우', 'GOLD', 12000, '2024-06-30', 'ACTIVE'),
(5, '정하준', 'BRONZE', 0, '2024-12-31', 'ACTIVE'),
(6, '강서윤', 'BRONZE', 1000, '2025-01-01', 'ACTIVE'),
(7, '조예준', 'BRONZE', 5000, '2025-01-15', 'WITHDRAWN'),
(8, '윤지아', 'SILVER', 450, '2025-02-20', 'WITHDRAWN');

CREATE TABLE coupons (
    code VARCHAR(20) PRIMARY KEY,
    member_id INT NOT NULL,
    discount INT NOT NULL,
    used TINYINT NOT NULL,
    expires_at DATE NOT NULL
);

INSERT INTO coupons (code, member_id, discount, used, expires_at) VALUES
('WELCOME-01', 1, 3000, 0, '2025-12-31'),
('WELCOME-02', 2, 3000, 1, '2025-12-31'),
('SPRING-10', 3, 5000, 0, '2025-05-31'),
('SPRING-11', 7, 5000, 0, '2025-05-31'),
('VIP-100', 4, 10000, 0, '2025-12-31'),
('VIP-101', 7, 10000, 1, '2025-12-31'),
('BDAY-08', 8, 2000, 0, '2025-08-31'),
('BDAY-06', 6, 2000, 0, '2025-06-30'),
('EVENT-99', 99, 1000, 0, '2025-03-31');

CREATE TABLE notices (
    id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(20) NOT NULL,
    created_at DATE NOT NULL,
    is_pinned TINYINT NOT NULL
);

INSERT INTO notices (id, title, author, created_at, is_pinned) VALUES
(1, '서비스 오픈 안내', '운영팀', '2024-03-01', 1),
(2, '설 연휴 배송 안내', '운영팀', '2024-02-05', 0),
(3, '개인정보처리방침 개정', '법무팀', '2024-08-20', 1),
(4, '추석 연휴 배송 안내', '운영팀', '2024-09-10', 0),
(5, '서버 점검 안내', '개발팀', '2024-12-30', 0),
(6, '신규 결제수단 추가', '개발팀', '2025-01-02', 0),
(7, '봄맞이 이벤트', '마케팅팀', '2025-03-15', 0),
(8, '이용약관 개정', '법무팀', '2025-04-01', 1);

-- 재고 이동 기록. qty 가 양수면 입고, 음수면 출고.
CREATE TABLE stock_moves (
    id INT PRIMARY KEY,
    item_id INT NOT NULL,
    qty INT NOT NULL,
    moved_at DATE NOT NULL
);

INSERT INTO stock_moves (id, item_id, qty, moved_at) VALUES
(1, 1, 50, '2025-05-01'),
(2, 1, -20, '2025-05-03'),
(3, 4, 100, '2025-05-02'),
(4, 5, -30, '2025-05-04'),
(5, 7, 10, '2025-05-05'),
(6, 9, 20, '2025-05-06'),
(7, 4, -40, '2025-05-07');
