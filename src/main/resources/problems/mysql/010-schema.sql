DROP TABLE IF EXISTS students;

CREATE TABLE students (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    class_name VARCHAR(20) NOT NULL,
    score INT NOT NULL
);

INSERT INTO students (id, name, class_name, score) VALUES
    (1, '김철수', 'A', 95),
    (2, '이영희', 'A', 95),
    (3, '박민수', 'A', 88),
    (4, '정수연', 'A', 82),
    (5, '최지훈', 'B', 91),
    (6, '한지원', 'B', 91),
    (7, '강민호', 'B', 85),
    (8, '오세나', 'B', 70);
