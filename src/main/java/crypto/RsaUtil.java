package crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RsaUtil {

    public static KeyPair generateKeyPair()
    {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Can't generate key pair", e);
        }
    }

    public static byte[] sign(byte[] data, byte[] key)
    {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            Signature rsa = Signature.getInstance("SHA1withRSA");
            rsa.initSign(kf.generatePrivate(spec));
            rsa.update(data);

            return rsa.sign();
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IllegalStateException("Can't sign data", e);
        }
    }

    public static boolean verify(byte[] data, byte[] key)
    {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(kf.generatePublic(spec));
            sig.update(data);

            return sig.verify(data);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            throw new IllegalStateException("Can't verify data", e);
        }
    }
}
