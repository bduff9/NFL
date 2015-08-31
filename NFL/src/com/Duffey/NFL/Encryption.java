package com.Duffey.NFL;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.*;

public class Encryption {

	public static String encrypt(String plainData) throws Exception {
		Key key = generateKey();
		Cipher c = Cipher.getInstance(NFLConst.ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encVal = c.doFinal(plainData.getBytes());
		String encryptedValue = new BASE64Encoder().encode(encVal);
		return encryptedValue;
	}

	public static String decrypt(String encryptedData) throws Exception {
		Key key = generateKey();
		Cipher c = Cipher.getInstance(NFLConst.ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
		byte[] decValue = c.doFinal(decordedValue);
		String decryptedValue = new String(decValue);
		return decryptedValue;
	}

	private static Key generateKey() throws Exception {
		Key key = new SecretKeySpec(NFLConst.SECRET_KEY, NFLConst.ALGO);
		return key;
	}

	public static void main(String[] args) throws Exception {
		String test = "{ \"mrcuser\":\"duffey\", \"mrcpswd\":\"somepass\" }";
		System.out.println("Original value is " + test);
		String encrypted = encrypt(test);
		System.out.println("Encrypted value is " + encrypted);
		String decrypted = decrypt("Qyg4BTnxM+OPF689ni6Gk6UXnA2YMeT4JFnYDzHfz+e4zAnEpD3ly/nXWBDoGIMk");
		System.out.println("Decrypted value is " + decrypted);
	}

}