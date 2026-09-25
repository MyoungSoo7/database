DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS products;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    position VARCHAR(20) NOT NULL,
    salary INT NOT NULL,
    commission INT NULL,
    hire_date DATE NOT NULL
);

INSERT INTO employees (id, name, department, position, salary, commission, hire_date) VALUES
(1,  '김철수', '개발팀', '과장', 5200, NULL, '2019-03-02'),
(2,  '이영희', '기획팀', '대리', 4300, NULL, '2021-07-15'),
(3,  '박민수', '개발팀', '사원', 3600, NULL, '2024-01-08'),
(4,  '정수진', '인사팀', '부장', 6800, NULL, '2012-11-20'),
(5,  '최동현', '영업팀', '대리', 4100, 300,  '2020-05-11'),
(6,  '강하늘', '영업팀', '사원', 3400, 100,  '2024-09-02'),
(7,  '윤서연', '개발팀', '대리', 4600, NULL, '2021-02-01'),
(8,  '김도윤', '기획팀', '사원', 3500, NULL, '2025-02-03'),
(9,  '한지원', '영업팀', '과장', 5000, 500,  '2017-08-21'),
(10, '오민재', '디자인팀', '대리', 4200, NULL, '2022-04-18'),
(11, '김하은', '디자인팀', '사원', 3300, NULL, '2025-07-07'),
(12, '서준호', '영업팀', '부장', 6500, 0,    '2010-01-04'),
(13, '임지아', '개발팀', '과장', 5500, 400,  '2018-06-25'),
(14, '장우진', '인사팀', '사원', 3200, NULL, '2024-12-02'),
(15, '신유나', '기획팀', '과장', 5200, NULL, '2016-10-10');

CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    discount_rate INT NOT NULL
);

INSERT INTO products (id, name, category, price, stock, discount_rate) VALUES
(1,  '무선 키보드',       '전자기기', 45000,  30, 10),
(2,  '게이밍 마우스',     '전자기기', 32000,  0,  0),
(3,  'USB-C 케이블',      '전자기기', 9000,   120, 20),
(4,  '27인치 모니터',     '전자기기', 289000, 8,  15),
(5,  '노트북 거치대',     '사무용품', 25000,  40, 0),
(6,  '볼펜 10자루',       '사무용품', 5000,   200, 10),
(7,  'A4 복사용지',       '사무용품', 21000,  60, 5),
(8,  '스테인리스 텀블러', '생활용품', 18000,  25, 30),
(9,  '무선 이어폰',       '전자기기', 129000, 15, 20),
(10, '데스크 매트',       '사무용품', 15000,  0,  0),
(11, '가습기',            '생활용품', 39000,  12, 25),
(12, '무선 충전기',       '전자기기', 29000,  22, 10),
(13, '머그컵',            '생활용품', 8000,   80, 0),
(14, '독서대',            '사무용품', 32000,  10, 15),
(15, '전기 포트',         '생활용품', 32000,  18, 20);
