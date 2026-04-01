package dk.digitalidentity.sofd.dao.model.projection;

public interface ProfessionLookup {
	long getAffiliationId();
	String getPositionName();
	String getPayGrade();
	String getPositionTypeName();
	Long getProfessionId();
	long getOrganisationId();
}
