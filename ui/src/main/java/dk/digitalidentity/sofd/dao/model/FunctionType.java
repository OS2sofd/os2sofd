package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dk.digitalidentity.sofd.dao.model.mapping.FunctionTypePhoneTypeMapping;
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
