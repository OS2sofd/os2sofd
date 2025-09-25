package dk.digitalidentity.sofd.controller.mvc.dto;

import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
	private long id;
	private String master;
	private boolean addressProtected;
	private boolean returnAddress;
	private boolean prime;

	@Size(min = 1, max = 255, message = "Skal være mellem 1 og 255 tegn")
	private String street;
	
	@Size(max = 255, message = "Må højest være 255 tegn")
	private String localname;
	
	@Size(min = 1, max = 8, message = "Skal være mellem 1 og 8 tegn")
	private String postalCode;
	
	@Size(min = 1, max = 255, message = "Skal være mellem 1 og 255 tegn")
	private String city;
	
	@Size(min = 1, max = 255, message = "Skal være mellem 1 og 255 tegn")
	private String country;
}
