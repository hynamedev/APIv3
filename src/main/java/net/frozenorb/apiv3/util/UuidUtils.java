package net.frozenorb.apiv3.util;

import java.util.UUID;
import java.util.logging.Logger;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UuidUtils {
    
    public static boolean isAcceptableUuid(UUID uuid) {
        return uuid != null && uuid.version() == 4;
    }
    
    public static UUID parseUuid(String input) {
        if (input.length() == 36) {
            return UUID.fromString(input);
        } else if (input.length() == 32) {
            Logger.getGlobal().info("Got 32 length UUID");
            return from32(input);
        } else {
            throw new IllegalArgumentException("Invalid UUID string: " + input);
        }
    }
    
    private static UUID from32(String id) {
        long lo, hi;
        lo = hi = 0;
        
        for (int i = 0, j = 0; i < 32; ++j) {
            int curr;
            char c = id.charAt(i);
            
            if (c >= '0' && c <= '9') {
                curr = (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                curr = (c - 'a' + 10);
            } else if (c >= 'A' && c <= 'F') {
                curr = (c - 'A' + 10);
            } else {
                throw new NumberFormatException("Non-hex character at #" + i + ": '" + c + "' (value 0x" + Integer.toHexString(c) + ")");
            }
            curr = (curr << 4);
            
            c = id.charAt(++i);
            
            if (c >= '0' && c <= '9') {
                curr |= (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                curr |= (c - 'a' + 10);
            } else if (c >= 'A' && c <= 'F') {
                curr |= (c - 'A' + 10);
            } else {
                throw new NumberFormatException("Non-hex character at #" + i + ": '" + c + "' (value 0x" + Integer.toHexString(c) + ")");
            }

            if (j < 8) {
                hi = (hi << 8) | curr;
            } else {
                lo = (lo << 8) | curr;
            }
            ++i;
        }
        return new UUID(hi, lo);
    }
}