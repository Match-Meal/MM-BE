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

------------- 식단 테스트 스키마 -----------------------------
-- ==========================================
-- 1. 식단 기록 (부모 테이블)
-- ==========================================
CREATE TABLE  IF NOT EXISTS diet_records (
    diet_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '식단 ID',
    user_id            BIGINT NOT NULL COMMENT '사용자 ID',

    -- 언제, 어떤 끼니를 먹었는지
    eat_date           DATE NOT NULL COMMENT '식사 날짜 (YYYY-MM-DD)',
    eat_time           TIME COMMENT '식사 시간 (HH:mm)',
    meal_type          VARCHAR(20) NOT NULL COMMENT '식사 타입 (BREAKFAST, LUNCH, DINNER, SNACK)',
    memo               TEXT COMMENT '식사 메모',

    -- [핵심] 통계 성능을 위한 합계 컬럼 (반정규화)
    total_calories     DOUBLE DEFAULT 0 COMMENT '총 칼로리 합계',
    total_carbohydrate DOUBLE DEFAULT 0 COMMENT '총 탄수화물 합계',
    total_protein      DOUBLE DEFAULT 0 COMMENT '총 단백질 합계',
    total_fat          DOUBLE DEFAULT 0 COMMENT '총 지방 합계',

    -- 메타 데이터
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    -- [성능 최적화] "특정 유저의 특정 날짜 식단"을 빨리 찾기 위한 복합 인덱스
    INDEX idx_diet_user_date (user_id, eat_date)
);

-- ==========================================
-- 2. 식단 상세 (자식 테이블) - 음식 목록
-- ==========================================
CREATE TABLE IF NOT EXISTS diet_details (
    diet_detail_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    diet_id            BIGINT NOT NULL,

    --
    food_id            BIGINT NOT NULL COMMENT '음식 PK (FK)',
    food_name          VARCHAR(255) NOT NULL, -- 스냅샷은 유지 (이름 변경 대비)

    quantity           DOUBLE NOT NULL,
    unit               VARCHAR(20) DEFAULT 'g',

    -- 영양성분 스냅샷
    calories           DOUBLE DEFAULT 0,
    carbohydrate       DOUBLE DEFAULT 0,
    protein            DOUBLE DEFAULT 0,
    fat                DOUBLE DEFAULT 0,

    -- 외래키 제약조건 (식단 삭제 시 상세도 삭제)
    CONSTRAINT fk_diet_details_diet
      FOREIGN KEY (diet_id) REFERENCES diet_records (diet_id)
          ON DELETE CASCADE,

    -- [추가] 음식 테이블과도 외래키 연결 (선택사항이지만 데이터 무결성 위해 추천)
    -- 주의: 만약 음식 DB를 물리적으로 삭제할 일이 있다면 이 제약조건이 방해가 될 수 있음.
    -- 서비스 정책상 음식 데이터 삭제가 없다면 거는 게 좋음.
    CONSTRAINT fk_diet_details_food
      FOREIGN KEY (food_id) REFERENCES foods (food_id)
);