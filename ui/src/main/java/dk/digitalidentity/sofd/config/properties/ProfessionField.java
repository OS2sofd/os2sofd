package dk.digitalidentity.sofd.config.properties;

public enum ProfessionField {
	POSITION_NAME("positionName"),
	PAY_GRADE("payGrade");
	
	private String name;
	
	private ProfessionField(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
