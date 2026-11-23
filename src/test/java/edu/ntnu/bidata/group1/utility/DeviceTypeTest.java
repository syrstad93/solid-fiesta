package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class DeviceTypeTest {
    

    /**
    * Checks if "light" is considered a valid device type.
    */

    @Test
    void validKindReturnsTrue() {
        assertTrue(DeviceType.isValidKind("Light"));
    }

    /**
    * Checks if null is rejected as a valid device.
    */

    @Test 
    void nullKindReturnsFalse() {
        assertFalse(DeviceType.isValidKind(null));
    }

    /**
    * Checks if an unsupported string is rejected.
    */

    @Test
    void validKindReturnsFalse() {
        assertFalse(DeviceType.isValidKind("INVALID"));
    }

}
