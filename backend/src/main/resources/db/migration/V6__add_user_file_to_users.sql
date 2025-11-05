-- Add user_file column to users table for storing file paths

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS user_file VARCHAR(500);

-- Create index on user_file for faster queries
CREATE INDEX IF NOT EXISTS idx_users_user_file ON users(user_file);

