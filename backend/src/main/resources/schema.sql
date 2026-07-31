-- careerlink schema definition

CREATE TABLE IF NOT EXISTS student_sessions (
    id BIGSERIAL PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    student_phone VARCHAR(255) NOT NULL,
    school_type VARCHAR(50) NOT NULL,
    grade INT NOT NULL DEFAULT 0,
    gender INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 기존 DB 스키마 마이그레이션 (컬럼 및 체크 제약조건 자동 보정)
ALTER TABLE student_sessions DROP CONSTRAINT IF EXISTS student_sessions_school_type_check;
ALTER TABLE student_sessions ADD COLUMN IF NOT EXISTS gender INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS consultation_topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

INSERT INTO consultation_topics (id, name, description)
VALUES (1, '학과/직업', '학과 및 직업 탐색 관련 상담 테마'),
       (2, '자기이해', '나를 더욱 깊이 이해하는 상담 테마')
ON CONFLICT (name) DO NOTHING;

SELECT setval('consultation_topics_id_seq', COALESCE((SELECT MAX(id) FROM consultation_topics), 1));

CREATE TABLE IF NOT EXISTS consultation_types (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT REFERENCES consultation_topics(id),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

ALTER TABLE consultation_types ADD COLUMN IF NOT EXISTS topic_id BIGINT REFERENCES consultation_topics(id);
UPDATE consultation_types SET topic_id = 1 WHERE topic_id IS NULL;

CREATE TABLE IF NOT EXISTS consultations (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_sessions(id),
    type_id BIGINT NOT NULL REFERENCES consultation_types(id),
    status VARCHAR(50) NOT NULL,
    counselor_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    masked_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS counselor_sessions (
    id BIGSERIAL PRIMARY KEY,
    counselor_name VARCHAR(255) NOT NULL,
    counselor_phone VARCHAR(255) NOT NULL,
    type_id BIGINT NOT NULL REFERENCES consultation_types(id),
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
