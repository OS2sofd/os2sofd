CREATE TABLE `revisions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timestamp` bigint NOT NULL,
  `auditor_id` varchar(128) DEFAULT NULL,
  `auditor_name` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `client` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `api_key` varchar(36) NOT NULL,
  `access_role` varchar(36) NOT NULL,
  `monitor_for_activity` tinyint NOT NULL DEFAULT '0',
  `show_on_frontpage` tinyint NOT NULL DEFAULT '0',
  `last_active` timestamp NULL DEFAULT NULL,
  `version` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `access_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `entity` varchar(36) NOT NULL,
  `field` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `client_id` (`client_id`),
  CONSTRAINT `access_field_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `client` (`id`)
);

CREATE TABLE `access_field_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `client_id` bigint DEFAULT NULL,
  `entity` varchar(36) DEFAULT NULL,
  `field` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `access_field_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `account_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `employee_id` varchar(255) DEFAULT NULL,
  `requester_uuid` varchar(36) DEFAULT NULL,
  `requester_api_user_id` varchar(255) DEFAULT NULL,
  `ordered_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_type` varchar(64) NOT NULL,
  `order_type` varchar(64) NOT NULL,
  `activation_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_date` datetime DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `message` text,
  `modified_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `person_notified` tinyint DEFAULT '0',
  `requester_notified` tinyint DEFAULT '0',
  `requested_user_id` varchar(255) DEFAULT NULL,
  `linked_user_id` varchar(255) DEFAULT NULL,
  `actual_user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `organisations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `short_name` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `short_name` (`short_name`)
);

