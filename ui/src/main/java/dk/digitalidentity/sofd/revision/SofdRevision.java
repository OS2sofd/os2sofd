package dk.digitalidentity.sofd.revision;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "revisions")
@Getter
@Setter
@EqualsAndHashCode
@RevisionEntity(SofdRevisionListener.class)
public class SofdRevision implements Serializable {
	private static final long serialVersionUID = 9113880695800122023L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@RevisionNumber
	private int id;

	@Column
	private String auditorId;

	@Column
	private String auditorName;

	@Column
	@RevisionTimestamp
	private long timestamp;

	@Transient
	public Date getRevisionDate() {
		return new Date( timestamp );
	}
}
