CREATE OR REPLACE view `view_odata_user`
AS
  SELECT `users`.`uuid`                           AS `uuid`,
         `users`.`master`                         AS `master`,
         `users`.`master_id`                      AS `master_id`,
         `users`.`user_id`                        AS `user_id`,
         `users`.`local_extensions`               AS `local_extensions`,
         `users`.`user_type`                      AS `user_type`,
         `users`.`prime`                          AS `prime`,
         `users`.`employee_id`                    AS `employee_id`,
         `persons_users`.`person_uuid`            AS `person_uuid`,
         `active_directory_details`.`kombit_uuid` AS `kombit_uuid`
  FROM   `users`
  JOIN `persons_users` ON `persons_users`.`user_id` = `users`.`id`
  LEFT JOIN `active_directory_details` ON `active_directory_details`.`user_id` = `users`.`id`
  WHERE ifnull(`users`.disabled,0) = 0;