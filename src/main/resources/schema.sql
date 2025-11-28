-- H2 DB 초기화용 스키마 파일입니다.
-- 현재는 테이블 생성 쿼리가 없어서 주석만 남겨둡니다.

------------- 음식 DB 테스트 스키마 -----------------------------
CREATE TABLE IF NOT EXISTS foods (
       food_id      BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 내부 관리용 PK (고속)
       food_code    VARCHAR(50) NOT NULL,               -- 비즈니스 식별자
       food_name    VARCHAR(255) NOT NULL,
       category     VARCHAR(100),
       serving_size DOUBLE,
       unit         VARCHAR(10) DEFAULT 'g',
       calories     DOUBLE,
       protein      DOUBLE,
       fat          DOUBLE,
       carbohydrate DOUBLE,
       created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
       updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

       UNIQUE INDEX idx_food_code (food_code)  -- ★ 핵심: 코드로 조회할 때 빠르게!
);