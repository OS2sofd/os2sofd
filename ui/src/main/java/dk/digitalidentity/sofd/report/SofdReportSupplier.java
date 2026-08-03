package dk.digitalidentity.sofd.report;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import dk.digitalidentity.reporter.suppliers.ReporterSupplier;
import dk.digitalidentity.sofd.service.VersionService;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SofdReportSupplier implements ReporterSupplier {
	private final VersionService versionService;

	@Override
	public Supplier<String> getCustomerNameSupplier() {
		// defaults to folder name
		return null;
	}

	@Override
	public Supplier<String> getProductNameSupplier() {
		return (() -> "OS2sofd");
	}

	@Override
	public Supplier<String> getVersionSupplier() {
		return (() -> versionService.getVersion());
	}

	@Override
	public Supplier<Map<String, String>> getAttributeMapSupplier() {
		return null;
	}
}
