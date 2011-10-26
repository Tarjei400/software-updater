package updater.builder.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import javax.xml.transform.TransformerException;
import updater.script.InvalidFormatException;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class KeyGenerator {

    protected KeyGenerator() {
    }

    public static void generateRSA(int keySize, File saveTo) throws IOException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
            RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

            RSAKey rsaKey = new RSAKey(privateKeySpec.getModulus().toByteArray(), publicKeySpec.getPublicExponent().toByteArray(), privateKeySpec.getPrivateExponent().toByteArray());

            Util.writeFile(saveTo, rsaKey.output());
        } catch (InvalidKeySpecException ex) {
            System.err.println(ex);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        } catch (UnsupportedEncodingException ex) {
            System.err.println(ex);
        } catch (TransformerException ex) {
            System.err.println(ex);
        }
    }

    public static void generateAES(int keySize, File saveTo) throws IOException {
        Random random = new Random();

        byte[] key = new byte[keySize / 8];
        random.nextBytes(key);

        byte[] IV = new byte[16];
        random.nextBytes(IV);

        AESKey aesKey = new AESKey(key, IV);
        try {
            Util.writeFile(saveTo, aesKey.output());
        } catch (TransformerException ex) {
            System.err.println(ex);
        }
    }

    public static void renewAESIV(File file) throws IOException, InvalidFormatException {
        Random random = new Random();

        byte[] IV = new byte[16];
        random.nextBytes(IV);

        AESKey aesKey = AESKey.read(Util.readFile(file));
        aesKey.setIV(IV);

        try {
            Util.writeFile(file, aesKey.output());
        } catch (TransformerException ex) {
            System.err.println(ex);
        }
    }
}