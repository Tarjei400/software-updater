package updater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.RemoteContent.GetCatalogResult;
import updater.RemoteContent.GetPatchListener;
import updater.RemoteContent.GetPatchResult;
import updater.RemoteContent.RSAPublicKey;
import updater.util.CommonUtil.ObjectReference;
import updater.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RemoteContentTest {

    public RemoteContentTest() {
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
     * Test of getCatalog method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetCatalog() {
        System.out.println("getCatalog");

        String modulesString = "0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f";
        String publicExponentString = "010001";
        String privateExponentString = "45fa8429d4494b161bbb21a7bfd29a7d1ccfa4b74c852a0d2175b7572e86f85a9b28f79a6d55ca625a7a53ba1b456bc3feec65264d1d7cdcc069299f9a95461ccf1dd38d7767abef8c25da835bd3da07f5da67ed517ab5d779987a33bf397849e58627b011bac0ec227392278413515ecbd9ea8c7cc1843780a1c296998698769825cd7ac298f5a468af873e2e30eb94cf867086742d0b8d1fd9ab7efc7ce3f07a855fe280e8714c963c8436a20fbaf81f874a6714da4699a75cb5c7e2fa0546038f8a8134661a25ce30ff37d73bd94dee33e7bdc6425729e2fd71bdb938a2f5cd7caf56eca8f7ccb8ea320b20610ffeae7f5c8380da62dca4d7964ded34b731";

        String xmlFileName = "RemoteContentTest_getCatalog.xml";
        String manipulatedXmlFileName = "RemoteContentTest_getCatalog_manipulated.xml";
        File originalFile = new File(TestSuite.pathToTestPackage + RemoteContentTest.class.getPackage().getName().replace('.', '/') + "/RemoteContentTest/" + xmlFileName);
        String originalFileString = null;
        try {
            originalFileString = new String(Util.readFile(originalFile), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RemoteContentTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue(originalFileString != null);

        //<editor-fold defaultstate="collapsed" desc="test normal request">
        System.out.println("getCatalog - test normal request");

        System.out.println(TestSuite.urlRoot + manipulatedXmlFileName);
        String url = TestSuite.urlRoot + manipulatedXmlFileName;
        long lastUpdateDate = 0L;
        RSAPublicKey key = new RSAPublicKey(new BigInteger(modulesString, 16), new BigInteger(publicExponentString, 16));
        GetCatalogResult result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertTrue(result != null);
        assertTrue(!result.isNotModified());
        assertTrue(result.getCatalog() != null);
        assertTrue(result.getCatalog().output().equals(originalFileString));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test If-Modified-Since header">
        System.out.println("getCatalog - test If-Modified-Since header");

        url = TestSuite.urlRoot + manipulatedXmlFileName;
        lastUpdateDate = System.currentTimeMillis() - 2000;
        key = new RSAPublicKey(new BigInteger(modulesString, 16), new BigInteger(publicExponentString, 16));
        result = RemoteContent.getCatalog(url, lastUpdateDate, key);

        assertTrue(result != null);
        assertTrue(result.isNotModified());
        assertTrue(result.getCatalog() == null);
        //</editor-fold>
    }

    /**
     * Test of getPatch method, of class RemoteContent.
     * This test depends on some functions in /updater/util/Util.java.
     */
    @Test
    public void testGetPatch() {
        System.out.println("getPatch");

        String originalFileName = "RemoteContentTest_getPatch_original.png";
        String partFileName = "RemoteContentTest_getPatch_part.png";
        // the file is fully downloaded and some downloaded bytes are incorrect
        String fullBrokenFileName = "RemoteContentTest_getPatch_full_broken.png";
        // the file is partly downloaded and some downloaded bytes are incorrect
        String partBrokenFileName = "RemoteContentTest_getPatch_part_broken.png";
        String largerFileName = "RemoteContentTest_getPatch_larger.png";

        String FilePathPrefix = TestSuite.pathToTestPackage + getClass().getPackage().getName().replace('.', '/') + "/RemoteContentTest/";
        File originalFile = new File(FilePathPrefix + originalFileName);
        File partFile = new File(FilePathPrefix + partFileName);
        File fullBrokenFile = new File(FilePathPrefix + fullBrokenFileName);
        File partBrokenFile = new File(FilePathPrefix + partBrokenFileName);
        File largerFile = new File(FilePathPrefix + largerFileName);

        assertTrue(originalFile.exists());
        assertTrue(partFile.exists());
        assertTrue(fullBrokenFile.exists());
        assertTrue(partBrokenFile.exists());
        assertTrue(largerFile.exists());

        File tempFile = new File(originalFileName + ".kh6am");
        final ObjectReference<Long> startingPosition = new ObjectReference<Long>(0L);
        final ObjectReference<Integer> cumulativeByteDownloaded = new ObjectReference<Integer>(0);


        //<editor-fold defaultstate="collapsed" desc="test fresh download">
        System.out.println("getPatch - test fresh download");

        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        GetPatchListener listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        String url = TestSuite.urlRoot + originalFileName;
        File saveToFile = tempFile;
        String fileSHA1 = Util.getSHA256(originalFile);
        int expectedLength = (int) originalFile.length();

        GetPatchResult result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download">
        System.out.println("getPatch - test resume download");

        boolean copyResult = Util.copyFile(partFile, tempFile);
        assertEquals(true, copyResult);
        int initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test resume download but some downloaded bytes in the file are broken">
        System.out.println("getPatch - test resume download but some downloaded bytes in the file are broken");

        copyResult = Util.copyFile(partBrokenFile, tempFile);
        assertEquals(true, copyResult);
        initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        PrintStream ps = System.err;
        System.setErr(new PrintStream(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
            }
        }));
        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);
        System.setErr(ps);

        assertEquals(false, result.getResult());
        assertEquals(initFileSize, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertTrue(!Util.getSHA256(originalFile).equals(Util.getSHA256(saveToFile)));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded">
        System.out.println("getPatch - test download when file is fully downloaded");

        copyResult = Util.copyFile(originalFile, tempFile);
        assertEquals(true, copyResult);
        initFileSize = (int) tempFile.length();
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length() - initFileSize, (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when file is fully downloaded but some downloaded bytes in the file are broken">
        System.out.println("getPatch - test download when file is fully downloaded but some downloaded bytes in the file are broken");

        copyResult = Util.copyFile(fullBrokenFile, tempFile);
        assertEquals(true, copyResult);
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="test download when the downloaded file is larger">
        System.out.println("getPatch - test download when the downloaded file is larger");

        copyResult = Util.copyFile(largerFile, tempFile);
        assertEquals(true, copyResult);
        startingPosition.setObj(0L);
        cumulativeByteDownloaded.setObj(0);

        listener = new GetPatchListener() {

            @Override
            public boolean downloadInterrupted() {
                return true;
            }

            @Override
            public void byteStart(long pos) {
                startingPosition.setObj(pos);
            }

            @Override
            public void byteDownloaded(int numberOfBytes) {
                cumulativeByteDownloaded.setObj(cumulativeByteDownloaded.getObj() + numberOfBytes);
            }
        };
        url = TestSuite.urlRoot + originalFileName;
        saveToFile = tempFile;
        fileSHA1 = Util.getSHA256(originalFile);
        expectedLength = (int) originalFile.length();

        result = RemoteContent.getPatch(listener, url, saveToFile, fileSHA1, expectedLength);

        assertEquals(true, result.getResult());
        assertEquals(0, (long) startingPosition.getObj());
        assertEquals(originalFile.length(), (int) cumulativeByteDownloaded.getObj());
        assertEquals(originalFile.length(), saveToFile.length());
        assertEquals(Util.getSHA256(originalFile), Util.getSHA256(saveToFile));
        //</editor-fold>

        // test thread interrupt


        tempFile.delete();
    }

    public static boolean makeXMLForGetCatalogTest(File in, File out, BigInteger mod, BigInteger privateExp) {
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(mod, privateExp);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // compress
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            GZIPOutputStream gout = new GZIPOutputStream(bout);
            gout.write(Util.readFile(in));
            gout.finish();
            byte[] compressedData = bout.toByteArray();

            // encrypt
            int blockSize = mod.bitLength() / 8;
            byte[] encrypted = Util.rsaEncrypt(privateKey, blockSize, blockSize - 11, compressedData);

            // write to file
            Util.writeFile(out, encrypted);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String modulesString = "0080ac742891f8ba0d59dcc96b464e2245e53a9b29f8219aa0b683ad10007247ced6d74b7bef2a6b0555ec22735827b2b9dfe94664d492a723ad78d6d97d1c9b19ade1225edc060eaced684436ce221659c7e8320bc2bf5ddcdbe6751b0f476066437ccc50ea0e5afafb6a59581df509145d34aa4d0541f500f09868686f5681a509bf58feda73b35326f816b60205550783d628e5e61b24e37198349e416f09ef7579f6f25b5725d54df44017e256b1c7060f0c5ba5f3dd162e26fc5fbfcf4294ee261124737b1cdc3024dc2be62c8ebd89c8766bfaf3606a9e7aefa4fd41758498441fe69a967005c66df3ac0551d7b04910c6a9fa272aa6d081defbc2db174f";
        String publicExponentString = "010001";
        String privateExponentString = "45fa8429d4494b161bbb21a7bfd29a7d1ccfa4b74c852a0d2175b7572e86f85a9b28f79a6d55ca625a7a53ba1b456bc3feec65264d1d7cdcc069299f9a95461ccf1dd38d7767abef8c25da835bd3da07f5da67ed517ab5d779987a33bf397849e58627b011bac0ec227392278413515ecbd9ea8c7cc1843780a1c296998698769825cd7ac298f5a468af873e2e30eb94cf867086742d0b8d1fd9ab7efc7ce3f07a855fe280e8714c963c8436a20fbaf81f874a6714da4699a75cb5c7e2fa0546038f8a8134661a25ce30ff37d73bd94dee33e7bdc6425729e2fd71bdb938a2f5cd7caf56eca8f7ccb8ea320b20610ffeae7f5c8380da62dca4d7964ded34b731";

//        System.out.println(makeXMLForGetCatalogTest(new File("catalog.xml"), new File("catalog_manipulated.xml"), new BigInteger(modulesString, 16), new BigInteger(privateExponentString, 16)));
//        System.out.println(makeXMLForGetCatalogTest(new File(TestSuite.pathToTestPackage + RemoteContentTest.class.getPackage().getName() + "/RemoteContentTest/RemoteContentTest_getCatalog.xml"), new File("RemoteContentTest_getCatalog_manipulated.xml"), new BigInteger(modulesString, 16), new BigInteger(privateExponentString, 16)));
    }
}
