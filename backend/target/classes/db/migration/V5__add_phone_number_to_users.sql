-- Add phone_number column to users table

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

-- Create index on phone_number for faster queries
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);

