package updater;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import updater.script.Catalog;
import updater.script.Catalog.Update;
import updater.script.InvalidFormatException;
import updater.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareUpdaterTest {

    public SoftwareUpdaterTest() {
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
     * Test of getPatches method, of class SoftwareUpdater.
     */
    @Test
    public void testGetPatches_Catalog_String() {
        System.out.println("testGetPatches_Catalog_String");

        byte[] catalogData = Util.readFile(new File(TestSuite.pathToTestPackage + getClass().getPackage().getName().replace('.', '/') + "/SoftwareUpdaterTest/SoftwareUpdaterTest_getPatches.xml"));
        assertTrue(catalogData != null && catalogData.length != 0);

        Catalog catalog = null;
        try {
            catalog = Catalog.read(catalogData);
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareUpdaterTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Failed to read test file.");
        }


        List<Update> result = SoftwareUpdater.getPatches(catalog, "1.0.0");
        assertEquals(4, result.size());

        int totalSize = 0;

        Update update = result.get(0);
        totalSize += update.getPatchLength();
        assertEquals("1.0.0", update.getVersionFrom());
        assertEquals("1.0.1", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getPatchLength();
        assertEquals("1.0.1", update.getVersionFrom());
        assertEquals("1.0.4", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getPatchLength();
        assertEquals("1.0.4", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(3);
        totalSize += update.getPatchLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(82 + 13 + 7 + 14, totalSize);


        result = SoftwareUpdater.getPatches(catalog, "1.0.2");
        assertEquals(3, result.size());

        totalSize = 0;

        update = result.get(0);
        totalSize += update.getPatchLength();
        assertEquals("1.0.2", update.getVersionFrom());
        assertEquals("1.0.3", update.getVersionTo());
        update = result.get(1);
        totalSize += update.getPatchLength();
        assertEquals("1.0.3", update.getVersionFrom());
        assertEquals("1.0.5", update.getVersionTo());
        update = result.get(2);
        totalSize += update.getPatchLength();
        assertEquals("1.0.5", update.getVersionFrom());
        assertEquals("1.0.6", update.getVersionTo());

        assertEquals(16 + 88 + 14, totalSize);
    }

    public static void _main(String[] args) {
        File patch1 = new File("1.patch");
        System.out.println(Util.getSHA256(patch1));
        System.out.println(patch1.length());
        File patch2 = new File("2.patch");
        System.out.println(Util.getSHA256(patch2));
        System.out.println(patch2.length());
    }
}
