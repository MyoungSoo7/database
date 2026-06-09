DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INT NULL,
    FOREIGN KEY (parent_id) REFERENCES departments(id)
);

INSERT INTO departments (id, name, parent_id) VALUES
    (1, '전사',         NULL),
    (2, '기술본부',     1),
    (3, '경영본부',     1),
    (4, '백엔드팀',     2),
    (5, '프론트엔드팀', 2),
    (6, '인사팀',       3),
    (7, 'API 그룹',     4),
    (8, '플랫폼 그룹',  4);
