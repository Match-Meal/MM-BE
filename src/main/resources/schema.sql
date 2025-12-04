-- H2 DB 초기화용 스키마 파일입니다.
-- 현재는 테이블 생성 쿼리가 없어서 주석만 남겨둡니다.

------------- 음식 DB 테스트 스키마 -----------------------------
DROP TABLE IF EXISTS foods;

CREATE TABLE foods (
                       food_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id      BIGINT,
                       food_code    VARCHAR(50) NOT NULL,
                       food_name    VARCHAR(255) NOT NULL,
                       category     VARCHAR(100),
                       serving_size DOUBLE,
                       unit         VARCHAR(10) DEFAULT 'g',
                       calories     DOUBLE,
                       protein      DOUBLE,
                       fat          DOUBLE,
                       carbohydrate DOUBLE,
                       created_at   TIMESTAMP DEFAULT NOW(),
                       updated_at   TIMESTAMP DEFAULT NOW() -- ON UPDATE CURRENT_TIMESTAMP 삭제
);

-- 인덱스는 테이블 생성 후 따로 만드는 것이 H2에서 가장 안전합니다.
CREATE UNIQUE INDEX idx_food_code ON foods(food_code);

DROP TABLE IF EXISTS users;

CREATE TABLE users (
                       user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email         VARCHAR(100) NOT NULL,
                       platform      VARCHAR(50),
                       social_id     VARCHAR(255),
                       user_name     VARCHAR(50),
                       gender        VARCHAR(10),
                       birth_date    DATE,
                       height_cm     DOUBLE,
                       weight_kg     DOUBLE,
                       role          VARCHAR(20),
                       status        VARCHAR(20),
                       status_message VARCHAR(255),
                       allergies     TEXT,
                       diseases      TEXT,
                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP,
                       deleted_at    TIMESTAMP
);