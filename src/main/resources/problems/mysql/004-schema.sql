DROP TABLE IF EXISTS monthly_sales;

CREATE TABLE monthly_sales (
    year_month_str CHAR(7) PRIMARY KEY,
    revenue INT NOT NULL
);

INSERT INTO monthly_sales (year_month_str, revenue) VALUES
    ('2026-01', 1200000),
    ('2026-02', 1500000),
    ('2026-03', 1450000),
    ('2026-04', 1800000),
    ('2026-05', 2100000);
