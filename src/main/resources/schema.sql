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
                       updated_at   TIMESTAMP,
                       deleted_at   TIMESTAMP DEFAULT NULL
);

-- 인덱스는 테이블 생성 후 따로 만드는 것이 H2에서 가장 안전합니다.
CREATE UNIQUE INDEX idx_food_code ON foods(food_code);

DROP TABLE IF EXISTS follows;

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
                       is_public    BOOLEAN DEFAULT 1,
                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP,
                       deleted_at    TIMESTAMP
);

-- 팔로우 스키마
CREATE TABLE follows (
                         id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                         follower_id  BIGINT NOT NULL, -- 팔로우 하는 사람 (나)
                         following_id BIGINT NOT NULL, -- 팔로우 당하는 사람 (상대방)
                         created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 중복 팔로우 방지 (A가 B를 두 번 팔로우할 수 없음)
                         UNIQUE (follower_id, following_id),

    -- 외래키 설정 (유저 삭제 시 팔로우 관계도 삭제)
                         CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users (user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_following FOREIGN KEY (following_id) REFERENCES users (user_id) ON DELETE CASCADE
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
                              updated_at         TIMESTAMP,
                              deleted_at         TIMESTAMP DEFAULT NULL
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

-- 1. 챌린지 마스터 테이블
DROP TABLE IF EXISTS challenges;
DROP TABLE IF EXISTS user_challenges;
DROP TABLE IF EXISTS challenge_invitations;

-- H2 DB 호환 스키마

CREATE TABLE IF NOT EXISTS challenges (
                                          challenge_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          owner_id            BIGINT NOT NULL,
                                          title               VARCHAR(100) NOT NULL,
    description         TEXT,
    type                VARCHAR(30) NOT NULL,
    target_value        INT NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    goal_count          INT NOT NULL,
    max_participants    INT NOT NULL DEFAULT 10,
    current_head_count  INT NOT NULL DEFAULT 1,
    is_public           BOOLEAN NOT NULL DEFAULT TRUE, -- TINYINT 대신 BOOLEAN 권장 (H2)
    invitation_code     VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_challenges (
                                               user_challenge_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               user_id             BIGINT NOT NULL,
                                               challenge_id        BIGINT NOT NULL,
                                               status              VARCHAR(20) DEFAULT 'PROGRESS',
    current_count       INT DEFAULT 0,
    current_streak      INT DEFAULT 0,
    max_streak          INT DEFAULT 0,
    last_success_date   DATE DEFAULT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS challenge_invitations (
                                                     invitation_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     challenge_id        BIGINT NOT NULL,
                                                     inviter_id          BIGINT NOT NULL,
                                                     invitee_id          BIGINT NOT NULL,
                                                     status              VARCHAR(20) DEFAULT 'PENDING',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


-- [기존 유저 및 팔로우 데이터 (제공해주신 내용)]
-- (이미 DB에 있다면 이 부분은 생략 가능, 초기화 시 필요)
INSERT INTO users (email, platform, social_id, user_name, gender, birth_date, height_cm, weight_kg, role, status, status_message, is_public, created_at, updated_at)
VALUES ('kim_chulsoo@gmail.com', 'google', 'google_123456789', '헬스보이철수', 'M', '1992-05-15', 178.5, 76.0, 'ROLE_USER', 'ACTIVE', '3대 500 치는 그날까지! 💪', TRUE, NOW(), NOW());

INSERT INTO users (email, platform, social_id, user_name, gender, birth_date, height_cm, weight_kg, role, status, status_message, allergies, diseases, is_public, created_at, updated_at)
VALUES ('lee_younghee@gmail.com', 'google', 'google_987654321', '샐러드조아', 'F', '1995-10-20', 162.0, 48.5, 'ROLE_USER', 'ACTIVE', '건강하게 다이어트 하기 🌱', '복숭아,우유', '당뇨', FALSE, NOW(), NOW());

INSERT INTO users (email, platform, social_id, user_name, gender, birth_date, role, status, is_public, created_at, updated_at)
VALUES ('new_user@gmail.com', 'google', 'google_99887766', '뉴비', NULL, NULL, 'ROLE_USER', 'ACTIVE', TRUE, NOW(), NOW());

INSERT INTO users (email, platform, social_id, user_name, gender, birth_date, height_cm, weight_kg, role, status, status_message, is_public, created_at, updated_at)
VALUES ('admin@matchmeal.com', 'google', 'google_admin_001', '관리자', 'M', '1990-01-01', 180.0, 80.0, 'ROLE_ADMIN', 'ACTIVE', '관리자 계정입니다.', FALSE, NOW(), NOW());

INSERT INTO follows (follower_id, following_id, created_at) VALUES (1, 2, NOW());
INSERT INTO follows (follower_id, following_id, created_at) VALUES (2, 1, NOW());
INSERT INTO follows (follower_id, following_id, created_at) VALUES (3, 1, NOW());
INSERT INTO follows (follower_id, following_id, created_at) VALUES (3, 2, NOW());
INSERT INTO follows (follower_id, following_id, created_at) VALUES (1, 4, NOW());


-- ==========================================
-- [추가] 챌린지 및 참여 데이터 (Challenges & UserChallenges)
-- ==========================================

-- 1. [챌린지 생성]
-- 1-1. 철수(ID:1)가 만든 "벌크업 챌린지" (칼로리 섭취 목표)
INSERT INTO challenges (
    owner_id, title, description, type, target_value,
    start_date, end_date, goal_count,
    max_participants, current_head_count, is_public, invitation_code, created_at
) VALUES (
             1, '💪 3대 500을 위한 벌크업 식단', '근성장을 위해 하루 2500kcal 이상 섭취합시다!', 'CALORIE_LIMIT', 2500,
             CURRENT_DATE - INTERVAL '5' DAY, CURRENT_DATE + INTERVAL '25' DAY, 20,
             50, 3, TRUE, 'BULKUP01', NOW()
         );

-- 1-2. 철수(ID:1)가 만든 "매일 식단 기록" (기록 습관)
INSERT INTO challenges (
    owner_id, title, description, type, target_value,
    start_date, end_date, goal_count,
    max_participants, current_head_count, is_public, invitation_code, created_at
) VALUES (
             1, '📝 헬린이 탈출! 매일 기록하기', '식단 기록 습관을 기릅시다. 하루 3번 기록!', 'RECORD_FREQUENCY', 3,
             CURRENT_DATE - INTERVAL '1' DAY, CURRENT_DATE + INTERVAL '13' DAY, 14,
             20, 1, TRUE, 'HELLIN01', NOW()
         );

-- 1-3. 영희(ID:2)가 만든 "당뇨 관리 식단" (영희의 질병 정보 반영)
INSERT INTO challenges (
    owner_id, title, description, type, target_value,
    start_date, end_date, goal_count,
    max_participants, current_head_count, is_public, invitation_code, created_at
) VALUES (
             2, '🥗 당뇨 관리 - 저염식 챌린지', '혈당 관리를 위한 건강한 식단 공유해요.', 'CALORIE_LIMIT', 1800,
             CURRENT_DATE, CURRENT_DATE + INTERVAL '30' DAY, 30,
             10, 5, TRUE, 'SUGARFREE', NOW()
         );

-- 1-4. 영희(ID:2)가 만든 "아침형 인간" (시간 제한)
INSERT INTO challenges (
    owner_id, title, description, type, target_value,
    start_date, end_date, goal_count,
    max_participants, current_head_count, is_public, invitation_code, created_at
) VALUES (
             2, '⏰ 미라클 모닝! 8시 전 아침먹기', '아침 8시 전에 식사를 마치고 인증하세요.', 'TIME_RANGE', 8,
             CURRENT_DATE + INTERVAL '1' DAY, CURRENT_DATE + INTERVAL '14' DAY, 14,
             100, 10, TRUE, 'MIRACLE8', NOW()
         );

-- 1-5. 관리자(ID:4)가 만든 "비공개 방" (코드 필요)
INSERT INTO challenges (
    owner_id, title, description, type, target_value,
    start_date, end_date, goal_count,
    max_participants, current_head_count, is_public, invitation_code, created_at
) VALUES (
             4, '🔒 관리자 시크릿 챌린지', '초대받은 VIP 회원만 입장 가능합니다.', 'RECORD_FREQUENCY', 1,
             CURRENT_DATE, CURRENT_DATE + INTERVAL '100' DAY, 100,
             5, 1, FALSE, 'SECRET12', NOW()
         );


-- 2. [챌린지 참여 현황 (UserChallenges)]
-- 철수(ID:1)의 참여 상태를 중점으로 구성

-- 2-1. 철수(1)가 본인이 만든 "벌크업 챌린지(ID:1)"에 참여 중 (진척도 25%)
INSERT INTO user_challenges (
    user_id, challenge_id, status,
    current_count, current_streak, max_streak, last_success_date, created_at
) VALUES (
             1, 1, 'PROGRESS',
             5, 5, 5, CURRENT_DATE - INTERVAL '1' DAY, NOW()
         );

-- 2-2. 철수(1)가 본인이 만든 "매일 기록하기(ID:2)"에 참여 중 (방금 시작)
INSERT INTO user_challenges (
    user_id, challenge_id, status,
    current_count, current_streak, max_streak, last_success_date, created_at
) VALUES (
             1, 2, 'PROGRESS',
             0, 0, 0, NULL, NOW()
         );

-- 2-3. 철수(1)가 영희(2)의 "당뇨 관리(ID:3)"에 참여 중 (조금 진행함)
INSERT INTO user_challenges (
    user_id, challenge_id, status,
    current_count, current_streak, max_streak, last_success_date, created_at
) VALUES (
             1, 3, 'PROGRESS',
             3, 1, 3, CURRENT_DATE - INTERVAL '2' DAY, NOW()
         );

-- [참여자 수(Head Count) 맞추기용 더미]
-- 벌크업 챌린지(1): 영희(2), 뉴비(3)도 참여
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (2, 1, 'PROGRESS');
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (3, 1, 'PROGRESS');

-- 당뇨 관리(3): 뉴비(3), 관리자(4) 등 참여
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (3, 3, 'PROGRESS');
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (4, 3, 'PROGRESS');
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (2, 3, 'PROGRESS'); -- 영희 본인도 참여

-- 아침형 인간(4): 다수 참여
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (3, 4, 'PROGRESS');
INSERT INTO user_challenges (user_id, challenge_id, status) VALUES (1, 4, 'PROGRESS'); -- 철수도 참여