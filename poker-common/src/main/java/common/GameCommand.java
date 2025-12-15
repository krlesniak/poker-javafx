package common;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a single command or game event, parsed from a text message.
 */
public class GameCommand {
    /**
     * private final command type defined by enum class CommandType
     */
    private final CommandType type;
    /**
     * private final string array that contains of parameters that appears after command name
     * ( for example BET 100 -> ['100'] )
     */
    private final String[] parameters;

    /**
     * A constructor that creates GameCommand object
     * @param type of the command
     * @param parameters a string array that contains of parameters that appears after command name
     */
    public GameCommand(CommandType type, String[] parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    /**
     * getter
     * @return type of the command
     */
    public CommandType getType() {return type;}

    /**
     * getter
     * @return parameters a string array of params after the command name
     */
    public String[] getParameters() {return parameters;}

    /**
     * A method takes a parameter at the specified index and converts it to an integer (int).
     * Used for commands with single numeric values (for example BET 100)
     * @param idx of the parameter (0 for the fist one)
     * @return parameter value as integer (int)
     */
    // get parameter as integer for command types with integers after (for example : BET 100)
    public int getIntParam(int idx) {
        if (idx >= parameters.length) throw new IllegalArgumentException("Missing parameter at index " + idx);
        try {
            return Integer.parseInt(parameters[idx]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter at index " + idx + " is not a number");
        }
    }

    /**
     * A method takes all the parameters and converts them to integers (int).
     * Used for commands with multiple numeric values (for example DRAW 0 1 4)
     * @return result a list of int numbers (for example indexes of cards)
     */
    // list of integers parameters for : DRAW 1 3 4
    public List<Integer> getIntListParams() {
        List<Integer> result = new ArrayList<>();
        for (String p : parameters) {
            try {
                result.add(Integer.parseInt(p));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Draw command parameter is not a number: " + p);
            }
        }
        return result;
    }

    /**
     * A method that combines all parameters into one string, separated by spaces
     * @return Concatenated string
     */
    // all parameters as one message
    public String getMessageAllParams() {
        return String.join(" ", parameters);
    }
}
