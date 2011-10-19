package updater.util;

import updater.TestSuite;
import java.io.File;
import java.security.spec.RSAPublicKeySpec;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class UtilTest {

    public UtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of humanReadableTimeCount method, of class Util.
     */
    @Test
    public void testHumanReadableTimeCount() {
        System.out.println("humanReadableTimeCount");
        // 2 yrs, 3 mths, 4 days, 5h 10m 45s
        int time = (31536000 * 2) + (2592000 * 3) + (86400 * 4) + (3600 * 5) + (60 * 10) + 45;
        assertEquals("2 yrs", Util.humanReadableTimeCount(time, 1));
        assertEquals("2 yrs, 3 mths", Util.humanReadableTimeCount(time, 2));
        assertEquals("2 yrs, 3 mths, 4 days", Util.humanReadableTimeCount(time, 3));
        assertEquals("2 yrs, 3 mths, 4 days, 5h", Util.humanReadableTimeCount(time, 4));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m", Util.humanReadableTimeCount(time, 5));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m 45s", Util.humanReadableTimeCount(time, 6));
        assertEquals("2 yrs, 3 mths, 4 days, 5h 10m 45s", Util.humanReadableTimeCount(time, 7));
        // 1 yr, 1 mth, 1 day, 1h 1m 59s
        time = (31536000 * 1) + (2592000 * 1) + (86400 * 1) + (3600 * 1) + (60 * 1) + 59;
        assertEquals("1 yr", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 yr, 1 mth", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 yr, 1 mth, 1 day", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 yr, 1 mth, 1 day, 1h", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m 59s", Util.humanReadableTimeCount(time, 6));
        assertEquals("1 yr, 1 mth, 1 day, 1h 1m 59s", Util.humanReadableTimeCount(time, 7));
        // 1 yr, 0 mth, 0 day, 0h 0m 1s
        time = (31536000 * 1) + (2592000 * 0) + (86400 * 0) + (3600 * 0) + (60 * 0) + 1;
        assertEquals("1 yr", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 yr, 0 mth", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 yr, 0 mth, 0 day", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 yr, 0 mth, 0 day, 0h", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 6));
        assertEquals("1 yr, 0 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 7));
        // 1 mth, 0 day, 0h 0m 1s
        time = (2592000 * 1) + (86400 * 0) + (3600 * 0) + (60 * 0) + 1;
        assertEquals("1 mth", Util.humanReadableTimeCount(time, 1));
        assertEquals("1 mth, 0 day", Util.humanReadableTimeCount(time, 2));
        assertEquals("1 mth, 0 day, 0h", Util.humanReadableTimeCount(time, 3));
        assertEquals("1 mth, 0 day, 0h 0m", Util.humanReadableTimeCount(time, 4));
        assertEquals("1 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 5));
        assertEquals("1 mth, 0 day, 0h 0m 1s", Util.humanReadableTimeCount(time, 6));
        // 3 days, 3h 0m 1s
        time = (86400 * 3) + (3600 * 3) + (60 * 0) + 1;
        assertEquals("3 days", Util.humanReadableTimeCount(time, 1));
        assertEquals("3 days, 3h", Util.humanReadableTimeCount(time, 2));
        assertEquals("3 days, 3h 0m", Util.humanReadableTimeCount(time, 3));
        assertEquals("3 days, 3h 0m 1s", Util.humanReadableTimeCount(time, 4));
        assertEquals("3 days, 3h 0m 1s", Util.humanReadableTimeCount(time, 5));
        // 3h 3m 1s
        time = (3600 * 3) + (60 * 3) + 1;
        assertEquals("3h", Util.humanReadableTimeCount(time, 1));
        assertEquals("3h 3m", Util.humanReadableTimeCount(time, 2));
        assertEquals("3h 3m 1s", Util.humanReadableTimeCount(time, 3));
        assertEquals("3h 3m 1s", Util.humanReadableTimeCount(time, 4));
        // 59s
        time = (60 * 0) + 59;
        assertEquals("59s", Util.humanReadableTimeCount(time, 1));
        assertEquals("59s", Util.humanReadableTimeCount(time, 2));
    }

    /**
     * Test of rsaEncrypt & rsaDecrypt method, of class Util.
     */
    @Test
    public void testRsaEnDecrypt() {
        System.out.println("testRsaEnDecrypt");
        try {
            String modulesString = "0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f";
            String publicExponentString = "010001";
            String privateExponentString = "45fa8429d4494b161bbb21a7bfd29a7d1ccfa4b74c852a0d2175b7572e86f85a9b28f79a6d55ca625a7a53ba1b456bc3feec65264d1d7cdcc069299f9a95461ccf1dd38d7767abef8c25da835bd3da07f5da67ed517ab5d779987a33bf397849e58627b011bac0ec227392278413515ecbd9ea8c7cc1843780a1c296998698769825cd7ac298f5a468af873e2e30eb94cf867086742d0b8d1fd9ab7efc7ce3f07a855fe280e8714c963c8436a20fbaf81f874a6714da4699a75cb5c7e2fa0546038f8a8134661a25ce30ff37d73bd94dee33e7bdc6425729e2fd71bdb938a2f5cd7caf56eca8f7ccb8ea320b20610ffeae7f5c8380da62dca4d7964ded34b731";

            BigInteger mod = new BigInteger(modulesString, 16);

            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(mod, new BigInteger(privateExponentString, 16));
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod, new BigInteger(publicExponentString, 16));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            File testFile = new File(TestSuite.pathToTestPackage + UtilTest.class.getPackage().getName().replace('.', '/') + "/UtilTest/" + "UtilTest_rsaEnDecrypt.ico");
            byte[] testData = Util.readFile(testFile);
            assertTrue(testData != null && testData.length > 0);

            // encrypt
            int blockSize = mod.bitLength() / 8;
            byte[] encrypted = Util.rsaEncrypt(privateKey, blockSize, blockSize - 11, testData);
            assertTrue(encrypted != null);
            assertEquals(1280, encrypted.length);

            // decrypt
            byte[] decrypted = Util.rsaDecrypt(publicKey, blockSize, encrypted);
            assertTrue(decrypted != null);
            assertEquals(1150, decrypted.length);

            for (int i = 0, iEnd = testData.length; i < iEnd; i++) {
                assertEquals(testData[i], decrypted[i]);
            }
        } catch (Exception ex) {
            fail("Exception caught in testRsaEnDecrypt");
            Logger.getLogger(UtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