CREATE TABLE `organisations_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `short_name` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `organisations_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits` (
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) NOT NULL,
  `master_id` varchar(255) NOT NULL,
  `deleted` tinyint DEFAULT '0',
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_changed` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `parent_uuid` varchar(36) DEFAULT NULL,
  `shortname` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `cvr` bigint DEFAULT NULL,
  `ean` bigint DEFAULT NULL,
  `senr` bigint DEFAULT NULL,
  `pnr` bigint DEFAULT NULL,
  `cost_bearer` varchar(255) DEFAULT NULL,
  `org_type` varchar(255) DEFAULT NULL,
  `org_type_id` bigint DEFAULT NULL,
  `local_extensions` text,
  `key_words` text,
  `opening_hours` text,
  `notes` text,
  `orgunit_type_id` bigint DEFAULT NULL,
  `belongs_to` bigint NOT NULL,
  `inherit_kle` tinyint DEFAULT '0',
  PRIMARY KEY (`uuid`),
  KEY `parent_uuid` (`parent_uuid`),
  KEY `fk_belongs_to` (`belongs_to`),
  CONSTRAINT `fk_belongs_to` FOREIGN KEY (`belongs_to`) REFERENCES `organisations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_ibfk_1` FOREIGN KEY (`parent_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_aud` (
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `deleted` tinyint DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  `last_changed` timestamp NULL DEFAULT NULL,
  `parent_uuid` varchar(36) DEFAULT NULL,
  `shortname` varchar(64) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `cvr` bigint DEFAULT NULL,
  `ean` bigint DEFAULT NULL,
  `senr` bigint DEFAULT NULL,
  `pnr` bigint DEFAULT NULL,
  `cost_bearer` varchar(255) DEFAULT NULL,
  `org_type` varchar(255) DEFAULT NULL,
  `org_type_id` bigint DEFAULT NULL,
  `local_extensions` text,
  `key_words` text,
  `opening_hours` text,
  `notes` text,
  `orgunit_type_id` bigint DEFAULT NULL,
  `belongs_to` bigint DEFAULT NULL,
  `inherit_kle` tinyint DEFAULT NULL,
  PRIMARY KEY (`uuid`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prime` tinyint NOT NULL DEFAULT '0',
  `street` varchar(255) NOT NULL,
  `localname` varchar(255) DEFAULT NULL,
  `postal_code` varchar(8) NOT NULL,
  `city` varchar(255) NOT NULL,
  `country` varchar(255) NOT NULL,
  `address_protected` tinyint NOT NULL,
  `master` varchar(64) NOT NULL DEFAULT '',
  `master_id` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
);

CREATE TABLE `posts_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `prime` tinyint DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `localname` varchar(255) DEFAULT NULL,
  `postal_code` varchar(8) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `address_protected` tinyint DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `posts_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `persons_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_date` date NOT NULL,
  `stop_date` date DEFAULT NULL,
  `reason` varchar(64) NOT NULL,
  `reason_text` text,
  `disable_account_orders` tinyint NOT NULL DEFAULT '0',
  `expire_accounts` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
);

CREATE TABLE `persons_leave_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `stop_date` date DEFAULT NULL,
  `reason` varchar(64) DEFAULT NULL,
  `reason_text` text,
  `disable_account_orders` tinyint DEFAULT NULL,
  `expire_accounts` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `persons_leave_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `persons` (
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) NOT NULL,
  `deleted` tinyint DEFAULT '0',
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_changed` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `firstname` varchar(255) NOT NULL,
  `surname` varchar(255) NOT NULL,
  `cpr` varchar(10) NOT NULL,
  `chosen_name` varchar(255) DEFAULT NULL,
  `first_employment_date` date DEFAULT NULL,
  `anniversary_date` date DEFAULT NULL,
  `local_extensions` text,
  `registered_post_address_id` bigint DEFAULT NULL,
  `residence_post_address_id` bigint DEFAULT NULL,
  `key_words` text,
  `notes` text,
  `taxed_phone` tinyint NOT NULL DEFAULT '0',
  `disable_account_orders` tinyint NOT NULL DEFAULT '0',
  `force_stop` tinyint NOT NULL DEFAULT '0',
  `leave_id` bigint DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `cpr` (`cpr`),
  KEY `registered_post_address_id` (`registered_post_address_id`),
  KEY `residence_post_address_id` (`residence_post_address_id`),
  KEY `fk_persons_leave` (`leave_id`),
  CONSTRAINT `fk_persons_leave` FOREIGN KEY (`leave_id`) REFERENCES `persons_leave` (`id`),
  CONSTRAINT `persons_ibfk_1` FOREIGN KEY (`registered_post_address_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `persons_ibfk_2` FOREIGN KEY (`residence_post_address_id`) REFERENCES `posts` (`id`)
);

CREATE TABLE `persons_aud` (
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) DEFAULT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `deleted` tinyint DEFAULT NULL,
  `created` timestamp NULL DEFAULT NULL,
  `last_changed` timestamp NULL DEFAULT NULL,
  `firstname` varchar(255) DEFAULT NULL,
  `surname` varchar(255) DEFAULT NULL,
  `cpr` varchar(10) DEFAULT NULL,
  `chosen_name` varchar(255) DEFAULT NULL,
  `first_employment_date` date DEFAULT NULL,
  `anniversary_date` date DEFAULT NULL,
  `local_extensions` text,
  `registered_post_address_id` bigint DEFAULT NULL,
  `residence_post_address_id` bigint DEFAULT NULL,
  `key_words` text,
  `notes` text,
  `taxed_phone` tinyint DEFAULT NULL,
  `disable_account_orders` tinyint DEFAULT NULL,
  `force_stop` tinyint DEFAULT NULL,
  `leave_id` bigint DEFAULT NULL,
  PRIMARY KEY (`uuid`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `persons_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `persons_children` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cpr` varchar(10) NOT NULL,
  `name` varchar(255) NOT NULL,
  `person_uuid` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  CONSTRAINT `persons_children_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`)
);

CREATE TABLE `persons_children_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `cpr` varchar(10) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `persons_children_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `affiliations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) NOT NULL,
  `master_id` varchar(255) NOT NULL,
  `start_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `stop_date` datetime DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  `orgunit_uuid` varchar(36) NOT NULL,
  `person_uuid` varchar(36) NOT NULL,
  `employee_id` varchar(255) DEFAULT NULL,
  `employment_terms` varchar(255) DEFAULT NULL,
  `employment_terms_text` varchar(255) DEFAULT NULL,
  `pay_grade` varchar(255) DEFAULT NULL,
  `working_hours_denominator` double(5,3) DEFAULT NULL,
  `working_hours_numerator` double(5,3) DEFAULT NULL,
  `affiliation_type` varchar(64) NOT NULL,
  `local_extensions` text,
  `position_id` varchar(255) DEFAULT NULL,
  `position_name` varchar(255) NOT NULL,
  `prime` tinyint NOT NULL DEFAULT '0',
  `position_type_id` varchar(255) DEFAULT NULL,
  `position_type_name` varchar(255) DEFAULT NULL,
  `inherit_privileges` tinyint NOT NULL DEFAULT '1',
  `selected_prime` tinyint NOT NULL DEFAULT '0',
  `wage_step` varchar(255) DEFAULT NULL,
  `vendor` varchar(255) DEFAULT NULL,
  `internal_reference` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `person_uuid` (`person_uuid`),
  CONSTRAINT `affiliations_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `affiliations_ibfk_2` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `affiliations_aud` (
  `id` bigint NOT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `stop_date` datetime DEFAULT NULL,
  `deleted` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  `employee_id` varchar(255) DEFAULT NULL,
  `employment_terms` varchar(255) DEFAULT NULL,
  `employment_terms_text` varchar(255) DEFAULT NULL,
  `pay_grade` varchar(255) DEFAULT NULL,
  `working_hours_denominator` double(5,3) DEFAULT NULL,
  `working_hours_numerator` double(5,3) DEFAULT NULL,
  `affiliation_type` varchar(64) DEFAULT NULL,
  `local_extensions` text,
  `position_id` varchar(255) DEFAULT NULL,
  `position_name` varchar(255) DEFAULT NULL,
  `prime` tinyint DEFAULT NULL,
  `position_type_id` varchar(255) DEFAULT NULL,
  `position_type_name` varchar(255) DEFAULT NULL,
  `inherit_privileges` tinyint DEFAULT NULL,
  `selected_prime` tinyint DEFAULT NULL,
  `wage_step` varchar(255) DEFAULT NULL,
  `vendor` varchar(255) DEFAULT NULL,
  `internal_reference` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `affiliations_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `affiliations_function` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `affiliation_id` bigint NOT NULL,
  `function` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `affiliation_id` (`affiliation_id`),
  CONSTRAINT `affiliations_function_ibfk_1` FOREIGN KEY (`affiliation_id`) REFERENCES `affiliations` (`id`) ON DELETE CASCADE
);

CREATE TABLE `affiliations_function_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `affiliation_id` bigint DEFAULT NULL,
  `function` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `affiliations_function_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `affiliations_kle_primary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `affiliation_id` bigint NOT NULL,
  `kle_value` varchar(8) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `affiliation_id` (`affiliation_id`),
  CONSTRAINT `affiliations_kle_primary_ibfk_1` FOREIGN KEY (`affiliation_id`) REFERENCES `affiliations` (`id`) ON DELETE CASCADE
);

CREATE TABLE `affiliations_kle_primary_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `affiliation_id` bigint DEFAULT NULL,
  `kle_value` varchar(8) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `affiliations_kle_primary_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `affiliations_kle_secondary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `affiliation_id` bigint NOT NULL,
  `kle_value` varchar(8) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `affiliation_id` (`affiliation_id`),
  CONSTRAINT `affiliations_kle_secondary_ibfk_1` FOREIGN KEY (`affiliation_id`) REFERENCES `affiliations` (`id`) ON DELETE CASCADE
);

CREATE TABLE `affiliations_kle_secondary_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `affiliation_id` bigint DEFAULT NULL,
  `kle_value` varchar(8) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `affiliations_kle_secondary_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `affiliations_manager` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `affiliation_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `affiliation_id` (`affiliation_id`),
  CONSTRAINT `affiliations_manager_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `affiliations_manager_ibfk_2` FOREIGN KEY (`affiliation_id`) REFERENCES `affiliations` (`id`) ON DELETE CASCADE
);

CREATE TABLE `affiliations_manager_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `affiliation_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `affiliations_manager_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user_id` varchar(128) NOT NULL,
  `username` varchar(128) NOT NULL,
  `entity_type` varchar(64) NOT NULL,
  `entity_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `bad_words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `client_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `name` varchar(64) DEFAULT NULL,
  `api_key` varchar(36) DEFAULT NULL,
  `access_role` varchar(36) DEFAULT NULL,
  `monitor_for_activity` tinyint DEFAULT NULL,
  `show_on_frontpage` tinyint DEFAULT NULL,
  `last_active` timestamp NULL DEFAULT NULL,
  `version` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `client_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `client_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `client_name` varchar(64) NOT NULL,
  `configuration` mediumblob NOT NULL,
  `last_changed` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `comment` text NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `email_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_type` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `email_template_children` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email_template_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` mediumtext NOT NULL,
  `enabled` tinyint NOT NULL,
  `minutes_delay` bigint NOT NULL DEFAULT '0',
  `recipients` text,
  `recipients_cc` text,
  `recipients_bcc` text,
  PRIMARY KEY (`id`),
  KEY `email_template_id` (`email_template_id`),
  CONSTRAINT `email_template_children_ibfk_1` FOREIGN KEY (`email_template_id`) REFERENCES `email_templates` (`id`)
);

CREATE TABLE `email_templates_attachment_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` mediumblob NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `email_templates_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) NOT NULL,
  `file_id` bigint NOT NULL,
  `email_template_child_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `file_id` (`file_id`),
  KEY `email_template_child_id` (`email_template_child_id`),
  CONSTRAINT `email_templates_attachment_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `email_templates_attachment_file` (`id`),
  CONSTRAINT `email_templates_attachment_ibfk_3` FOREIGN KEY (`email_template_child_id`) REFERENCES `email_template_children` (`id`)
);

CREATE TABLE `email_queue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `message` mediumtext NOT NULL,
  `cpr` varchar(10) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `delivery_tts` timestamp NOT NULL,
  `email_template_child_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `email_template_child_id` (`email_template_child_id`),
  CONSTRAINT `email_queue_ibfk_1` FOREIGN KEY (`email_template_child_id`) REFERENCES `email_template_children` (`id`)
);

