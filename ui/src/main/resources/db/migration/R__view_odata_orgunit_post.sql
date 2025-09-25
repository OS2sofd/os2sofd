CREATE OR REPLACE view `view_odata_orgunit_post`
AS
  SELECT `posts`.`id`                    AS `id`,
         `posts`.`prime`                 AS `prime`,
         `posts`.`street`                AS `street`,
         `posts`.`localname`             AS `localname`,
         `posts`.`postal_code`           AS `postal_code`,
         `posts`.`city`                  AS `city`,
         `posts`.`country`               AS `country`,
         `posts`.`address_protected`     AS `address_protected`,
         `posts`.`master`                AS `master`,
         `posts`.`master_id`             AS `master_id`,
         `orgunits_posts`.`orgunit_uuid` AS `org_unit_uuid`
  FROM   `posts`
  JOIN `orgunits_posts` ON `orgunits_posts`.`post_id` = `posts`.`id`;
