DROP TABLE IF EXISTS daily_visits;
DROP TABLE IF EXISTS monthly_revenue;
DROP TABLE IF EXISTS players;

CREATE TABLE daily_visits (
    visit_date DATE PRIMARY KEY,
    visitors INT NOT NULL
);

CREATE TABLE monthly_revenue (
    id INT PRIMARY KEY,
    month CHAR(7) NOT NULL,        -- 'YYYY-MM'
    region VARCHAR(50) NOT NULL,
    revenue INT NOT NULL,
    UNIQUE KEY uk_month_region (month, region)
);

CREATE TABLE players (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    team VARCHAR(50) NOT NULL,
    score INT NOT NULL             -- 점수는 서로 겹치지 않는다
);

INSERT INTO daily_visits (visit_date, visitors) VALUES
('2025-03-01', 120),
('2025-03-02', 150),
('2025-03-03', 90),
('2025-03-04', 200),
('2025-03-05', 170),
('2025-03-06', 130),
('2025-03-07', 160),
('2025-03-08', 110),
('2025-03-09', 180),
('2025-03-10', 140);

INSERT INTO monthly_revenue (id, month, region, revenue) VALUES
(1, '2025-01', '서울', 500),
(2, '2025-01', '부산', 300),
(3, '2025-01', '대구', 200),
(4, '2025-02', '서울', 450),
(5, '2025-02', '부산', 350),
(6, '2025-02', '대구', 200),
(7, '2025-03', '서울', 600),
(8, '2025-03', '부산', 250),
(9, '2025-03', '대구', 150),
(10, '2025-04', '서울', 550),
(11, '2025-04', '부산', 400),
(12, '2025-04', '대구', 250);

INSERT INTO players (id, name, team, score) VALUES
(1, '김하늘', '레드', 920),
(2, '이바다', '레드', 780),
(3, '박산', '레드', 850),
(4, '최별', '레드', 610),
(5, '정구름', '블루', 990),
(6, '강노을', '블루', 700),
(7, '조비', '블루', 830),
(8, '윤달', '블루', 560),
(9, '장바람', '그린', 880),
(10, '임솔', '그린', 740),
(11, '한결', '그린', 650),
(12, '오름', '그린', 900);
