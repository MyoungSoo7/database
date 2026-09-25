DROP TABLE IF EXISTS user_profile;

CREATE TABLE user_profile (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    info JSON NOT NULL
);

INSERT INTO user_profile (id, name, info) VALUES
    (1, '김철수', '{"age": 28, "city": "서울", "skills": ["Java", "MySQL", "Spring"]}'),
    (2, '이영희', '{"age": 32, "city": "부산", "skills": ["Python", "PostgreSQL"]}'),
    (3, '박민수', '{"age": 25, "city": "서울", "skills": ["JavaScript", "React", "Node.js"]}'),
    (4, '정수연', '{"age": 41, "city": "대구", "skills": ["Go", "Kubernetes"]}');
