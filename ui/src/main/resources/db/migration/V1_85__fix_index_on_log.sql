ALTER TABLE security_log ADD INDEX idx_sec_log_tts (timestamp);
ALTER TABLE audit_log ADD INDEX idx_aud_log_tts (timestamp);
