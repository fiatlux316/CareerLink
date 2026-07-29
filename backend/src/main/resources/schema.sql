-- careerlink schema definition

CREATE TABLE IF NOT EXISTS student_sessions (
    id BIGSERIAL PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    student_phone VARCHAR(255) NOT NULL,
    school_type VARCHAR(50) NOT NULL,
    grade INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consultation_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

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
