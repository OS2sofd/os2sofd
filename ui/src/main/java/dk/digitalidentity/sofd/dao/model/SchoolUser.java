package dk.digitalidentity.sofd.dao.model;

public interface SchoolUser {
    long getId();
    String getUuid();
    String getUserId();
    boolean isDisabled();
    String getName();
    String getTitle();
    String getCpr();
    String getLocalExtensions();
}