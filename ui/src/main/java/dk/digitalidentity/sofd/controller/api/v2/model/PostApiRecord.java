package dk.digitalidentity.sofd.controller.api.v2.model;

import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PostApiRecord extends BaseRecord {
	
	// primary key
	
	@NotNull
	private String master;

	@NotNull
	private String masterId;

	// read/write fields
	
	@NotNull
	private String street;
		
	@NotNull
	private String postalCode;
	
	@NotNull
	private String city;
	
	private String localname;
	private String country;
	private boolean addressProtected;
	
	// read-only
	private boolean prime;

	public PostApiRecord(Post post) {
		this.master = post.getMaster();
		this.masterId = post.getMasterId();
		this.street = post.getStreet();
		this.localname = post.getLocalname();
		this.postalCode = post.getPostalCode();
		this.city = post.getCity();
		this.country = post.getCountry();
		this.addressProtected = post.isAddressProtected();
		this.prime = post.isPrime();
	}

	public Post toPost() {
		Post post = new Post();
		post.setAddressProtected(addressProtected);
		post.setCity(city);
		post.setCountry(country);
		post.setLocalname(localname);
		post.setMaster(master);
		post.setMasterId(masterId);
		post.setPostalCode(postalCode);
		post.setStreet(street);
		
		return post;
	}
}
