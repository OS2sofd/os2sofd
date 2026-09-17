package dk.digitalidentity.sofd.controller.rest;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.AccountOrderViewDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AccountOrderView;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.dto.AccountOrderGridDTO;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadOrManagerAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;

@RequireControllerWriteAccess
@RestController
public class AccountOrderRestController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private PersonService personService;

	@Autowired
	private AccountOrderViewDatatableDao accountOrderViewDatatableDao;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private MessageSource messageSource;

	// matches the SUBSTRING(activation_timestamp, 1, 16) in view_datatables_accountorders
	private static final DateTimeFormatter ACTIVATION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	record DeleteOrder(long orderId) {}

	// the report itself is readable by anyone with read- or manager-access, so the datatable feeding it must be too
	@RequireReadOrManagerAccess
	@PostMapping("/rest/accountorder/list")
	public DataTablesOutput<AccountOrderGridDTO> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult, Locale locale) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<AccountOrderGridDTO> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		// the dropdown-filtered columns are pulled out of the input and handled below, as the built-in
		// column filter does a "like %value%", and e.g. ACTIVE_DIRECTORY would then also match
		// ACTIVE_DIRECTORY_SCHOOL
		AccountOrderType orderType = parseEnum(AccountOrderType.class, takeColumnSearch(input, "orderType"));
		AccountOrderStatus status = parseEnum(AccountOrderStatus.class, takeColumnSearch(input, "status"));
		String userType = takeColumnSearch(input, "userType");

		// the activation column is filtered by a window rather than free text, as searching for an exact
		// date is rarely what anyone wants. Teardown orders are scheduled months ahead and would
		// otherwise sit on top of everything that actually happened. It caps the future end only, so
		// history stays put
		String activationCutoff = activationCutoff(takeColumnSearch(input, "activationTimestamp"));

		// everything the user picked in the filter row goes in the additional specification, which counts
		// towards "recordsFiltered" only. The window belongs here with the other three rather than in the
		// pre-filter, even though pre-filtering would make the count query cheaper. Sitting in the
		// pre-filter it silently changed the "ud af" total, so the same filter row had one control that
		// behaved unlike its neighbours and nothing on screen explained why
		Specification<AccountOrderView> userFilters = (root, _, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (orderType != null) {
				predicates.add(criteriaBuilder.equal(root.get("orderType"), orderType));
			}

			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			if (StringUtils.hasLength(userType)) {
				predicates.add(criteriaBuilder.equal(root.get("userType"), userType));
			}

			if (activationCutoff != null) {
				// the view stores the timestamp as a zero-padded yyyy-MM-dd HH:mm string, so comparing
				// it as text cuts chronologically
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activationTimestamp"), activationCutoff));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};

		// orders awaiting approval have their own report and are not part of this one at all, so this
		// goes in the pre-filtering specification, which counts towards "recordsTotal" as well
		Specification<AccountOrderView> preFilter = (root, _, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status"), AccountOrderStatus.PENDING_APPROVAL);

		DataTablesOutput<AccountOrderView> output = accountOrderViewDatatableDao.findAll(input, userFilters, preFilter);

		List<AccountOrderGridDTO> dtos = output.getData().stream()
				.map(o -> new AccountOrderGridDTO(o, supportedUserTypeService.getPrettyName(o.getUserType()), messageSource, locale))
				.collect(Collectors.toList());

		DataTablesOutput<AccountOrderGridDTO> result = new DataTablesOutput<>();
		result.setData(dtos);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}

	// reads the search-value for a given column and clears it, so the datatables library does not also filter on it
	private static String takeColumnSearch(DataTablesInput input, String columnName) {
		for (Column column : input.getColumns()) {
			if (columnName.equals(column.getData())) {
				String value = column.getSearch().getValue();
				column.getSearch().setValue("");

				return value;
			}
		}

		return null;
	}

	/**
	 * The last activation timestamp the report should include, formatted the way the view stores it,
	 * or null for no limit.
	 *
	 * The window arrives as an ISO-8601 period such as P2W or P1M, so "1 måned frem" is a calendar
	 * month from today rather than an approximated 30 days. It runs to the end of that day rather
	 * than to this exact time, so the last day is included in full.
	 */
	private static String activationCutoff(String window) {
		if (!StringUtils.hasLength(window)) {
			return null;
		}

		try {
			return LocalDate.now().plus(Period.parse(window)).atTime(23, 59).format(ACTIVATION_FORMAT);
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
		if (!StringUtils.hasLength(value)) {
			return null;
		}

		try {
			return Enum.valueOf(enumType, value);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	@PostMapping("/rest/accountorder/delete")
	public ResponseEntity<?> delete(@RequestBody DeleteOrder deleteOrder) {
		AccountOrder order = accountOrderService.findById(deleteOrder.orderId);
		if (order != null) {
			accountOrderService.delete(order);
		} else {
			return ResponseEntity.notFound().build();
		}
		
		String message = "Kontoordre slettet: Handling: " + order.getOrderType() + " Type: " + order.getUserType() + " Status: " + order.getStatus() + ".";
		auditLogger.log(order.getPersonUuid(), EntityType.ACCOUNT_ORDER, EventType.DELETE, PersonService.getName(personService.getByUuid(order.getPersonUuid())), message);

		return ResponseEntity.ok().build();
	}

}
