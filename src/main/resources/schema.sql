-- 테이블 초기화 (순서 중요: 자식 -> 부모)
DROP TABLE IF EXISTS comment_likes;
DROP TABLE IF EXISTS post_likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS post_files;
DROP TABLE IF EXISTS posts;

DROP TABLE IF EXISTS challenge_invitations;
DROP TABLE IF EXISTS user_challenges;
DROP TABLE IF EXISTS challenges;

DROP TABLE IF EXISTS diet_details;
DROP TABLE IF EXISTS diet_records;
DROP TABLE IF EXISTS foods;

DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS users;


-- 1. 유저 테이블
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
                       is_public     BOOLEAN DEFAULT 1,
                       created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP,
                       deleted_at    TIMESTAMP
);

-- 2. 팔로우 (개인 관계이므로 탈퇴 시 삭제 - CASCADE)
CREATE TABLE follows (
                         id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                         follower_id  BIGINT NOT NULL,
                         following_id BIGINT NOT NULL,
                         created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         UNIQUE (follower_id, following_id),
                         CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users (user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_following FOREIGN KEY (following_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- 3. 음식 (개인 등록 음식은 삭제 - CASCADE)
CREATE TABLE foods (
                       food_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id      BIGINT,
                       food_code    VARCHAR(50) NOT NULL,
                       food_name    VARCHAR(255) NOT NULL,
                       category     VARCHAR(100),
                       serving_size DOUBLE PRECISION,
                       unit         VARCHAR(10) DEFAULT 'g',
                       calories     DOUBLE PRECISION,
                       protein      DOUBLE PRECISION,
                       fat          DOUBLE PRECISION,
                       carbohydrate DOUBLE PRECISION,
                       sugars       DOUBLE PRECISION,
                       sodium       DOUBLE PRECISION,
                       created_at   TIMESTAMP,
                       updated_at   TIMESTAMP,
                       deleted_at   TIMESTAMP DEFAULT NULL,

                       CONSTRAINT fk_foods_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX idx_food_code ON foods(food_code);

-- 4. 식단 (지극히 개인적인 기록이므로 삭제 - CASCADE)
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
                              deleted_at         TIMESTAMP DEFAULT NULL,

                              CONSTRAINT fk_diet_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
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

                              CONSTRAINT fk_diet_details_diet FOREIGN KEY (diet_id) REFERENCES diet_records (diet_id) ON DELETE CASCADE
);

-- 5. 챌린지 (방장이 나가도 방은 유지 - SET NULL)
CREATE TABLE challenges (
                            challenge_id        BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- [보존] 방장이 탈퇴해도 챌린지는 유지되어야 함 (NULL 허용)
                            owner_id            BIGINT,

                            title               VARCHAR(100) NOT NULL,
                            description         TEXT,
                            type                VARCHAR(30) NOT NULL,
                            target_value        INT NOT NULL,
                            start_date          DATE NOT NULL,
                            end_date            DATE NOT NULL,
                            goal_count          INT NOT NULL,
                            max_participants    INT NOT NULL DEFAULT 10,
                            current_head_count  INT NOT NULL DEFAULT 1,
                            is_public           BOOLEAN NOT NULL DEFAULT TRUE,
                            invitation_code     VARCHAR(20) NOT NULL,
                            created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- [핵심] SET NULL 설정
                            CONSTRAINT fk_challenge_owner FOREIGN KEY (owner_id) REFERENCES users (user_id) ON DELETE SET NULL
);

-- 챌린지 참여 기록 (유저가 탈퇴하면 참여 명단에선 빠져야 함 - CASCADE)
CREATE TABLE user_challenges (
                                 user_challenge_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id             BIGINT NOT NULL,
                                 challenge_id        BIGINT NOT NULL,
                                 status              VARCHAR(20) DEFAULT 'PROGRESS',
                                 current_count       INT DEFAULT 0,
                                 current_streak      INT DEFAULT 0,
                                 max_streak          INT DEFAULT 0,
                                 last_success_date   DATE DEFAULT NULL,
                                 created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at          TIMESTAMP DEFAULT NULL,

                                 CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
                                 CONSTRAINT fk_uc_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (challenge_id) ON DELETE CASCADE
);

CREATE TABLE challenge_invitations (
                                       invitation_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       challenge_id        BIGINT NOT NULL,
                                       inviter_id          BIGINT NOT NULL,
                                       invitee_id          BIGINT NOT NULL,
                                       status              VARCHAR(20) DEFAULT 'PENDING',
                                       created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_invite_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (challenge_id) ON DELETE CASCADE,
                                       CONSTRAINT fk_invite_inviter FOREIGN KEY (inviter_id) REFERENCES users (user_id) ON DELETE CASCADE,
                                       CONSTRAINT fk_invite_invitee FOREIGN KEY (invitee_id) REFERENCES users (user_id) ON DELETE CASCADE
);


-- 6. 게시글 (탈퇴해도 글은 유지 - SET NULL)
CREATE TABLE posts (
                       post_id      BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- [보존] 작성자 ID NULL 허용
                       user_id      BIGINT,

                       category     VARCHAR(50) NOT NULL,
                       title        VARCHAR(255) NOT NULL,
                       content      TEXT NOT NULL,
                       view_count   INT DEFAULT 0,
                       created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at   TIMESTAMP NULL,

    -- [핵심] 유저 삭제 시 user_id를 NULL로 변경
                       CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);
CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_category ON posts (category);

CREATE TABLE post_files (
                            file_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id      BIGINT NOT NULL,
                            file_url     VARCHAR(500) NOT NULL,
                            file_type    VARCHAR(20) NOT NULL,

                            CONSTRAINT fk_post_files_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE
);

-- 7. 게시글 좋아요 (탈퇴해도 좋아요 숫자는 유지 - SET NULL)
CREATE TABLE post_likes (
                            post_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id      BIGINT NOT NULL,

    -- [보존] 좋아요 누른 사람 NULL 허용
                            user_id      BIGINT,

                            created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                            UNIQUE (post_id, user_id), -- NULL은 중복 허용됨 (여러 탈퇴자가 좋아요 가능)
                            CONSTRAINT fk_post_likes_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,

    -- [핵심] SET NULL
                            CONSTRAINT fk_post_likes_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);


-- 8. 댓글 (탈퇴해도 댓글 내용은 유지 - SET NULL)
CREATE TABLE comments (
                          comment_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                          post_id           BIGINT NOT NULL,

    -- [보존] 작성자 NULL 허용
                          user_id           BIGINT,

                          content           TEXT NOT NULL,
                          parent_comment_id BIGINT NULL,
                          created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          deleted_at        TIMESTAMP NULL,

                          CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
                          CONSTRAINT fk_comments_parent_id FOREIGN KEY (parent_comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE,

    -- [핵심] SET NULL
                          CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);
CREATE INDEX idx_comments_post_id ON comments (post_id);

-- 9. 댓글 좋아요 (탈퇴해도 좋아요 숫자는 유지 - SET NULL)
CREATE TABLE comment_likes (
                               comment_like_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               comment_id      BIGINT NOT NULL,

    -- [보존] 좋아요 누른 사람 NULL 허용
                               user_id         BIGINT,

                               created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               UNIQUE (comment_id, user_id),
                               CONSTRAINT fk_comment_likes_comment_id FOREIGN KEY (comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE,

    -- [핵심] SET NULL
                               CONSTRAINT fk_comment_likes_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);