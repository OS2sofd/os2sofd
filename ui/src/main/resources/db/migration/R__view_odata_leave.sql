CREATE OR REPLACE view `view_odata_leave`
AS
  SELECT l.`id`                       AS `id`,
         p.`uuid`                   AS `person_uuid`,
         l.`start_date`               AS `start_date`,
         l.`stop_date`                AS `stop_date`,
         l.`reason`                   AS `reason`,
         l.`reason_text`              AS `reason_text`,
         l.`disable_account_orders`   AS `disable_account_orders`,
         l.`expire_accounts`        	AS `expire_accounts`
  FROM   `persons_leave` l
  INNER JOIN persons p on p.leave_id = l.id;