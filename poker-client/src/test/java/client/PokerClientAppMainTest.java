package client;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class PokerClientAppMainTest {

    @Test
    void testMainMethodExecutionFlow() {

        // Simulating command HELLO 1.0 and after EXIT
        String testSimulatedInput = "HELLO 1.0\nEXIT\n";
        InputStream originalIn = System.in;

        // try-with-resources
        try (ByteArrayInputStream simulatedIn = new ByteArrayInputStream(testSimulatedInput.getBytes())) {

            // replacing system.in
            System.setIn(simulatedIn);

            // Situation when we cannot connect to server -> there will not be an exception
            assertDoesNotThrow(() -> PokerClientAppMain.main(new String[]{}));

        } catch (IOException e) {
            // catch required with try-with-resources
            fail("Unexpected IOException occurred during test setup: " + e.getMessage());
        } finally {
            // getting back system.in not to destroy other tests
            System.setIn(originalIn);
        }
    }
}