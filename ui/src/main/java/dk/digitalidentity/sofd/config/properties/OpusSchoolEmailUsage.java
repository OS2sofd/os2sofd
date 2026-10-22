package dk.digitalidentity.sofd.config.properties;

// controls whether (and how) "Skole Email" accounts are used as the source of the email address written to OPUS infotype 105
public enum OpusSchoolEmailUsage {
	NONE,      // only Exchange accounts are used
	FALLBACK,  // Exchange is used if present, otherwise Skole Email
	PREFERRED  // Skole Email is used if present, otherwise Exchange
}
