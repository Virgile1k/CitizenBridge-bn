

-- 2. Make sure the UUID extension is available
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 3. Recreate tables with proper types
-- Create the roles table first (it's referenced by user_roles)
CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description TEXT,
                       created_at TIMESTAMP NOT NULL
);

-- Create the users table with JSONB type for preferences
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       phone_number VARCHAR(50),
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       preferences JSONB DEFAULT '{}',  -- Initialize with empty JSON object
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL
);

-- Create the user_roles table
CREATE TABLE user_roles (
                            id SERIAL PRIMARY KEY,
                            user_id UUID NOT NULL,
                            role_id INTEGER NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                            UNIQUE (user_id, role_id)
);

-- Create the agencies table (assuming structure based on your application context)
CREATE TABLE agencies (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          contact_email VARCHAR(255),
                          contact_phone VARCHAR(50),
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);

-- Create the agency_representatives table (assuming structure)
CREATE TABLE agency_representatives (
                                        id SERIAL PRIMARY KEY,
                                        user_id UUID NOT NULL,
                                        agency_id INTEGER NOT NULL,
                                        title VARCHAR(100),
                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP NOT NULL,
                                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                        FOREIGN KEY (agency_id) REFERENCES agencies(id) ON DELETE CASCADE,
                                        UNIQUE (user_id, agency_id)
);

-- 4. Insert default roles
INSERT INTO roles (name, description, created_at) VALUES
                                                      ('USER', 'Regular user with basic privileges', NOW()),
                                                      ('MODERATOR', 'User with moderation capabilities', NOW()),
                                                      ('ADMIN', 'Administrator with full system access', NOW());