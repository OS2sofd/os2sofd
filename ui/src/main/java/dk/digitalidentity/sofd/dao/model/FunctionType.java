package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dk.digitalidentity.sofd.dao.model.mapping.FunctionTypePhoneTypeMapping;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "function_types")
@Getter
@Setter
@Audited
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler" }) // need this because we sometimes detach the object from Hibernate
public class FunctionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @NotNull
    private String name;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "functionType")
    private List<FunctionTypePhoneTypeMapping> phoneTypes;
}
