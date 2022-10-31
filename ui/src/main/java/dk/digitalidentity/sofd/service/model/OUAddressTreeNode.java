package dk.digitalidentity.sofd.service.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUAddressTreeNode {
	private String id;
	private String text;
	private List<OUAddressTreeNode> children;
}
