# Database Setup Instructions

## Prerequisites
- PostgreSQL installed and running
- PostgreSQL user with CREATE DATABASE privileges

## Setup Steps

### 1. Create Database (if not exists)
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE giggles_db;

# Connect to the new database
\c giggles_db;
```

### 2. Run SQL Script
```bash
# From the project root directory
psql -U postgres -d giggles_db -f database/create_database.sql
```

Or execute the SQL file directly in your PostgreSQL client.

### 3. Verify Tables
```sql
-- Check if tables are created
\dt

-- Check users table structure
\d users

-- Check user_sessions table structure
\d user_sessions
```

## Database Configuration

Update `application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/giggles_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

## Table Structure

### users
- Stores user account information
- Unique constraints on `email` and `phone_number`
- Soft delete support with `deleted` flag

### user_sessions
- Stores JWT session tokens
- Foreign key relationship with `users` table
- Tracks session status and expiration
- Soft delete support with `deleted` flag

## Notes
- All tables use soft delete (records marked as deleted, not physically removed)
- JWT tokens have 4-hour expiration (14400000 milliseconds)
- Session type defaults to MULTI (allows multiple concurrent sessions)
- User role defaults to USER

