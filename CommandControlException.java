/**Custom exception for handling smart home specific errors.*/
public class CommandControlException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     * @param message The detail message.
     */
    public CommandControlException(String message) {
        super(message);
    }
}