-- Changes to this view should be in R__view_orgunits_manager
create or replace view view_orgunits_manager
as
with recursive cte as
(
	select
		o.uuid,
		o.parent_uuid,
		ifnull(o.selected_manager_uuid,o.imported_manager_uuid) as manager_uuid,
		0 as inherited
	from orgunits o
	where
		o.parent_uuid is null
		and o.deleted = 0

	union all

	select
		o.uuid,
		o.parent_uuid,
		ifnull(ifnull(o.selected_manager_uuid,o.imported_manager_uuid),cte.manager_uuid) as manager_uuid,
		isnull(ifnull(o.selected_manager_uuid,o.imported_manager_uuid)) as inherited
	from orgunits o
	inner join cte on cte.uuid = o.parent_uuid
	where
		o.deleted = 0
)
select
	cte.uuid as orgunit_uuid,
	cte.manager_uuid,
	cte.inherited,
	ifnull(p.chosen_name,concat(p.firstname,' ',p.surname)) as name
from cte
left join persons p on p.uuid = cte.manager_uuid
