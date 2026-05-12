package dk.digitalidentity.sofd.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import dk.digitalidentity.sofd.dao.model.AuthorizationCode;

// TODO: temporary new version of AuthorizationCodeService, that scrapes a CSV output
@Service
public class AutorisationLookupService {
    private static final String BASE_URL = "https://autregweb.stps.dk";
    private static final String CSV_PATH = "/api/export/csv";
    private static final DateTimeFormatter DK_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String CSV_DELIMITER = ";";

    // column indexes in the CSV (0-based), based on the header returned by the service
    private static final int COL_AUTORISATIONS_ID = 0;
    private static final int COL_AUTORISATIONS_NAME = 6;
    private static final int COL_AUTORISATION_GYLDIG = 7;
    private static final String VALID_VALUE = "Ja";

    private final RestClient restClient;

    public AutorisationLookupService() {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * Looks up a healthcare professional by name and birthday and returns the
     * Autorisations-IDs of all rows where "Autorisation gyldig" == "Ja".
     *
     * @param healthcareName full name, e.g. "John Doe"
     * @param birthday       date of birth
     * @return list of valid Autorisations-IDs, possibly empty, never null
     */
    public List<AuthorizationCode> findValidAutorisationsIds(String healthcareName, LocalDate birthday) {
        String csv = fetchCsv(healthcareName, birthday);

        return extractValidIds(csv);
    }

    /** JSON request payload for the CSV export endpoint. */
    private record CsvExportRequest(
        String categoryTitle,
        String culture,
        String requestQuery,
        String Language
    ) {}

    private String fetchCsv(String healthcareName, LocalDate birthday) {
        String dateStr = birthday.format(DK_DATE);

        // the remote service expects the name with '+' for spaces (form-encoded style).
        String encodedName = UriUtils.encode(healthcareName, StandardCharsets.UTF_8).replace("%20", "+");

        String requestQuery = "?birthDateFrom=%s&birthDateTo=%s&healthcareName=%s".formatted(dateStr, dateStr, encodedName);

        CsvExportRequest payload = new CsvExportRequest(
            "Sundhedsperson",
            "da",
            requestQuery,
            "da"
        );

        String body = restClient.post()
                .uri(CSV_PATH)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        return body == null ? "" : body;
    }

    private List<AuthorizationCode> extractValidIds(String csv) {
        List<AuthorizationCode> result = new ArrayList<>();
        if (csv.isBlank()) {
            return result;
        }

        String[] lines = csv.split("\\R");  // any line break
        // lines[0] is the header — skip it
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
            	continue;
            }

            // -1 keeps trailing empty fields (the sample row ends with several empties)
            String[] cols = line.split(CSV_DELIMITER, -1);
            if (cols.length <= COL_AUTORISATION_GYLDIG) {
            	continue;
            }

            if (VALID_VALUE.equalsIgnoreCase(cols[COL_AUTORISATION_GYLDIG].trim())) {
                String id = cols[COL_AUTORISATIONS_ID].trim();
                String name = cols[COL_AUTORISATIONS_NAME].trim();
                if (!id.isEmpty()) {
                	AuthorizationCode code = new AuthorizationCode();
                	code.setCode(id);
                	code.setName(name);

                    result.add(code);
                }
            }
        }

        return result;
    }
}
