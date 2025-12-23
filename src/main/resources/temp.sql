CREATE TABLE badges (
                        badge_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
                        category      VARCHAR(50) NOT NULL, -- COMMUNITY, DIET, CHALLENGE, SUBSCRIPTION
                        sub_category  VARCHAR(50) NOT NULL, -- POST_COUNT, DIET_STREAK, etc.
                        name          VARCHAR(100) NOT NULL, -- 뱃지 이름 (예: "소통의 신")
                        description   VARCHAR(255),          -- 뱃지 설명
                        target_value  INT NOT NULL,          -- 달성 목표 값 (10, 25, 50...)
                        image_url     VARCHAR(500) NOT NULL, -- 활성화(컬러) 이미지 URL
                        gray_image_url VARCHAR(500) NOT NULL, -- 비활성화(회색) 이미지 URL
                        tier          INT NOT NULL           -- 등급 (1=10개, 5=500개 등, 프로필 노출 우선순위용)
);

CREATE TABLE user_badges (
                             user_badge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id       BIGINT NOT NULL,
                             badge_id      BIGINT NOT NULL,
                             current_value INT DEFAULT 0,         -- 현재 달성 수치 (예: 작성한 글 수 8)
                             is_acquired   BOOLEAN DEFAULT FALSE, -- 획득 여부
                             acquired_at   DATETIME,              -- 획득 일시

                             CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_ub_badge FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE,
                             UNIQUE (user_id, badge_id) -- 유저당 뱃지는 하나씩만 존재
);