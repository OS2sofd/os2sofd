package dk.digitalidentity.sofd.dao.model.enums;

public enum AccountOrderDeactivateAndDeleteRule {
    KEEP_ALIVE("html.enum.account_order_deactivate_and_delete_rule.keep_alive"),
    DEACTIVATE_AND_DELETE("html.enum.account_order_deactivate_and_delete_rule.deactivate_and_delete"),
    DEACTIVATE_AND_DELETE_IF_HOURLY_PAID("html.enum.account_order_deactivate_and_delete_rule.deactivate_and_delete_if_hourly_paid");

    private String message;

    private AccountOrderDeactivateAndDeleteRule(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
