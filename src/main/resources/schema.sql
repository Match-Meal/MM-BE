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
                       serving_size DOUBLE PRECISION, -- DOUBLE -> DOUBLE PRECISION (표준)
                       unit         VARCHAR(10) DEFAULT 'g',
                       calories     DOUBLE PRECISION,
                       protein      DOUBLE PRECISION,
                       fat          DOUBLE PRECISION,
                       carbohydrate DOUBLE PRECISION,

    -- [핵심 수정] 복잡한 날짜 설정 제거 -> 그냥 TIMESTAMP
                       created_at   TIMESTAMP,
                       updated_at   TIMESTAMP
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
                       height_cm     DOUBLE PRECISION,
                       weight_kg     DOUBLE PRECISION,
                       role          VARCHAR(20),
                       status        VARCHAR(20),
                       status_message VARCHAR(255),
                       profile_image VARCHAR(1000),
                       allergies     TEXT,
                       diseases      TEXT,
                       is_public    BOOLEAN DEFAULT TRUE,
                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP,
                       deleted_at    TIMESTAMP
);


------------- 식단 테스트 스키마 -----------------------------
DROP TABLE IF EXISTS diet_details;
DROP TABLE IF EXISTS diet_records;

CREATE TABLE diet_records (
                              diet_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id            BIGINT NOT NULL,

                              eat_date           DATE NOT NULL,
                              eat_time           TIME,
                              meal_type          VARCHAR(20) NOT NULL,
                              memo               TEXT,
                              diet_img_url       VARCHAR(500),

                              total_calories     DOUBLE PRECISION DEFAULT 0,
                              total_carbohydrate DOUBLE PRECISION DEFAULT 0,
                              total_protein      DOUBLE PRECISION DEFAULT 0,
                              total_fat          DOUBLE PRECISION DEFAULT 0,

                              created_at         TIMESTAMP,
                              updated_at         TIMESTAMP
);

CREATE INDEX idx_diet_user_date ON diet_records(user_id, eat_date);

CREATE TABLE diet_details (
                              diet_detail_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                              diet_id            BIGINT NOT NULL,

                              food_id            BIGINT,
                              food_name          VARCHAR(255) NOT NULL,

                              quantity           DOUBLE PRECISION NOT NULL,
                              unit               VARCHAR(20) DEFAULT 'g',

                              calories           DOUBLE PRECISION DEFAULT 0,
                              carbohydrate       DOUBLE PRECISION DEFAULT 0,
                              protein            DOUBLE PRECISION DEFAULT 0,
                              fat                DOUBLE PRECISION DEFAULT 0,

                              CONSTRAINT fk_diet_details_diet
                                  FOREIGN KEY (diet_id) REFERENCES diet_records (diet_id)
                                      ON DELETE CASCADE
);

-- 1. 모든 정보가 입력된 표준 남성 유저 (공개 프로필)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message, profile_image,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'kim_chulsoo@gmail.com', 'google', 'google_123456789', '헬스보이철수', 'MALE', '1992-05-15',
             178.5, 76.0, 'ROLE_USER', 'ACTIVE', '3대 500 치는 그날까지! 💪', NULL,
             '땅콩,호두', NULL, TRUE, NOW(), NOW()
         );

-- 2. 질병 정보가 있는 여성 유저 (비공개 프로필)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message, profile_image,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'lee_younghee@gmail.com', 'google', 'google_987654321', '샐러드조아', 'FEMALE', '1995-10-20',
             162.0, 48.5, 'ROLE_USER', 'ACTIVE', '건강하게 다이어트 하기 🌱', NULL,
             '복숭아,우유', '당뇨', FALSE, NOW(), NOW()
         );

-- 3. 알레르기와 질병이 없는 건강한 유저 (소셜ID만 있는 상태, 프로필 작성 전 가정)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message, profile_image,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'new_user@gmail.com', 'google', 'google_99887766', '뉴비', NULL, NULL,
             NULL, NULL, 'ROLE_USER', 'ACTIVE', NULL, NULL,
             NULL, NULL, TRUE, NOW(), NOW()
         );

-- 4. 관리자 계정 (ROLE_ADMIN)
INSERT INTO users (
    email, platform, social_id, user_name, gender, birth_date,
    height_cm, weight_kg, role, status, status_message, profile_image,
    allergies, diseases, is_public, created_at, updated_at
) VALUES (
             'admin@matchmeal.com', 'google', 'google_admin_001', '관리자', 'MALE', '1990-01-01',
             180.0, 80.0, 'ROLE_ADMIN', 'ACTIVE', '관리자 계정입니다.', NULL,
             NULL, NULL, FALSE, NOW(), NOW()
         );