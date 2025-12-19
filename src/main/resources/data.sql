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

