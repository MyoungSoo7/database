DROP TABLE IF EXISTS purchase_items;
DROP TABLE IF EXISTS purchases;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS goods;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS grade_levels;

-- ===== 쇼핑: 고객 - 주문 - 주문상품 - 상품 =====
CREATE TABLE customers (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    city VARCHAR(20) NOT NULL
);

INSERT INTO customers (id, name, city) VALUES
(1, '김민준', '서울'),
(2, '이서연', '부산'),
(3, '박도윤', '서울'),
(4, '최지우', '대전'),
(5, '정하준', '부산'),
(6, '강서아', '인천');

-- customer_id 는 customers.id 를 가리킨다 (FK 제약은 두지 않음)
CREATE TABLE purchases (
    id INT PRIMARY KEY,
    customer_id INT NOT NULL,
    purchase_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL   -- 'PAID' 또는 'CANCELLED'
);

INSERT INTO purchases (id, customer_id, purchase_date, status) VALUES
(101, 1, '2024-12-10', 'PAID'),
(102, 2, '2024-12-15', 'PAID'),
(103, 1, '2025-01-05', 'PAID'),
(104, 3, '2025-01-12', 'CANCELLED'),
(105, 2, '2025-01-20', 'PAID'),
(106, 1, '2025-02-03', 'PAID'),
(107, 5, '2024-11-28', 'PAID'),
(108, 3, '2025-02-14', 'PAID');

CREATE TABLE goods (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL,
    price INT NOT NULL
);

INSERT INTO goods (id, name, category, price) VALUES
(1, '볼펜',     '문구', 1500),
(2, '노트',     '문구', 3000),
(3, '텀블러',   '생활', 15000),
(4, '우산',     '생활', 12000),
(5, '이어폰',   '전자', 45000),
(6, '보조배터리', '전자', 30000);

-- 주문 한 건에 여러 상품 (purchase_id → purchases.id, goods_id → goods.id)
CREATE TABLE purchase_items (
    purchase_id INT NOT NULL,
    goods_id INT NOT NULL,
    qty INT NOT NULL,
    PRIMARY KEY (purchase_id, goods_id)
);

INSERT INTO purchase_items (purchase_id, goods_id, qty) VALUES
(101, 1, 10), (101, 3, 1),
(102, 5, 1),
(103, 2, 5), (103, 1, 2),
(104, 6, 1),
(105, 3, 2), (105, 4, 1),
(106, 5, 1), (106, 2, 3),
(107, 4, 2),
(108, 1, 4), (108, 3, 1);

-- ===== 회사: 부서 - 직원 - 교육과정 =====
CREATE TABLE departments (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(30) NOT NULL,
    location VARCHAR(20) NOT NULL
);

INSERT INTO departments (dept_id, dept_name, location) VALUES
(10, '개발팀', '서울'),
(20, '영업팀', '부산'),
(30, '인사팀', '서울'),
(40, '연구팀', '대전');

-- dept_id → departments.dept_id (NULL = 부서 미배정), manager_id → staff.id (NULL = 상사 없음)
CREATE TABLE staff (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    dept_id INT NULL,
    manager_id INT NULL,
    salary INT NOT NULL
);

INSERT INTO staff (id, name, dept_id, manager_id, salary) VALUES
(1, '한대표', 10, NULL, 9000),
(2, '오개발', 10, 1,    6500),
(3, '신코딩', 10, 2,    4800),
(4, '장영업', 20, 1,    6000),
(5, '문판매', 20, 4,    6200),
(6, '배인사', 30, 1,    5200),
(7, '조신입', 10, 2,    3200),
(8, '유수습', NULL, 6,  2800);

CREATE TABLE courses (
    id INT PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    hours INT NOT NULL
);

INSERT INTO courses (id, title, hours) VALUES
(1, 'SQL 기초',     8),
(2, '협상 기술',     6),
(3, '리더십',       12),
(4, '보안 교육',     4);

-- 직원-과정 다대다 중간 테이블
CREATE TABLE enrollments (
    staff_id INT NOT NULL,
    course_id INT NOT NULL,
    PRIMARY KEY (staff_id, course_id)
);

INSERT INTO enrollments (staff_id, course_id) VALUES
(2, 1), (3, 1), (7, 1),
(4, 2), (5, 2),
(2, 3), (4, 3), (6, 3),
(3, 4) ;

-- 급여 등급 구간 (min_salary 이상 max_salary 이하)
CREATE TABLE grade_levels (
    level_name VARCHAR(10) PRIMARY KEY,
    min_salary INT NOT NULL,
    max_salary INT NOT NULL
);

INSERT INTO grade_levels (level_name, min_salary, max_salary) VALUES
('G1', 0,    3999),
('G2', 4000, 5999),
('G3', 6000, 7999),
('G4', 8000, 99999);
