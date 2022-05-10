package server;

import org.apache.commons.codec.digest.DigestUtils;

public class PasswordsHashGetter {
    public static String getHash(String pass){
        return DigestUtils.md5Hex(pass);
    }
}
