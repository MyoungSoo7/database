DROP TABLE IF EXISTS loans;
DROP TABLE IF EXISTS readers;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS authors;

CREATE TABLE authors (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL
);

CREATE TABLE books (
    id INT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author_id INT NULL,            -- 작자 미상이면 NULL
    genre VARCHAR(50) NOT NULL,
    price INT NOT NULL,
    published_year INT NOT NULL
);

CREATE TABLE readers (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    joined_date DATE NOT NULL
);

CREATE TABLE loans (
    id INT PRIMARY KEY,
    book_id INT NOT NULL,
    reader_id INT NOT NULL,
    loan_date DATE NOT NULL,
    return_date DATE NULL          -- 아직 반납 안 했으면 NULL
);

INSERT INTO authors (id, name, country) VALUES
(1, '한강', '한국'),
(2, '김영하', '한국'),
(3, '무라카미 하루키', '일본'),
(4, '조지 오웰', '영국'),
(5, '정유정', '한국'),
(6, '히가시노 게이고', '일본'),
(7, '어니스트 헤밍웨이', '미국'),
(8, '박완서', '한국'),
(9, '알베르 카뮈', '프랑스');

INSERT INTO books (id, title, author_id, genre, price, published_year) VALUES
(1, '채식주의자', 1, '소설', 13500, 2007),
(2, '소년이 온다', 1, '소설', 15000, 2014),
(3, '살인자의 기억법', 2, '소설', 12000, 2013),
(4, '여행의 이유', 2, '에세이', 13000, 2019),
(5, '노르웨이의 숲', 3, '소설', 16500, 1987),
(6, '달리기를 말할 때 내가 하고 싶은 이야기', 3, '에세이', 14000, 2007),
(7, '1984', 4, '소설', 11000, 1949),
(8, '동물농장', 4, '소설', 9000, 1945),
(9, '종의 기원', 5, '소설', 14500, 2016),
(10, '용의자 X의 헌신', 6, '추리', 15500, 2005),
(11, '나미야 잡화점의 기적', 6, '소설', 16000, 2012),
(12, '백야행', 6, '추리', 18000, 1999),
(13, '노인과 바다', 7, '소설', 8500, 1952),
(14, '천자문', NULL, '고전', 7000, 1500),
(15, '옛 민요 모음', NULL, '시', 9500, 1900);

INSERT INTO readers (id, name, city, joined_date) VALUES
(1, '김민준', '서울', '2024-03-02'),
(2, '이서연', '부산', '2024-05-11'),
(3, '박지호', '서울', '2024-06-20'),
(4, '최수아', '대구', '2024-08-15'),
(5, '정도윤', '서울', '2024-09-01'),
(6, '강하은', '부산', '2024-10-07'),
(7, '윤시우', '인천', '2024-11-23'),
(8, '임지유', '서울', '2024-12-30');

INSERT INTO loans (id, book_id, reader_id, loan_date, return_date) VALUES
(1, 1, 1, '2025-01-05', '2025-01-19'),
(2, 2, 1, '2025-02-10', '2025-02-24'),
(3, 10, 1, '2025-03-15', NULL),
(4, 3, 2, '2025-01-08', '2025-01-22'),
(5, 1, 2, '2025-02-01', '2025-02-15'),
(6, 12, 2, '2025-02-20', '2025-03-06'),
(7, 5, 3, '2025-01-12', '2025-01-26'),
(8, 11, 3, '2025-03-02', NULL),
(9, 4, 4, '2025-01-20', '2025-02-03'),
(10, 7, 5, '2025-01-25', '2025-02-08'),
(11, 1, 5, '2025-02-14', '2025-02-28'),
(12, 2, 5, '2025-03-03', '2025-03-17'),
(13, 9, 5, '2025-03-20', NULL),
(14, 10, 6, '2025-02-05', '2025-02-19'),
(15, 12, 6, '2025-03-10', NULL),
(16, 5, 1, '2025-03-25', NULL),
(17, 2, 3, '2025-03-28', NULL),
(18, 11, 4, '2025-02-27', '2025-03-13');
