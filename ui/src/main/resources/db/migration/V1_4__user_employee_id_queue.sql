CREATE TABLE user_change_employee_id_queue (
  id                    BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id               BIGINT NOT NULL,
  employee_id           VARCHAR(255) NOT NULL,
  date_of_transaction   DATE NOT NULL,

  CONSTRAINT user_change_employee_id_queue_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);