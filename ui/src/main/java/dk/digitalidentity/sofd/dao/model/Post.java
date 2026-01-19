package dk.digitalidentity.sofd.dao.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.ReadOnlyProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@BatchSize(size = 100)
@EqualsAndHashCode(exclude = { "id", "masterId", "prime" }, callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Post extends MasteredEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@NotNull
	private String master;
	
	@Column
	@NotNull
	private String masterId;

	@Column
	@NotNull
	@Size(max = 255)
	private String street;

	@Column
	@Size(max = 255)
	private String localname;

	@Column
	@NotNull
	private String postalCode;

	@Column
	@NotNull
	@Size(max = 255)
	private String city;

	@Column
	@NotNull
	@Size(max = 255)
	private String country;

	@Column
	@NotNull
	private boolean addressProtected;

	@ReadOnlyProperty
	@Column
	@NotNull
	private boolean prime;

	@Column
	@NotNull
	private boolean returnAddress;

    public String getAsOneLine() {
		String result = street + ", " + postalCode + " " + city;
		if (country != null && !country.trim().isEmpty()) {
			result += ", " + country;
		}

		return result;
    }
}