DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS events;

CREATE TABLE members (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) NULL,
    grade VARCHAR(10) NOT NULL,
    point INT NOT NULL,
    birth_date DATE NOT NULL,
    joined_at DATETIME NOT NULL
);

INSERT INTO members (id, name, email, phone, nickname, grade, point, birth_date, joined_at) VALUES
(1,  '김철수',    'chulsoo.kim@naver.com',  '010-1234-5678', '  철수짱 ',  'gold',   7350, '1990-03-15', '2024-01-05 09:12:00'),
(2,  '이영희',    'younghee@gmail.com',     '010-2345-6789', NULL,         'silver', 2480, '1995-07-22', '2024-03-18 14:30:00'),
(3,  '박민',      'min.park@daum.net',      '010-3456-7890', '민이',       'bronze', 960,  '2001-11-02', '2024-03-02 20:05:00'),
(4,  '남궁민수',  'namgung@gmail.com',      '010-4567-8901', '',           'gold',   12040, '1985-01-30', '2024-06-11 11:00:00'),
(5,  'Tom',       'tom.lee@gmail.com',      '010-5678-9012', ' tommy',     'silver', 3000, '1998-05-09', '2024-09-27 08:45:00'),
(6,  '최지훈',    'jihoon@naver.com',       '010-6789-0123', '지훈 ',      'bronze', 450,  '2003-03-03', '2025-03-01 00:10:00'),
(7,  '정수연',    'sooyeon.j@kakao.com',    '010-7890-1234', NULL,         'vip',    25500, '1979-12-24', '2025-01-20 16:20:00'),
(8,  '황보',      'hwangbo@daum.net',       '010-8901-2345', '보보',       'silver', 5000, '1992-08-17', '2025-03-31 23:59:00'),
(9,  '제갈공명',  'zhuge@naver.com',        '010-9012-3456', '  ',         'bronze', 1999, '2000-02-29', '2025-05-05 10:00:00'),
(10, 'Alice',     'alice@gmail.com',        '010-1111-2222', 'ally',       'gold',   8800, '1996-10-01', '2025-08-15 13:13:00'),
(11, '윤하늘',    'sky.yoon@kakao.com',     '010-3333-4444', NULL,         'bronze', 0,    '2005-06-30', '2025-11-03 07:30:00'),
(12, '서지아',    'jia.seo@gmail.com',      '010-5555-6666', '지아',       'silver', 4999, '1988-03-08', '2025-12-24 19:00:00');

CREATE TABLE events (
    id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    round_no VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    fee INT NOT NULL,
    capacity INT NOT NULL,
    registered INT NOT NULL
);

INSERT INTO events (id, title, round_no, start_date, end_date, fee, capacity, registered) VALUES
(1,  'SQL 입문 스터디',       '1',  '2025-01-10', '2025-01-10', 10000, 20, 18),
(2,  '백엔드 밋업',           '2',  '2025-02-05', '2025-02-05', 25000, 50, 50),
(3,  '데이터 분석 부트캠프',  '10', '2025-03-03', '2025-03-14', 350000, 30, 22),
(4,  '온라인 세미나',         '3',  '2025-03-22', '2025-03-22', 0,     0,  137),
(5,  '클라우드 워크숍',       '11', '2025-04-19', '2025-04-20', 88000, 40, 41),
(6,  '알고리즘 캠프',         '9',  '2025-05-06', '2025-05-09', 120000, 25, 9),
(7,  '해커톤',                '20', '2025-06-14', '2025-06-15', 50000, 100, 76),
(8,  '면접 특강',             '4',  '2025-07-02', '2025-07-02', 15000, 60, 60),
(9,  '오픈소스 기여 행사',    '12', '2025-08-31', '2025-09-01', 0,     80, 35),
(10, '연말 네트워킹 파티',    '100','2025-12-19', '2025-12-19', 40000, 0,  64);
