package group.aim.framework.basemodel.exception;

/**
 * Created by Nattapongr on 9/28/2016 AD.
 */

public class UnsupportedClassException extends RuntimeException {

    String message;

    public UnsupportedClassException() {
        this.message = super.getMessage();
    }

    public UnsupportedClassException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
