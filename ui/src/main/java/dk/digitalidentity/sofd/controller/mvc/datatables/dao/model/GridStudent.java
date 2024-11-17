package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_students")
public class GridStudent {

	@Id
	@Column
	private long id;
	
	@Column
	private String userId;

	@Column
	private String name;

	@Column
	private boolean disabled;

	@Column
	private String institutionNumbers;
}

