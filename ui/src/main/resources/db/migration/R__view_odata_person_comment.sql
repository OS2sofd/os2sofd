CREATE OR REPLACE view `view_odata_person_comment`
AS
SELECT
	`comment`.`id`           AS `id`,
	`comment`.`person_uuid`	AS `person_uuid`,
	`comment`.`user_name`  	AS `user_name`,
	`comment`.`timestamp`  	AS `timestamp`,
	`comment`.`comment`    	AS `comment`
FROM `comment`