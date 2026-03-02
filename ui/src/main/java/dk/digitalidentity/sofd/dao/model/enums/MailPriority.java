package dk.digitalidentity.sofd.dao.model.enums;

public enum MailPriority {
    NORMAL("html.enum.MailPriority.normal"),
    HIGH("html.enum.MailPriority.high");

    private String message;

    private MailPriority(String message) { this.message = message; }

    public String getMessage() {
        return message;
    }

}