CREATE TABLE `emails` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prime` tinyint NOT NULL DEFAULT '0',
  `email` varchar(255) NOT NULL,
  `master` varchar(64) NOT NULL DEFAULT '',
  `master_id` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
);

CREATE TABLE `emails_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `prime` tinyint DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `emails_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `entity_change_queue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(64) NOT NULL,
  `entity_uuid` varchar(36) NOT NULL,
  `change_type` varchar(64) NOT NULL,
  `tts` timestamp NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `entity_change_queue_details` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_change_queue_id` bigint NOT NULL,
  `change_type` varchar(255) NOT NULL,
  `change_type_details` varchar(255) DEFAULT NULL,
  `old_value` varchar(255) DEFAULT NULL,
  `new_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `entity_change_queue_id` (`entity_change_queue_id`),
  CONSTRAINT `entity_change_queue_details_ibfk_1` FOREIGN KEY (`entity_change_queue_id`) REFERENCES `entity_change_queue` (`id`) ON DELETE CASCADE
);

CREATE TABLE `function_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `function_types_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `function_types_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `function_type_constraints` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `function_type_id` bigint NOT NULL,
  `phone_type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `function_type_id` (`function_type_id`),
  CONSTRAINT `function_type_constraints_ibfk_1` FOREIGN KEY (`function_type_id`) REFERENCES `function_types` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `function_type_constraints_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `function_type_id` bigint DEFAULT NULL,
  `phone_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `function_type_constraints_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `future_emails` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `all_or_none` tinyint NOT NULL,
  `delivery_tts` timestamp NOT NULL,
  `eboks` tinyint NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` mediumtext NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `future_emails_persons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `future_email_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  KEY `future_email_id` (`future_email_id`),
  CONSTRAINT `future_emails_persons_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `future_emails_persons_ibfk_2` FOREIGN KEY (`future_email_id`) REFERENCES `future_emails` (`id`) ON DELETE CASCADE
);

CREATE TABLE `kle` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(8) NOT NULL,
  `name` varchar(255) NOT NULL,
  `active` tinyint NOT NULL,
  `parent` varchar(8) NOT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
);

CREATE TABLE `known_usernames` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_type` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `known_usernames_UNIQUE` (`user_type`,`username`),
  KEY `known_username_idx` (`user_type`)
);

