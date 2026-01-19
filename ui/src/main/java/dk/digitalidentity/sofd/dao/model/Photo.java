package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
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