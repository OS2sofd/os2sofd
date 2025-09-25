package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "orgunits_tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@EqualsAndHashCode(exclude = "id")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class OrgUnitTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String customValue;

	@OneToOne
	@JoinColumn(name = "orgunit_uuid")
	private OrgUnit orgUnit;

	@OneToOne
	@JoinColumn(name = "tag_id")
	private Tag tag;
}