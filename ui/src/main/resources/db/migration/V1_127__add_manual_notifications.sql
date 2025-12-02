CREATE TABLE manual_notifications (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    active              BIT(1) NOT NULL,
    title               VARCHAR(255),
    details             TEXT,
    frequency           VARCHAR(50),
    frequency_qualifier INT NOT NULL,
    first_date          DATE,
    next_date           DATE,
    last_run            DATE
);

CREATE TABLE manual_notifications_aud (
    id                  BIGINT,
    rev                 BIGINT NOT NULL,
    revtype             TINYINT DEFAULT NULL,
    active              BIT(1),
    title               VARCHAR(255),
    details             TEXT,
    frequency           VARCHAR(50),
    frequency_qualifier INT,
    first_date          DATE,
    next_date           DATE,
    last_run            DATE,

    FOREIGN KEY fk_manual_notifications_aud_rev (rev) REFERENCES revisions(id),
    PRIMARY KEY pk_manual_notifications_aud (id, rev)
);