CREATE TABLE `message_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `message` text NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `modification_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `uuid` varchar(36) NOT NULL,
  `entity` varchar(64) NOT NULL,
  `change_type` varchar(36) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notification_type` varchar(255) NOT NULL,
  `affected_entity_uuid` varchar(36) NOT NULL,
  `affected_entity_type` varchar(255) NOT NULL,
  `affected_entity_name` varchar(255) NOT NULL,
  `active` tinyint DEFAULT '0',
  `message` varchar(1000) DEFAULT NULL,
  `created` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_updated` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `admin_uuid` varchar(36) DEFAULT NULL,
  `admin_name` varchar(255) DEFAULT NULL,
  `event_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `orgunit_account_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  CONSTRAINT `orgunit_account_order_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunit_account_order_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_order_id` bigint NOT NULL,
  `user_type` varchar(255) NOT NULL,
  `rule` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `account_order_id` (`account_order_id`),
  CONSTRAINT `orgunit_account_order_type_ibfk_1` FOREIGN KEY (`account_order_id`) REFERENCES `orgunit_account_order` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunit_account_order_type_position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_order_type_id` bigint NOT NULL,
  `position_name` varchar(255) NOT NULL,
  `rule` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `account_order_type_id` (`account_order_type_id`),
  CONSTRAINT `orgunit_account_order_type_position_ibfk_1` FOREIGN KEY (`account_order_type_id`) REFERENCES `orgunit_account_order_type` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunit_change_queue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `orgunit_name` varchar(64) NOT NULL,
  `change_date` date NOT NULL,
  `change_type` varchar(255) NOT NULL,
  `attribute_field` varchar(255) DEFAULT NULL,
  `attribute_value` varchar(255) DEFAULT NULL,
  `create_payload` text,
  `parent_uuid` varchar(36) DEFAULT NULL,
  `parent_name` varchar(64) DEFAULT NULL,
  `applied_status` varchar(255) NOT NULL,
  `applied_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `orgunit_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `changed_timestamp` datetime DEFAULT NULL,
  `sent_timestamp` datetime DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `change_type` varchar(64) NOT NULL,
  `old_value` varchar(255) DEFAULT NULL,
  `new_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  CONSTRAINT `orgunit_changes_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunit_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_key` varchar(64) NOT NULL,
  `type_value` varchar(255) NOT NULL,
  `active` tinyint DEFAULT '1',
  `ext_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `orgunit_types_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `type_key` varchar(64) DEFAULT NULL,
  `type_value` varchar(255) DEFAULT NULL,
  `active` tinyint DEFAULT NULL,
  `ext_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunit_types_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_emails` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `email_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `email_id` (`email_id`),
  CONSTRAINT `orgunits_emails_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_emails_ibfk_2` FOREIGN KEY (`email_id`) REFERENCES `emails` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_emails_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `email_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_emails_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_kle_primary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `kle_value` varchar(8) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  CONSTRAINT `orgunits_kle_primary_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_kle_primary_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `kle_value` varchar(8) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_kle_primary_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_kle_secondary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `kle_value` varchar(8) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  CONSTRAINT `orgunits_kle_secondary_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_kle_secondary_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `kle_value` varchar(8) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_kle_secondary_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_kle_tertiary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `kle_value` varchar(8) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  CONSTRAINT `orgunits_kle_tertiary_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_kle_tertiary_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `kle_value` varchar(8) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_kle_tertiary_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_manager` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `manager_uuid` varchar(36) NOT NULL,
  `inherited` tinyint NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `manager_uuid` (`manager_uuid`),
  CONSTRAINT `orgunits_manager_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_manager_ibfk_2` FOREIGN KEY (`manager_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_manager_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `manager_uuid` varchar(36) DEFAULT NULL,
  `inherited` tinyint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_manager_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `phones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prime` tinyint NOT NULL DEFAULT '0',
  `type_prime` tinyint DEFAULT NULL,
  `phone_number` varchar(128) DEFAULT NULL,
  `phone_type` varchar(32) NOT NULL,
  `master` varchar(64) NOT NULL DEFAULT '',
  `master_id` varchar(255) NOT NULL DEFAULT '',
  `notes` text,
  `visibility` varchar(255) DEFAULT NULL,
  `function_type_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `function_type_id` (`function_type_id`),
  CONSTRAINT `phones_ibfk_1` FOREIGN KEY (`function_type_id`) REFERENCES `function_types` (`id`) ON DELETE SET NULL
);

