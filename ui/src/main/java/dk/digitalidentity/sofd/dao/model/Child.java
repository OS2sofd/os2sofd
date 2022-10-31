package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "persons_children")
@Getter
@Setter
@Audited
public class Child {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @Size(min = 10, max = 10)
    @NotNull
    private String cpr;

    @Column
    @NotNull
    @Size(max = 255)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_uuid")
    @JsonBackReference
    private Person parent;

}
