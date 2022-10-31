package dk.digitalidentity.sofd.controller.mvc.dto.history;

import dk.digitalidentity.sofd.dao.model.Post;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryPost {
	private String master;
	private String masterId;
	private String street;
	private String localname;
	private String postalCode;
	private String city;
	private String country;
	private boolean addressProtected;
	
	public HistoryPost(Post post) {
		this.master = post.getMaster();
		this.masterId = post.getMasterId();
		this.street = post.getStreet();
		this.localname = post.getLocalname();
		this.postalCode = post.getPostalCode();
		this.city = post.getCity();
		this.country = post.getCountry();
		this.addressProtected = post.isAddressProtected();
	}
}
