CREATE TABLE account_orders_approved (
   id                   BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
   approver_uuid        VARCHAR(36) NOT NULL,
   approver_name        VARCHAR(255) NOT NULL,
   person_uuid          VARCHAR(36) NOT NULL,
   person_name          VARCHAR(255) NOT NULL,
   user_id              VARCHAR(255) NOT NULL,
   approved_tts         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO settings (setting_key, setting_value) VALUES ('ACCOUNT_APPROVAL_DEPLOYED', CURRENT_DATE);
