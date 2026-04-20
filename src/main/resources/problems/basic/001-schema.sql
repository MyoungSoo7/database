DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary INT NOT NULL
);

INSERT INTO employees (id, name, department, salary) VALUES
(1, '김철수', '개발팀', 5000),
(2, '이영희', '기획팀', 4500),
(3, '박민수', '개발팀', 5500);
