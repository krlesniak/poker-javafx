package common;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GameCommandTest {

    @Test
    void testGetIntParam() {
        // cmd: BET 200
        GameCommand cmd = new GameCommand(CommandType.BET, new String[]{"200"});

        int value = cmd.getIntParam(0);
        assertEquals(200, value);
    }

    @Test
    void testGetIntParamInvalidNumber() {
        // cmd: BET fifty
        GameCommand cmd = new GameCommand(CommandType.BET, new String[]{"fifty"});

        // should throw an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            cmd.getIntParam(0);
        });
    }

    @Test
    void testGetIntParamIndexOutOfBounds() {
        // command with a parameter but do not receive any params
        // cmd : FOLD
        GameCommand cmd = new GameCommand(CommandType.FOLD, new String[]{});

        assertThrows(IllegalArgumentException.class, () -> {
            cmd.getIntParam(0); // can not access this param because
                                    // there is no param
        });
    }

    @Test
    void testGetIntListParams() {
        // cmd: DRAW 0 2 4
        GameCommand cmd = new GameCommand(CommandType.DRAW, new String[]{"0", "2", "4"});

        List<Integer> list = cmd.getIntListParams();

        assertEquals(3, list.size());
        assertEquals(0, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    void testGetMessageAllParams() {
        // cmd: ERR UNKNOWN_CMD ASD
        GameCommand cmd = new GameCommand(CommandType.ERR, new String[]{"UNKNOWN_CMD", "ASD"});
        String payload = cmd.getMessageAllParams();

        // checking if params are joined using space ' '
        assertEquals("UNKNOWN_CMD ASD", payload);
    }
}