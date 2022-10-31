CREATE TABLE active_directory_details (
  id                             BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id                        BIGINT NOT NULL,
  password_expire_date           DATE NULL,
  account_expire_date            DATE NULL,
  password_locked_date           DATE NULL,
  password_locked                BOOLEAN NOT NULL,
  
  CONSTRAINT active_directory_details_fk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE active_directory_details_aud (
  id                             BIGINT NOT NULL,
  rev                            BIGINT NOT NULL,
  revtype                        TINYINT DEFAULT NULL,

  user_id                        BIGINT NULL,
  password_expire_date           DATE NULL,
  account_expire_date            DATE NULL,
  password_locked_date           DATE NULL,
  password_locked                BOOLEAN NOT NULL,

  PRIMARY KEY (id, rev),
  KEY rev (rev),
  CONSTRAINT active_directory_details_aud_fk_1 FOREIGN KEY (rev) REFERENCES revisions (id)
);

INSERT INTO active_directory_details (user_id, password_expire_date, account_expire_date, password_locked, password_locked_date)
SELECT u.id, u.password_expire_date, u.account_expire_date, COALESCE(u.password_locked, 0), u.password_locked_date
FROM users u
WHERE u.user_type = 'ACTIVE_DIRECTORY';

ALTER TABLE users DROP COLUMN password_expire_date,
             DROP COLUMN account_expire_date,
             DROP COLUMN password_locked,
             DROP COLUMN password_locked_date;

ALTER TABLE users_aud DROP COLUMN password_expire_date,
             DROP COLUMN account_expire_date,
             DROP COLUMN password_locked,
             DROP COLUMN password_locked_date;
