package com.Jdbye.BukkitIRCd;

import java.security.*;

public class Hash {

    public static String compute(String input, HashType hashType) {
	MessageDigest md;
	try {
	    String hashTypeStr = "";
	    if (hashType == HashType.MD5) {
		hashTypeStr = "MD5";
	    } else if (hashType == HashType.SHA_1) {
		hashTypeStr = "SHA-1";
	    } else if (hashType == HashType.SHA_256) {
		hashTypeStr = "SHA-256";
	    } else if (hashType == HashType.SHA_384) {
		hashTypeStr = "SHA-384";
	    } else if (hashType == HashType.SHA_512) {
		hashTypeStr = "SHA-512";
	    }
	    md = MessageDigest.getInstance(hashTypeStr);

	    md.update(input.getBytes());
	    byte[] mb = md.digest();
	    String out = "";
	    for (int i = 0; i < mb.length; i++) {
		byte temp = mb[i];
		String s = Integer.toHexString(new Byte(temp));
		while (s.length() < 2) {
		    s = "0" + s;
		}
		s = s.substring(s.length() - 2);
		out += s;
	    }
	    return out;

	} catch (NoSuchAlgorithmException e) {
	    return "";
	}
    }
}
