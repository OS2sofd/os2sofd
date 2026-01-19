package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.*;

import dk.digitalidentity.sofd.dao.model.enums.TagType;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@EqualsAndHashCode(exclude = "id")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String value;

	@Column
	private String description;

	@Column
	private boolean customValueEnabled;

	@Column
	private boolean customValueUnique;

	@Column
	private String customValueName;

	@Column
	private String customValueRegex;

	@OneToMany(mappedBy = "tag")
	private List<OrgUnitTag> orgUnitTags;

	@Column
	@Enumerated(EnumType.STRING)
	private TagType tagType;

	@Column
	private String itSystemUuid;

}