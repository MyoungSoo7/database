DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS students;

CREATE TABLE sales (
    id INT PRIMARY KEY,
    region VARCHAR(20) NOT NULL,
    product VARCHAR(50) NOT NULL,
    sale_date DATE NOT NULL,
    qty INT NOT NULL,
    amount INT NOT NULL,
    discount INT NULL
);

INSERT INTO sales (id, region, product, sale_date, qty, amount, discount) VALUES
(1,  '서울', '노트북',   '2024-11-03', 2, 2400000, 100000),
(2,  '서울', '마우스',   '2024-11-15', 10, 250000, NULL),
(3,  '부산', '노트북',   '2024-11-20', 1, 1200000, 50000),
(4,  '대구', '키보드',   '2024-12-01', 3, 270000, NULL),
(5,  '서울', '모니터',   '2024-12-05', 2, 600000, 30000),
(6,  '부산', '마우스',   '2024-12-12', 4, 100000, NULL),
(7,  '서울', '키보드',   '2024-12-20', 5, 450000, 20000),
(8,  '대구', '노트북',   '2024-12-28', 1, 1250000, NULL),
(9,  '부산', '모니터',   '2025-01-04', 3, 870000, 40000),
(10, '서울', '노트북',   '2025-01-09', 1, 1300000, NULL),
(11, '대구', '마우스',   '2025-01-15', 6, 150000, 10000),
(12, '서울', '마우스',   '2025-01-22', 8, 200000, NULL),
(13, '부산', '키보드',   '2025-01-30', 2, 180000, NULL),
(14, '광주', '모니터',   '2025-02-02', 1, 300000, NULL),
(15, '서울', '모니터',   '2025-02-10', 4, 1160000, 60000),
(16, '대구', '키보드',   '2025-02-14', 2, 190000, NULL),
(17, '부산', '노트북',   '2025-02-18', 2, 2500000, 150000),
(18, '서울', '키보드',   '2025-02-25', 3, 285000, NULL),
(19, '광주', '마우스',   '2025-02-27', 2, 50000, NULL),
(20, '서울', '노트북',   '2025-02-28', 1, 1350000, 50000);

CREATE TABLE students (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    grade INT NOT NULL,
    class_name VARCHAR(10) NOT NULL,
    email VARCHAR(100) NULL
);

INSERT INTO students (id, name, grade, class_name, email) VALUES
(1,  '김하늘', 1, 'A', 'sky@school.kr'),
(2,  '이바다', 1, 'A', NULL),
(3,  '박산',   1, 'B', 'mountain@school.kr'),
(4,  '최강',   1, 'B', 'river@school.kr'),
(5,  '정별',   2, 'A', NULL),
(6,  '한솔',   2, 'A', 'sol@school.kr'),
(7,  '윤달',   2, 'B', NULL),
(8,  '오빛',   2, 'C', 'light@school.kr'),
(9,  '서숲',   2, 'C', 'forest@school.kr'),
(10, '임들',   1, 'C', NULL);

CREATE TABLE scores (
    id INT PRIMARY KEY,
    student_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    score INT NULL
);

-- score 가 NULL 이면 결시(시험을 보지 않음)
INSERT INTO scores (id, student_id, subject, score) VALUES
(1,  1, '국어', 90), (2,  1, '수학', 85), (3,  1, '영어', 78),
(4,  2, '국어', 72), (5,  2, '수학', NULL), (6,  2, '영어', 88),
(7,  3, '국어', 65), (8,  3, '수학', 95), (9,  3, '영어', NULL),
(10, 4, '국어', 80), (11, 4, '수학', 70), (12, 4, '영어', 92),
(13, 5, '국어', NULL), (14, 5, '수학', 60), (15, 5, '영어', 75),
(16, 6, '국어', 88), (17, 6, '수학', 91), (18, 6, '영어', 84);
