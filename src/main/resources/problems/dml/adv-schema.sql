-- 데이터 변경(dml) 고급 문제 스키마. 제출할 때마다 통째로 다시 적재된다.
DROP TABLE IF EXISTS newsletter_subs;
DROP TABLE IF EXISTS sales_reps;

-- 뉴스레터 구독. 같은 이메일이 여러 번 들어간 중복이 있다 (id 가 작을수록 먼저 가입).
CREATE TABLE newsletter_subs (
    id INT PRIMARY KEY,
    email VARCHAR(50) NOT NULL,
    name VARCHAR(20) NOT NULL,
    subscribed_on DATE NOT NULL
);

INSERT INTO newsletter_subs (id, email, name, subscribed_on) VALUES
(1, 'minjun@mail.com', '김민준', '2025-01-03'),
(2, 'seoyeon@mail.com', '이서연', '2025-01-04'),
(3, 'minjun@mail.com', '김민준', '2025-01-10'),
(4, 'doyun@mail.com', '박도윤', '2025-01-12'),
(5, 'seoyeon@mail.com', '이서연(회사)', '2025-01-15'),
(6, 'jiwoo@mail.com', '최지우', '2025-01-20'),
(7, 'minjun@mail.com', '김민준', '2025-02-01'),
(8, 'hajun@mail.com', '정하준', '2025-02-03'),
(9, 'doyun@mail.com', '박도윤', '2025-02-10'),
(10, 'seoyun@mail.com', '강서윤', '2025-02-14'),
(11, 'jiwoo@mail.com', '최지우', '2025-02-20'),
(12, 'yejun@mail.com', '조예준', '2025-03-01');

-- 영업 사원 실적. bonus 는 지난 분기 값이라 새로 매겨야 한다.
CREATE TABLE sales_reps (
    id INT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    region VARCHAR(10) NOT NULL,
    sales_amt INT NOT NULL,
    bonus INT NOT NULL
);

INSERT INTO sales_reps (id, name, region, sales_amt, bonus) VALUES
(1, '김영업', '서울', 9200, 50000),
(2, '이실적', '서울', 7800, 0),
(3, '박성과', '서울', 7800, 0),
(4, '최열정', '서울', 6100, 100000),
(5, '정신입', '서울', 3000, 0),
(6, '강부산', '부산', 8800, 0),
(7, '조해운', '부산', 8800, 0),
(8, '윤광안', '부산', 5000, 0),
(9, '장수영', '부산', 4200, 30000),
(10, '한대구', '대구', 6600, 0),
(11, '오달서', '대구', 5900, 0),
(12, '서동성', '대구', 4100, 20000),
(13, '신광주', '광주', 3500, 0);
