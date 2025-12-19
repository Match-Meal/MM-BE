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



