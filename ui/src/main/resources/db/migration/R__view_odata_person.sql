CREATE OR REPLACE view `view_odata_person`
AS
  SELECT `persons`.`uuid`                       AS `uuid`,
         `persons`.`master`                     AS `master`,
         `persons`.`deleted`                    AS `deleted`,
         `persons`.`created`                    AS `created`,
         `persons`.`last_changed`               AS `last_changed`,
         `persons`.`firstname`                  AS `firstname`,
         `persons`.`surname`                    AS `surname`,
         `persons`.`cpr`                        AS `cpr`,
         `persons`.`chosen_name`                AS `chosen_name`,
         `persons`.`first_employment_date`      AS `first_employment_date`,
         `persons`.`anniversary_date`           AS `anniversary_date`,
         `persons`.`local_extensions`           AS `local_extensions`,
         `persons`.`registered_post_address_id` AS `registered_post_address_id`,
         `persons`.`residence_post_address_id`  AS `residence_post_address_id`,
         `persons`.`key_words`                  AS `key_words`,
         `persons`.`notes`                      AS `notes`,
         `persons`.`taxed_phone`                AS `taxed_phone`,
         `persons`.`disable_account_orders`     AS `disable_account_orders`,
         `persons`.`force_stop`                 AS `force_stop`
  FROM   `persons`;