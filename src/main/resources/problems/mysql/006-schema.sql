DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NULL,
    bonus INT NULL
);

INSERT INTO employees (id, name, department, bonus) VALUES
    (1, '김철수', '개발팀',   500),
    (2, '이영희', '기획팀',   NULL),
    (3, '박민수', NULL,       800),
    (4, '정수연', '기획팀',   NULL),
    (5, '최지훈', NULL,       NULL);
