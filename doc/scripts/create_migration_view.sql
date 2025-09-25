DELIMITER $$

create or replace view rollekatalog_roles as
select ou_uuid, count(*) as roleCount FROM os2rollekatalog_kommune.ou_roles group by ou_uuid;

create or replace view rollekatalog_users as
select ou_uuid, count(*) as userCount FROM os2rollekatalog_kommune.positions group by ou_uuid;

create or replace view rollekatalog_kles as
select ou_uuid, count(*) as kleCount FROM os2rollekatalog_kommune.ou_kles group by ou_uuid;

create or replace view rollekatalog_ous as
select
uuid
,parent_uuid
,name
,ifnull(kles.kleCount,0) as kle_count
,ifnull(roles.roleCount,0) as role_count
,ifnull(users.userCount,0) as user_count
from os2rollekatalog_kommune.ous
left join rollekatalog_roles as roles ON roles.ou_uuid = ous.uuid
left join rollekatalog_users as users ON users.ou_uuid = ous.uuid
left join rollekatalog_kles as kles ON kles.ou_uuid = ous.uuid
where ous.active = 1;

$$
DELIMITER ;
