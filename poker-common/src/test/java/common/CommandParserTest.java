package common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    @Test
    void testParseCommandNoParams() {
        // no params after command
        String input = "FOLD";
        GameCommand cmd = CommandParser.parse(input);

        assertEquals(CommandType.FOLD, cmd.getType());
        assertEquals(0, cmd.getParameters().length);
    }

    @Test
    void testParseCommandWithParams() {
        // one param after command
        String input = "BET 100";
        GameCommand cmd = CommandParser.parse(input);

        assertEquals(CommandType.BET, cmd.getType());
        assertEquals(1, cmd.getParameters().length);
        assertEquals("100", cmd.getParameters()[0]);
    }

    @Test
    void testParseCommandWithMoreThanOneParameters() {
        // many params after command
        String input = "DRAW 0 1 4";
        GameCommand cmd = CommandParser.parse(input);

        assertEquals(CommandType.DRAW, cmd.getType());
        assertEquals(3, cmd.getParameters().length);
        assertEquals("0", cmd.getParameters()[0]);
        assertEquals("4", cmd.getParameters()[2]);
    }

    @Test
    void testParseIgnoreCase() {
        // bet =?= BET
        String input = "bet 50";
        GameCommand cmd = CommandParser.parse(input);

        assertEquals(CommandType.BET, cmd.getType());
        assertEquals("50", cmd.getParameters()[0]);
    }

    @Test
    void testParseUnknownCommand() {
        // invalid command
        String input = "INVALID 123";
        GameCommand cmd = CommandParser.parse(input);

        // should return ERR and UNKNOWN_CMD
        assertEquals(CommandType.ERR, cmd.getType());
        assertTrue(cmd.getParameters()[0].contains("UNKNOWN_CMD"));
        assertTrue(cmd.getParameters()[1].contains("INVALID"));
    }

    @Test
    void testParseEmptyOrNull() {
        // click enter
        GameCommand cmdEmpty = CommandParser.parse("");
        assertEquals(CommandType.ERR, cmdEmpty.getType());

        GameCommand cmdNull = CommandParser.parse(null);
        assertEquals(CommandType.ERR, cmdNull.getType());
    }

    @Test
    void testParseWithExtraSpaces() {
        // it should still work with extra spaces
        String input = "JOIN    game1   John";
        GameCommand cmd = CommandParser.parse(input);

        assertEquals(CommandType.JOIN, cmd.getType());
        assertEquals(2, cmd.getParameters().length);
        assertEquals("game1", cmd.getParameters()[0]);
        assertEquals("John", cmd.getParameters()[1]);
    }

    // test for createMessage
    @Test
    void testCreateMessageWithNoParameters() {
        // Err command without params
        String message = CommandParser.createMessage(CommandType.ERR);
        assertEquals("ERR\n", message);
    }

    @Test
    void testCreateMessageWithOneParameter() {
        //  WELCOME cmd with one param
        String message = CommandParser.createMessage(CommandType.WELCOME, "game42");
        assertEquals("WELCOME game42\n", message);
    }

    @Test
    void testCreateMessageWithMultipleParameters() {
        // LOBBY cmd with many params
        String message = CommandParser.createMessage(CommandType.LOBBY, "playerA", "playerB", "playerC");
        assertEquals("LOBBY playerA playerB playerC\n", message);
    }

    @Test
    void testCreateMessageWithNumericParameters() {
        // TURN cmd with numeric and string params
        String message = CommandParser.createMessage(CommandType.TURN, "playerB", "BET1", "50");
        assertEquals("TURN playerB BET1 50\n", message);
    }

    @Test
    void testCreateMessageForAction() {
        // ACTION cmd
        String message = CommandParser.createMessage(CommandType.ACTION, "playerX", "BET", "100");
        assertEquals("ACTION playerX BET 100\n", message);
    }

}