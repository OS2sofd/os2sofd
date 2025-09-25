package dk.digitalidentity.sofd.dao.model.projection;

public interface ProfessionLookup {
	long getAffiliationId();
	String getPositionName();
	String getPayGrade();
	Long getProfessionId();
	long getOrganisationId();
}
