package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_students")
public class GridStudent {

	@Id
	@Column
	private long id;

	@Column
	private String name;

	@Column
	private String username;

	@Column
	private String classes;

	@Column
	private boolean disabled;

	@Column
	private String institutionNumbers;
}

