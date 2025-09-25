-- initial creation to support fresh installations.
-- note: simple "create view if exists" queries will make galera log an error - hence the procedure workaround
delimiter $$
create or replace procedure before_migrate()
begin
	if not exists (select * FROM INFORMATION_SCHEMA.TABLES where table_schema = database() and table_name = 'view_adm_organisation') then
		create view view_adm_organisation as select null as id; -- dummy view, will get updated by repeating migration
	end if;
end $$
delimiter ;
call before_migrate();
drop procedure before_migrate;

