CREATE TABLE client_ip_address (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    ip                      VARCHAR(255) NOT NULL,
    client_id               BIGINT NOT NULL,

    FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE TABLE client_ip_address_aud (
    id                      BIGINT,
    ip                      VARCHAR(255),
    client_id               BIGINT,
    rev                     BIGINT NOT NULL,
    revtype                 BOOLEAN DEFAULT NULL,

    FOREIGN KEY (rev) REFERENCES revisions(id)
);

INSERT INTO client_ip_address SELECT * FROM known_networks;

DROP TABLE known_networks;
DROP TABLE known_networks_aud;

ALTER TABLE client ADD COLUMN internal BOOLEAN DEFAULT FALSE;
ALTER TABLE client_aud ADD COLUMN internal BOOLEAN;