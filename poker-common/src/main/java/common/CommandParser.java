package common;

import java.util.Arrays;

/**
 *A class responsible for parsing raw text messages (String) received by the server
 * and converting them into a GameCommand object.
 */
public class CommandParser {
    /**
     * Prevents instantiation of this utility class.
     * All methods in CommandParser are static.
     */
    private CommandParser() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }
    /**
     * A static method that processes a raw message into a game command object.
     * @param cmd raw string received from the server.
     * @return GameCommand object or ERR if the command is unknown or null
     */
    public static GameCommand parse(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return new GameCommand(CommandType.ERR, new String[]{});
        }
        String[] parts = cmd.trim().split("\\s+"); // \\s+ for one or more integers after the command (DRAW 1, 3, 4)

        // first part is a command
        String commandStr = parts[0].toUpperCase();
        CommandType type;

        try {
            type = CommandType.valueOf(commandStr);
        } catch (IllegalArgumentException e) {
            // if command not valid
            return new GameCommand(CommandType.ERR, new String[]{"UNKNOWN_CMD", commandStr});
        }

        // the rest is parameters
        String[] params = Arrays.copyOfRange(parts, 1, parts.length);

        return new GameCommand(type, params);
    }

    /**
     * A static method that constructs and formats a text message (String) to be sent
     * to the client (Server -> Client), according to the protocol.
     * @param type of the command that will be sent
     * @param args optional list of parameters
     * @return formatted string ready to be sent
     */
    public static String createMessage(CommandType type, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        for (String arg : args) {
            sb.append(" ").append(arg);
        }
        sb.append("\n");
        return sb.toString();
    }
}
