package nl.eduvpn.app;

import nl.eduvpn.app.service.HistoryServiceTest;
import nl.eduvpn.app.service.PreferencesServiceTest;
import nl.eduvpn.app.service.SerializerServiceTest;
import nl.eduvpn.app.utils.FormattingUtilsTest;
import nl.eduvpn.app.utils.TTLCacheTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite used to run all unit tests.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ SerializerServiceTest.class, PreferencesServiceTest.class, HistoryServiceTest.class,
        FormattingUtilsTest.class, TTLCacheTest.class})
public class UnitTestSuite {
    // Test suite used to run all unit tests at once.
    // To run the tests, right click on the class name, and select "Run 'UnitTestSuite'".
    // The tests will run on your device.
}
