package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "photos")
@Getter
@Setter
public class Photo {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @NotNull
    private String personUuid;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date lastChanged;

    @Column
    @NotNull
    private byte[] data;

    @Column
    @NotNull
    private long checksum;

    @Column
    @NotNull
    private String format;

}