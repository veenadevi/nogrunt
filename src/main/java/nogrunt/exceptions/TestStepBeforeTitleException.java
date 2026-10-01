package nogrunt.exceptions;

public class TestStepBeforeTitleException extends RuntimeException{

	public TestStepBeforeTitleException() {
	        super();
	    }

	    public TestStepBeforeTitleException (String message) {
	        super(message);
	    }

	    public TestStepBeforeTitleException (String message, Throwable cause) {
	        super(message, cause);
	    }

	    public TestStepBeforeTitleException (Throwable cause) {
	        super(cause);
	    }

}
