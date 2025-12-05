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
                       is_public    BOOLEAN DEFAULT TRUE,
                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP,
                       deleted_at    TIMESTAMP
);

-- 1. 모든 정보가 입력된 표준 남성 유저 (공개 프로필)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'kim_chulsoo@gmail.com', 'google', 'google_123456789', '헬스보이철수', 'MALE', '1992-05-15',
             178.5, 76.0, 'ROLE_USER', 'ACTIVE', '3대 500 치는 그날까지! 💪',
             '땅콩,호두', NULL, TRUE, NOW(), NOW()
         );

-- 2. 질병 정보가 있는 여성 유저 (비공개 프로필)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'lee_younghee@gmail.com', 'google', 'google_987654321', '샐러드조아', 'FEMALE', '1995-10-20',
             162.0, 48.5, 'ROLE_USER', 'ACTIVE', '건강하게 다이어트 하기 🌱',
             '복숭아,우유', '당뇨', FALSE, NOW(), NOW()
         );

-- 3. 알레르기와 질병이 없는 건강한 유저 (소셜ID만 있는 상태, 프로필 작성 전 가정)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'new_user@gmail.com', 'google', 'google_99887766', '뉴비', NULL, NULL,
             NULL, NULL, 'ROLE_USER', 'ACTIVE', NULL,
             NULL, NULL, TRUE, NOW(), NOW()
         );

-- 4. 관리자 계정 (ROLE_ADMIN)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'admin@matchmeal.com', 'google', 'google_admin_001', '관리자', 'MALE', '1990-01-01',
             180.0, 80.0, 'ROLE_ADMIN', 'ACTIVE', '관리자 계정입니다.',
             NULL, NULL, FALSE, NOW(), NOW()
         );