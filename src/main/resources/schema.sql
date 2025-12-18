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
                       sugars       DOUBLE PRECISION,
                       sodium       DOUBLE PRECISION,

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
                              total_sugars       DOUBLE PRECISION DEFAULT 0,
                              total_sodium       DOUBLE PRECISION DEFAULT 0,

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
                              sugars             DOUBLE PRECISION DEFAULT 0,
                              sodium             DOUBLE PRECISION DEFAULT 0,

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

-- 기존 테이블이 있다면 삭제 (테스트 환경 초기화용)
DROP TABLE IF EXISTS comment_likes;
DROP TABLE IF EXISTS post_likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS post_files;
DROP TABLE IF EXISTS posts;

-- 1. 게시글 테이블
CREATE TABLE posts (
    post_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    category     VARCHAR(50) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content      TEXT NOT NULL,
    view_count   INT DEFAULT 0,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP NULL
);

-- 인덱스 (H2/MySQL 호환 문법)
CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_category ON posts (category);


-- 2. 게시글 첨부파일 테이블
CREATE TABLE post_files (
    file_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT NOT NULL,
    file_url     VARCHAR(500) NOT NULL,
    file_type    VARCHAR(20) NOT NULL,

    CONSTRAINT fk_post_files_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE
);


-- 3. 게시글 좋아요 테이블
CREATE TABLE post_likes (
    post_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_post_like_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE
);


-- 4. 댓글 테이블
CREATE TABLE comments (
    comment_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id           BIGINT NOT NULL,
    user_id           BIGINT NOT NULL,
    content           TEXT NOT NULL,
    parent_comment_id BIGINT NULL,

    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP NULL,

    CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent_id FOREIGN KEY (parent_comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_post_id ON comments (post_id);


-- 5. 댓글 좋아요 테이블
CREATE TABLE comment_likes (
    comment_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_comment_like_user UNIQUE (comment_id, user_id),
    CONSTRAINT fk_comment_likes_comment_id FOREIGN KEY (comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE
);

-- 테스트용 더미 데이터

-- 1. [공지사항] 10일 전 (2025-12-03)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (4, 'NOTICE', '📢 매치밀 서비스 오픈 안내', '건강한 식단과 운동 라이프를 위한 커뮤니티, 매치밀에 오신 것을 환영합니다!', 1500, '2025-12-03 09:00:00', '2025-12-03 09:00:00', NULL);

-- 2. [운동] 5일 전 (2025-12-08)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'WORKOUT', '등 운동 루틴 공유합니다 (렛풀다운 꿀팁)', '광배근 자극 제대로 먹이는 방법 알려드립니다.', 340, '2025-12-08 18:30:00', '2025-12-08 18:30:00', NULL);

-- 3. [식단] 4일 전 (2025-12-09)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (2, 'DIET', '당뇨 식단 3일차 기록 (혈당 수치 공개)', '현미밥 100g, 닭가슴살 100g, 야채 듬뿍.', 120, '2025-12-09 12:00:00', '2025-12-09 12:00:00', NULL);

-- 4. [질문] 3일 전 (2025-12-10)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (3, 'QNA', '헬린이 질문이요.. 단백질 보충제 꼭 먹어야 하나요?', '이제 막 운동 시작했는데 자연식으로만 섭취해도 될까요?', 55, '2025-12-10 14:15:00', '2025-12-10 14:15:00', NULL);

-- 5. [자유] 2일 전 (2025-12-11)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'FREE', '오늘 날씨 진짜 좋네요 러닝 뛰고 싶다', '한강공원 갈 사람 있나요?', 12, '2025-12-11 10:00:00', '2025-12-11 10:00:00', NULL);

-- 6. [식단] 2일 전 (2025-12-11)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (2, 'DIET', '🥗 맛있는 닭가슴살 샐러드 드레싱 레시피', '올리브오일, 발사믹 식초, 그리고 알룰로스를 섞으면 진짜 맛있어요!', 890, '2025-12-11 19:00:00', '2025-12-11 19:00:00', NULL);

-- 7. [삭제됨] 2일 전 작성, 당일 삭제
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'FREE', '이 글은 삭제된 게시글입니다.', '잘못 올렸어요 ㅠㅠ 삭제합니다.', 5, '2025-12-11 20:00:00', '2025-12-11 20:05:00', '2025-12-11 20:05:00');

