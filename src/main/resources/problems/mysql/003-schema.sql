DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary INT NOT NULL
);

INSERT INTO employees (id, name, department, salary) VALUES
    (1, '김철수',   '개발팀',   5500),
    (2, '이영희',   '기획팀',   4800),
    (3, '박민수',   '개발팀',   6200),
    (4, '정수연',   '기획팀',   5100),
    (5, '최지훈',   '디자인팀', 4500),
    (6, '한지원',   '개발팀',   5800),
    (7, '강민호',   '디자인팀', 4700);
