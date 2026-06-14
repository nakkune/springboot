#!/bin/bash
PGPASSWORD='1q@W3e4r5t' psql -h localhost -p 5433 -U spring -d springdb -c "ALTER TABLE users ADD COLUMN IF NOT EXISTS theme VARCHAR(20) DEFAULT 'dark';"