CREATE TABLE `phones_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `prime` tinyint DEFAULT NULL,
  `type_prime` tinyint DEFAULT NULL,
  `phone_number` varchar(128) DEFAULT NULL,
  `phone_type` varchar(32) DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  `notes` text,
  `visibility` varchar(255) DEFAULT NULL,
  `function_type_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `phones_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_phones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `phone_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `phone_id` (`phone_id`),
  CONSTRAINT `orgunits_phones_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_phones_ibfk_2` FOREIGN KEY (`phone_id`) REFERENCES `phones` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_phones_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `phone_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_phones_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `orgunits_posts_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_posts_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_posts_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `post_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_posts_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `value` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `custom_value_enabled` bit(1) NOT NULL DEFAULT b'0',
  `custom_value_unique` bit(1) NOT NULL DEFAULT b'0',
  `custom_value_name` varchar(255) DEFAULT NULL,
  `custom_value_regex` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_value` (`value`)
);

CREATE TABLE `tags_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `description` text,
  `custom_value_enabled` bit(1) DEFAULT NULL,
  `custom_value_unique` bit(1) DEFAULT NULL,
  `custom_value_name` varchar(255) DEFAULT NULL,
  `custom_value_regex` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `tags_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `orgunits_tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orgunit_uuid` varchar(36) NOT NULL,
  `tag_id` bigint NOT NULL,
  `custom_value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `orgunit_uuid` (`orgunit_uuid`),
  KEY `tag_id` (`tag_id`),
  CONSTRAINT `orgunits_tags_ibfk_1` FOREIGN KEY (`orgunit_uuid`) REFERENCES `orgunits` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `orgunits_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
);

