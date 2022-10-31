package dk.digitalidentity.sofd.exception;

@SuppressWarnings("serial")
public class InsufficientAccessRightException extends RuntimeException {

	public InsufficientAccessRightException() {
		super("You do not have write access to this Resource.", null, false, false);
	}
}