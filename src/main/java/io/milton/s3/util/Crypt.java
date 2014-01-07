/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.s3.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;

public class Crypt {

    /**
     * Calculates the directory hash of the given string of contents
     * 
     * @param content
     * @return
     */
    public static String toHexFromText(String text) {
        if (text == null)
            return null;

        return toHexFromByte(text.getBytes());
    }

    /**
     * Calculates the directory hash of the given bytes of contents
     * 
     * @param content
     * @return
     */
    public static String toHexFromByte(byte[] bytes) {
        if (bytes == null)
            return null;

        MessageDigest crypto = Crypt.getCrypt();
        crypto.update(bytes);
        return Crypt.toHex(crypto);
    }
    
    public static MessageDigest getCrypt() {
        MessageDigest crypto;
        try {
        	crypto = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        return crypto;
    }

    public static String toHex(MessageDigest crypto) {
        String hash = DigestUtils.shaHex(crypto.digest());
        return hash;
    }
}
