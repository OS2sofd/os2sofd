alter table orgunits add column imported_manager_uuid varchar(36) NULL;
alter table orgunits_aud add column imported_manager_uuid varchar(36) NULL;

alter table orgunits add column selected_manager_uuid varchar(36) NULL;
alter table orgunits_aud add column selected_manager_uuid varchar(36) NULL;