public class IncorrectMessageException extends Exception{

    public IncorrectMessageException(String errorMessage) {
        
        super(errorMessage + " See log for details");
    }
}
