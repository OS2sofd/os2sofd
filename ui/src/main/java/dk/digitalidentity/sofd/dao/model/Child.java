package dk.digitalidentity.sofd.dao.model;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