CREATE TABLE `orgunits_tags_aud` (
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `tag_id` bigint DEFAULT NULL,
  `custom_value` varchar(255) DEFAULT NULL,
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `orgunits_tags_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `master` varchar(64) NOT NULL,
  `master_id` varchar(255) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `local_extensions` text,
  `user_type` varchar(36) NOT NULL,
  `prime` tinyint NOT NULL DEFAULT '0',
  `employee_id` varchar(255) DEFAULT NULL,
  `password_expire_date` date NOT NULL DEFAULT '9999-12-31',
  `disabled` tinyint DEFAULT '0',
  `account_expire_date` date NOT NULL DEFAULT '9999-12-31',
  `password_locked` bit(1) DEFAULT b'0',
  `password_locked_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `users_aud` (
  `id` bigint NOT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `local_extensions` text,
  `user_type` varchar(36) DEFAULT NULL,
  `prime` tinyint DEFAULT NULL,
  `employee_id` varchar(255) DEFAULT NULL,
  `password_expire_date` date DEFAULT NULL,
  `disabled` tinyint DEFAULT NULL,
  `account_expire_date` date DEFAULT NULL,
  `password_locked` bit(1) DEFAULT NULL,
  `password_locked_date` date DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `users_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `persons_phones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `phone_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  KEY `phone_id` (`phone_id`),
  CONSTRAINT `persons_phones_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `persons_phones_ibfk_2` FOREIGN KEY (`phone_id`) REFERENCES `phones` (`id`) ON DELETE CASCADE
);

CREATE TABLE `persons_phones_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  `phone_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `persons_phones_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `persons_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `persons_users_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `persons_users_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `persons_users_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `persons_users_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `photos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `last_changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data` mediumblob NOT NULL,
  `checksum` bigint NOT NULL,
  `format` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_photos_persons` (`person_uuid`),
  CONSTRAINT `fk_photos_persons` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `reserved_usernames` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `employee_id` varchar(255) DEFAULT NULL,
  `user_type` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `tts` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  CONSTRAINT `reserved_usernames_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`)
);

CREATE TABLE `security_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `client_id` varchar(128) NOT NULL,
  `clientname` varchar(128) NOT NULL,
  `method` varchar(32) NOT NULL,
  `request` text NOT NULL,
  `ip_address` varchar(32) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `setting_key` varchar(64) NOT NULL,
  `setting_value` text NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `sms_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message` text NOT NULL,
  `timestamp` timestamp NOT NULL,
  `user_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `sms_log_recipients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sms_log_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sms_log_id` (`sms_log_id`),
  CONSTRAINT `sms_log_recipients_ibfk_1` FOREIGN KEY (`sms_log_id`) REFERENCES `sms_log` (`id`)
);

