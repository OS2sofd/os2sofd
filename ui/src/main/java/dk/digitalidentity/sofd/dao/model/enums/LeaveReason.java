package dk.digitalidentity.sofd.dao.model.enums;

public enum LeaveReason {
	ORLOV("html.enum.leavereason.orlov"),
	SKOLEOPHOLD("html.enum.leavereason.skoleophold"),
	INAKTIV("html.enum.leavereason.inaktiv"),
	FRITSTILLET("html.enum.leavereason.fritstillet"),
	ANDET("html.enum.leavereason.andet");

	private String message;

	private LeaveReason(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