-- 8. [운동] 1일 전 (2025-12-12)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'WORKOUT', '드디어 3대 500 달성했습니다!! (영상 유)', '스쿼트 180, 데드 200, 벤치 120 성공했습니다.', 2100, '2025-12-12 09:30:00', '2025-12-12 09:30:00', NULL);

-- 9. [질문] 1일 전 (2025-12-12)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (2, 'QNA', '살이 너무 안 빠져요.. 정체기 극복 팁 좀', '식단도 하고 운동도 하는데 몸무게가 그대로입니다.', 230, '2025-12-12 15:20:00', '2025-12-12 15:20:00', NULL);

-- 10. [공지] 12시간 전 (2025-12-12 저녁)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (4, 'NOTICE', '[공지] 새벽 서버 점검 안내 (02:00 ~ 04:00)', '더 나은 서비스를 위해 서버 점검이 진행됩니다.', 450, '2025-12-12 20:00:00', '2025-12-12 20:00:00', NULL);

-- 11. [자유] 10시간 전 (2025-12-12 밤)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (3, 'FREE', '안녕하세요~ 오늘 가입했습니다!', '열심히 활동하겠습니다. 잘 부탁드려요.', 30, '2025-12-12 22:00:00', '2025-12-12 22:00:00', NULL);

-- 12. [운동] 5시간 전 (2025-12-13 새벽)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (2, 'WORKOUT', '집에서 하기 좋은 층간소음 없는 유산소', '슬로우 버피랑 마운틴 클라이머 추천합니다.', 150, '2025-12-13 07:00:00', '2025-12-13 07:00:00', NULL);

-- 13. [리뷰] 3시간 전 (2025-12-13 오전)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'REVIEW', 'XX 피트니스 3개월 이용 솔직 후기', '기구는 좋은데 샤워실이 좀 좁네요.', 67, '2025-12-13 09:00:00', '2025-12-13 09:00:00', NULL);

-- 14. [삭제됨] 1시간 전 (2025-12-13 오전 11시)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (3, 'FREE', '광고성 게시글입니다', '비트코인 대박 정보...', 0, '2025-12-13 11:00:00', '2025-12-13 11:10:00', '2025-12-13 11:10:00');

-- 15. [최신글] 방금 전 (2025-12-13 정오)
INSERT INTO posts (user_id, category, title, content, view_count, created_at, updated_at, deleted_at)
VALUES (1, 'FREE', '운동 끝나고 먹는 치킨은 0칼로리 맞죠?', '양심상 튀김 옷은 벗기고 먹겠습니다 ㅎㅎ', 1, '2025-12-13 12:00:00', '2025-12-13 12:00:00', NULL);


-- (옵션) 연관 데이터들도 필요하시면 이것까지 복사하세요

INSERT INTO post_likes (post_id, user_id) VALUES (8, 2);
INSERT INTO post_likes (post_id, user_id) VALUES (8, 3);
INSERT INTO post_likes (post_id, user_id) VALUES (8, 4);
INSERT INTO post_likes (post_id, user_id) VALUES (6, 1);
INSERT INTO post_likes (post_id, user_id) VALUES (6, 3);

INSERT INTO comments (post_id, user_id, content, parent_comment_id) VALUES (4, 1, '초보자는 신타6 추천드립니다.', NULL);
INSERT INTO comments (post_id, user_id, content, parent_comment_id) VALUES (4, 2, '저는 마이프로틴 먹어요.', NULL);
