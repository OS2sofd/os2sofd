UPDATE users SET employee_id = NULL WHERE employee_id = '' AND user_type = 'ACTIVE_DIRECTORY';
