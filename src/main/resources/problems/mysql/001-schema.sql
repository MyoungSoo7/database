DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL
);

INSERT INTO employees (id, name, department) VALUES
    (1, '김철수', '개발팀'),
    (2, '이영희', '기획팀'),
    (3, '박민수', '개발팀'),
    (4, '정수연', '기획팀'),
    (5, '최지훈', '디자인팀');
