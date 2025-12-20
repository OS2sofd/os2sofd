CREATE TABLE classification
(
    id         BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    identifier VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,

    CONSTRAINT uq_classification_identifier UNIQUE (identifier)
);
CREATE TABLE classification_aud
(
    id         BIGINT       NOT NULL,
    rev        BIGINT       NOT NULL,
    revtype    TINYINT DEFAULT NULL,

    identifier VARCHAR(255) NULL,
    name       VARCHAR(255) NULL,

    PRIMARY KEY (id, rev),
    KEY rev (rev),
    CONSTRAINT fk_classification_aud_rev FOREIGN KEY (rev) REFERENCES revisions (id)
);
CREATE TABLE classification_item
(
    id                BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
    classification_id BIGINT        NOT NULL,
    identifier        VARCHAR(255) NOT NULL,
    name              VARCHAR(1024) NULL,

    CONSTRAINT fk_classification_item_classification FOREIGN KEY (classification_id) REFERENCES classification (id) ON DELETE CASCADE,
    CONSTRAINT uq_classification_item_identifier UNIQUE (classification_id, identifier)
);
CREATE TABLE classification_item_aud
(
    id                BIGINT        NOT NULL,
    rev               BIGINT        NOT NULL,
    revtype           TINYINT DEFAULT NULL,

    classification_id BIGINT        NULL,
    identifier        VARCHAR(1024) NULL,
    name              VARCHAR(1024) NULL,

    PRIMARY KEY (id, rev),
    KEY rev (rev),
    CONSTRAINT fk_classification_item_aud_rev FOREIGN KEY (rev) REFERENCES revisions (id)
);