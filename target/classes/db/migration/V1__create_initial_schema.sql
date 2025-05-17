-- V1__create_initial_schema.sql

-- Create users table
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       phone_number VARCHAR(255),
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       preferences JSONB
);

-- Create roles table
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL UNIQUE,
                       description TEXT,
                       created_at TIMESTAMP NOT NULL
);

-- Create agencies table
CREATE TABLE agencies (
                          id UUID PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          contact_email VARCHAR(255),
                          phone_number VARCHAR(255),
                          jurisdiction JSONB,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create junction table for users and roles
CREATE TABLE user_roles (
                            id UUID PRIMARY KEY,
                            user_id UUID NOT NULL REFERENCES users(id),
                            role_id INTEGER NOT NULL REFERENCES roles(id),
                            UNIQUE(user_id, role_id)
);

-- Create agency_representatives table with additional fields
CREATE TABLE agency_representatives (
                                        id UUID PRIMARY KEY,
                                        user_id UUID NOT NULL REFERENCES users(id),
                                        agency_id UUID NOT NULL REFERENCES agencies(id),
                                        name VARCHAR(255),
                                        assigned_at TIMESTAMP NOT NULL,
                                        is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                        UNIQUE(user_id, agency_id)
);

-- Insert initial roles
INSERT INTO roles (name, description, created_at) VALUES
                                                      ('ADMIN', 'System administrator with full privileges', NOW()),
                                                      ('USER', 'Regular user with limited access', NOW()),
                                                      ('AGENCY_ADMIN', 'Administrator for a specific agency', NOW());