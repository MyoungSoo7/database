-- DDL·제약조건(ddl) 문제 공유 스키마. 제출할 때마다 통째로 다시 적재된다.
-- 문제에서 사용자가 만드는 뷰·테이블(외래키로 아래 테이블을 참조할 수 있다)을 먼저 지운다: 자식 → 부모 순서.
DROP VIEW IF EXISTS v_public_posts;
DROP TABLE IF EXISTS post_tags;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS club_members;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS boards;
DROP TABLE IF EXISTS clubs;

-- 외래키는 걸지 않는다 (외래키는 문제에서 사용자가 만드는 것만).
CREATE TABLE clubs (
    id INT PRIMARY KEY,
    club_nm VARCHAR(20) NOT NULL,
    max_members INT NOT NULL,
    created_on DATE NOT NULL
);

INSERT INTO clubs (id, club_nm, max_members, created_on) VALUES
(1, '독서 모임', 20, '2024-03-02'),
(2, '등산 동호회', 50, '2024-04-13'),
(3, '보드게임', 12, '2024-07-01'),
(4, '사진 클럽', 30, '2024-09-21'),
(5, '러닝 크루', 100, '2025-01-05');

CREATE TABLE boards (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

INSERT INTO boards (id, name) VALUES
(1, '공지사항'),
(2, '자유게시판'),
(3, '질문답변');

CREATE TABLE posts (
    id INT PRIMARY KEY,
    board_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    is_public TINYINT NOT NULL,
    created_at DATETIME NOT NULL
);

INSERT INTO posts (id, board_id, title, is_public, created_at) VALUES
(1, 1, '커뮤니티 이용 규칙', 1, '2025-01-02 09:00:00'),
(2, 2, '주말 등산 후기', 1, '2025-01-05 20:13:00'),
(3, 2, '사진 보정 팁 공유', 1, '2025-01-07 11:40:00'),
(4, 3, 'DB 인덱스 질문', 0, '2025-01-08 15:22:00'),
(5, 2, '보드게임 추천', 1, '2025-01-10 18:05:00'),
(6, 3, '가입 승인은 언제 되나요', 0, '2025-01-11 08:30:00'),
(7, 1, '서버 점검 공지', 1, '2025-01-15 10:00:00'),
(8, 2, '임시 저장 글', 0, '2025-01-16 23:59:00');

CREATE TABLE tags (
    id INT PRIMARY KEY,
    name VARCHAR(30) NOT NULL
);

INSERT INTO tags (id, name) VALUES
(1, '후기'),
(2, '질문'),
(3, '공지'),
(4, '사진'),
(5, '추천');
