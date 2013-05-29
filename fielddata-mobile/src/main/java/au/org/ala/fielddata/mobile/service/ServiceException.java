package au.org.ala.fielddata.mobile.service;

/**
 * Thrown if there is an error accessing a remote service.
 */
public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = -869286629599049967L;

	public ServiceException(Exception e) {
		super(e);
	}
	
	public ServiceException(String message) {
		super(message);
	}
}
