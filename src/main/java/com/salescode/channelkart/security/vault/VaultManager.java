package com.salescode.channelkart.security.vault;

import com.salescode.channelkart.exceptions.SystemRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaultManager {

	private static final Logger log = LoggerFactory.getLogger(VaultManager.class);

	private static String secretKey = Optional.ofNullable(System.getenv("secretKey")).orElse("channelkart");
	private static String salt =  Optional.ofNullable(System.getenv("salt")).orElse("channelkart");
	private static byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private static IvParameterSpec ivspec = new IvParameterSpec(iv);
	private static SecretKeySpec secretKeySpec =null;
	static{
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");
		} catch (Exception e) {
			log.error("Could not initialize the vault manager" ,e);
		}
	}

	public static String encrypt(String strToEncrypt) {
		try {
			Cipher cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

			return Base64.getEncoder().encodeToString(cipherEncrypt.doFinal(strToEncrypt.getBytes(
					StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new SystemRuntimeException(e.getMessage());
		}
	}

	public static String decrypt(String strToDecrypt) {
	    try{
				Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5PADDING");
				cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
	        return new String(cipherDecrypt.doFinal(Base64.getDecoder().decode(strToDecrypt.getBytes(
							StandardCharsets.UTF_8))));
	    }
	    catch (Exception e) {
	        throw new SystemRuntimeException(e.getMessage());
	    }
	}

	public static void main(String[] args) {
		ExecutorService es= Executors.newFixedThreadPool(100);
		for(int i=0;i<1000;i++) {
			es.submit(()->{
				log.info(decrypt(encrypt("test")));
				String decrypt = decrypt(
						"QftuCEi0sUahTYuUIOoBkKaUUQZbdl/0TDviv6/re8UeTxzxqnBqD6WZfUD0b05Mh+wkTpSkgHxobRhqqy2yfRiJlSjhn1xIlxRGdTyEauB3J6FBNNACFw+1n2XAlpt7hwKtm7hhk01jeMNuzdwCM9Nxp4t3ZQHH528rIBaZxtdCkuwjl2deQqvIixmxxkEI9BygVDMfn7nZrzjUaE//llwY8dPauMnvZEc0DNXkFsZ1wX/3gAtm5tn9JyL5xBPoows7gRM4wGlrzNDko0fbhQ==");
				log.info(decrypt);
			});

		}
	}

}
