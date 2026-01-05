-- Create Booking System Tables
-- Run this after creating the users table

-- Create Branches Table
CREATE TABLE IF NOT EXISTS branches (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create Slots Table
CREATE TABLE IF NOT EXISTS slots (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    branch_id BIGINT NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_capacity INTEGER NOT NULL DEFAULT 2,
    booked_count INTEGER NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_slot_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    CONSTRAINT uk_slot_branch_date_time UNIQUE (branch_id, slot_date, start_time, end_time)
);

-- Create Bookings Table
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    user_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED')),
    payment_id VARCHAR(255),
    reserved_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE CASCADE
);

-- Create Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_slots_branch_date ON slots(branch_id, slot_date) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_slots_date ON slots(slot_date) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_slot ON bookings(slot_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_reserved_at ON bookings(reserved_at) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_expires_at ON bookings(expires_at) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_payment_id ON bookings(payment_id) WHERE deleted = FALSE AND payment_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE branches IS 'Stores branch/city information';
COMMENT ON TABLE slots IS 'Stores available time slots (9 AM to 7 PM, hourly)';
COMMENT ON TABLE bookings IS 'Stores user bookings with PENDING/CONFIRMED/EXPIRED status';
COMMENT ON COLUMN slots.max_capacity IS 'Maximum bookings allowed per slot (default: 2)';
COMMENT ON COLUMN slots.booked_count IS 'Current number of confirmed and pending bookings';
COMMENT ON COLUMN bookings.status IS 'Booking status: PENDING (reserved, payment pending), CONFIRMED (paid), EXPIRED (timeout/failed)';
COMMENT ON COLUMN bookings.expires_at IS 'Reservation expiration time (10 minutes from reserved_at)';

-- Sample data for testing
-- Insert sample branches
INSERT INTO branches (name, city, address, is_active) VALUES
('Downtown Branch', 'New York', '123 Main St, New York, NY', true),
('Uptown Branch', 'New York', '456 Park Ave, New York, NY', true),
('Central Branch', 'Los Angeles', '789 Sunset Blvd, Los Angeles, CA', true)
ON CONFLICT DO NOTHING;

