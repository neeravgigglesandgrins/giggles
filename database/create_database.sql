-- Create Database
-- Run this if you need to create the database first
-- CREATE DATABASE giggles_db;
-- \c giggles_db;

-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20) UNIQUE,
    address TEXT,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    session_type VARCHAR(20) NOT NULL DEFAULT 'MULTI' CHECK (session_type IN ('SINGLE', 'MULTI')),
    login_attempts INTEGER DEFAULT 0,
    is_locked BOOLEAN DEFAULT FALSE
);

-- Create User Sessions Table
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    user_id BIGINT NOT NULL,
    token VARCHAR(2000) UNIQUE NOT NULL,
    expiry TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID' CHECK (status IN ('VALID', 'INVALID', 'EXPIRED')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON user_sessions(token);
CREATE INDEX IF NOT EXISTS idx_user_sessions_status ON user_sessions(status);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expiry ON user_sessions(expiry);
CREATE INDEX IF NOT EXISTS idx_user_sessions_deleted ON user_sessions(deleted);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information';
COMMENT ON TABLE user_sessions IS 'Stores JWT session tokens for users';
COMMENT ON COLUMN users.deleted IS 'Soft delete flag - records are not physically deleted';
COMMENT ON COLUMN user_sessions.deleted IS 'Soft delete flag - records are not physically deleted';
COMMENT ON COLUMN user_sessions.status IS 'Session status: VALID, INVALID, or EXPIRED';
COMMENT ON COLUMN user_sessions.expiry IS 'Token expiration timestamp (4 hours from creation)';

