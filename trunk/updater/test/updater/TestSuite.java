package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.RemoteContentTest.class,
    updater.SoftwareUpdaterTest.class,
    updater.util.DownloadProgessUtilTest.class,
    updater.util.UtilTest.class
})
public class TestSuite {

    public static final String pathToTestPackage = "test/";
    public static final String urlRoot = "http://localhost/SoftwareUpdaterTest/";
}