CREATE TABLE `sofd_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person_uuid` varchar(36) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `password` varchar(60) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `person_uuid` (`person_uuid`),
  CONSTRAINT `sofd_account_ibfk_1` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE CASCADE
);

CREATE TABLE `supported_user_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `can_order` tinyint DEFAULT '0',
  `days_to_deactivate` bigint NOT NULL DEFAULT '0',
  `days_to_delete` bigint NOT NULL DEFAULT '0',
  `unique_key` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `days_before_to_create` bigint NOT NULL DEFAULT '0',
  `depends_on` bigint DEFAULT NULL,
  `minutes_delay` bigint NOT NULL DEFAULT '0',
  `username_prefix` varchar(64) NOT NULL DEFAULT 'NONE',
  `username_prefix_value` varchar(64) DEFAULT '',
  `username_infix` varchar(64) NOT NULL DEFAULT 'RANDOM',
  `username_infix_value` varchar(64) DEFAULT '5',
  `username_suffix` varchar(64) NOT NULL DEFAULT 'NONE',
  `username_suffix_value` varchar(64) DEFAULT '',
  `single_user_mode` tinyint DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `sut_fk` (`depends_on`),
  CONSTRAINT `supported_user_types_ibfk_1` FOREIGN KEY (`depends_on`) REFERENCES `supported_user_types` (`id`) ON DELETE SET NULL
);

CREATE TABLE `telephony_phones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `master` varchar(64) NOT NULL,
  `master_id` varchar(255) NOT NULL,
  `phone_number` varchar(128) DEFAULT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  `account_number` varchar(128) DEFAULT NULL,
  `ean` bigint DEFAULT NULL,
  `phone_type` varchar(32) NOT NULL,
  `visibility` varchar(255) NOT NULL,
  `function_type_id` bigint DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  `person_name` varchar(255) DEFAULT NULL,
  `last_changed` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `subscription_type` varchar(255) DEFAULT NULL,
  `notes` text,
  PRIMARY KEY (`id`),
  KEY `function_type_id` (`function_type_id`),
  KEY `person_uuid` (`person_uuid`),
  CONSTRAINT `telephony_phones_ibfk_1` FOREIGN KEY (`function_type_id`) REFERENCES `function_types` (`id`) ON DELETE SET NULL,
  CONSTRAINT `telephony_phones_ibfk_2` FOREIGN KEY (`person_uuid`) REFERENCES `persons` (`uuid`) ON DELETE SET NULL
);

CREATE TABLE `telephony_phones_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `master` varchar(64) DEFAULT NULL,
  `master_id` varchar(255) DEFAULT NULL,
  `phone_number` varchar(128) DEFAULT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  `account_number` varchar(128) DEFAULT NULL,
  `ean` bigint DEFAULT NULL,
  `phone_type` varchar(32) DEFAULT NULL,
  `visibility` varchar(255) DEFAULT NULL,
  `function_type_id` bigint DEFAULT NULL,
  `person_uuid` varchar(36) DEFAULT NULL,
  `person_name` varchar(255) DEFAULT NULL,
  `last_changed` timestamp NULL DEFAULT NULL,
  `subscription_type` varchar(255) DEFAULT NULL,
  `notes` text,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `telephony_phones_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);

CREATE TABLE `telephony_phones_orgunits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `telephony_phones_id` bigint NOT NULL,
  `orgunit_uuid` varchar(36) NOT NULL,
  `orgunit_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `telephony_phones_id` (`telephony_phones_id`),
  CONSTRAINT `telephony_phones_orgunits_ibfk_1` FOREIGN KEY (`telephony_phones_id`) REFERENCES `telephony_phones` (`id`)
);

CREATE TABLE `telephony_phones_orgunits_aud` (
  `id` bigint NOT NULL,
  `rev` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `telephony_phones_id` bigint DEFAULT NULL,
  `orgunit_uuid` varchar(36) DEFAULT NULL,
  `orgunit_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`rev`),
  KEY `rev` (`rev`),
  CONSTRAINT `telephony_phones_orgunits_aud_ibfk_1` FOREIGN KEY (`rev`) REFERENCES `revisions` (`id`)
);