CREATE TABLE known_networks (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    ip                      VARCHAR(255) NOT NULL,
    client_id               BIGINT NOT NULL,

    FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE TABLE known_networks_aud (
    id                      BIGINT,
    ip                      VARCHAR(255),
    client_id               BIGINT
);