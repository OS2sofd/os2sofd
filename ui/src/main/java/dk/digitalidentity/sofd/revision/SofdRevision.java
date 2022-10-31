package dk.digitalidentity.sofd.revision;

import javax.persistence.Entity;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "revisions")
@Data
@EqualsAndHashCode(callSuper = true)
@RevisionEntity(SofdRevisionListener.class)
public class SofdRevision extends DefaultRevisionEntity {
	private static final long serialVersionUID = 9113880695800122023L;

	private String auditorId;
	private String auditorName;
}
