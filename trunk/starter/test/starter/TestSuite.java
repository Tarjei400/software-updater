package starter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    starter.SoftwareStarterTest.class,
    starter.patch.PatchLogTest.class
})
public class TestSuite {

    public static final String pathToTestPackage = "test/";
}
