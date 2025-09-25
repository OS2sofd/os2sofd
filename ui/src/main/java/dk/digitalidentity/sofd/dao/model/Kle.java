package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
public class Kle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = 8)
	private String code;

	@Column(nullable = false, length = 256)
	private String name;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false, length = 8)
	private String parent;

	@Column
	private String uuid;
}
