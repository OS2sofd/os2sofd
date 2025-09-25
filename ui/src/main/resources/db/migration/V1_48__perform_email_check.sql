ALTER TABLE email_queue ADD COLUMN perform_email_check BOOLEAN NOT NULL DEFAULT 0;
DROP TABLE future_emails_persons;
DROP TABLE future_emails;
