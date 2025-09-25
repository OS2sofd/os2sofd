CREATE TABLE account_orders_aud (
  id                     BIGINT NOT NULL,
  rev                    BIGINT NOT NULL,
  revtype                TINYINT DEFAULT NULL,

  person_uuid            VARCHAR(36) NULL,
  employee_id            VARCHAR(255) NULL,
  requester_uuid         VARCHAR(36) NULL,
  requester_api_user_id  VARCHAR(255) NULL,
  ordered_timestamp      DATETIME NULL,
  user_type              VARCHAR(64) NULL,
  order_type             VARCHAR(64) NULL,
  activation_timestamp   DATETIME NULL,
  end_date               DATETIME NULL,
  status                 VARCHAR(64) NULL,
  message text           NULL,
  modified_timestamp     DATETIME NULL,
  person_notified        TINYINT NULL,
  requester_notified     TINYINT NULL,
  requested_user_id      VARCHAR(255) NULL,
  linked_user_id         VARCHAR(255) NULL,
  actual_user_id         VARCHAR(255) NULL,
  depends_on             BIGINT NULL,
  
  PRIMARY KEY (id,rev),
  KEY rev (rev),
  CONSTRAINT account_orders_aud_ibfk FOREIGN KEY (rev) REFERENCES revisions (id)
);
