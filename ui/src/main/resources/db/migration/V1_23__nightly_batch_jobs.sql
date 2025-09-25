CREATE TABLE batch_job_execution (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  job_name                     VARCHAR(255) NOT NULL UNIQUE,
  last_execution_time          DATETIME NULL
);