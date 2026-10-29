DROP TABLE IF EXISTS parcel_bands;
DROP TABLE IF EXISTS parcels;

-- 무게 구간표: min_weight 이상 max_weight 미만 (단위 g). 구간끼리 겹치지 않는다.
CREATE TABLE parcel_bands (
    band_name VARCHAR(10) PRIMARY KEY,
    min_weight INT NOT NULL,
    max_weight INT NOT NULL,
    fee INT NOT NULL
);

INSERT INTO parcel_bands (band_name, min_weight, max_weight, fee) VALUES
('초소형', 0,     500,   2500),
('소형',   500,   2000,  3500),
('중형',   2000,  5000,  5000),
('대형',   5000,  10000, 7000),
('특대형', 10000, 20000, 10000);

CREATE TABLE parcels (
    id INT PRIMARY KEY,
    receiver VARCHAR(20) NOT NULL,
    weight INT NOT NULL
);

INSERT INTO parcels (id, receiver, weight) VALUES
(1,  '김민준', 300),
(2,  '이서연', 500),
(3,  '박도윤', 1999),
(4,  '최지우', 2000),
(5,  '정하준', 4800),
(6,  '강서아', 800),
(7,  '윤시우', 20000),
(8,  '임지유', 25500),
(9,  '한결',   120),
(10, '오름',   3100);
