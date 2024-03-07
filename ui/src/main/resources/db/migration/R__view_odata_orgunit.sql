create or replace view view_odata_orgunit
as
with recursive cte as
(
select
       o.uuid
       ,o.parent_uuid
       ,o.ean
from
       orgunits o
where
       o.parent_uuid is null
       and o.deleted = 0

union all

select
       o.uuid
       ,o.parent_uuid
       ,ifnull(o.ean, parent.ean) as ean
from
       orgunits o
inner join cte parent on parent.uuid = o.parent_uuid
where
       o.deleted = 0
)
select o.uuid             	as uuid,
       o.master           	as master,
       o.master_id        	as master_id,
       o.deleted          	as deleted,
       o.created          	as created,
       o.last_changed     	as last_changed,
       o.parent_uuid      	as parent_uuid,
       o.shortname        	as shortname,
       o.name             	as name,
       o.cvr              	as cvr,
       o.cvr_name         	as cvr_name,
       ifnull(o.ean,cte.ean)     as ean,
       o.senr             	as senr,
       o.pnr              	as pnr,
       o.cost_bearer      	as cost_bearer,
       o.org_type         	as org_type,
       o.org_type_id      	as org_type_id,
       o.local_extensions 	as local_extensions,
       o.key_words        	as key_words,
       o.opening_hours    	as opening_hours,
       o.notes            	as notes,
       o.display_name     	as display_name,
       o.source_name      	as source_name,
       o.email            	as email,
       o.id               	as id,
       case when o.ean is null and cte.ean is not null and length(cte.ean) > 0 then 1 else 0 end as ean_inherited
from cte
inner join orgunits o on o.uuid = cte.uuid
inner join view_adm_organisation on view_adm_organisation.id = o.belongs_to;