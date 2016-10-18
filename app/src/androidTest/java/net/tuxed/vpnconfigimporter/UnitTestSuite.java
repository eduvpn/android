package net.tuxed.vpnconfigimporter;

import net.tuxed.vpnconfigimporter.service.PreferencesService;
import net.tuxed.vpnconfigimporter.service.PreferencesServiceTest;
import net.tuxed.vpnconfigimporter.service.SerializerServiceTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite used to run all unit tests.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({SerializerServiceTest.class, PreferencesServiceTest.class})
public class UnitTestSuite {
    // Test suite used to run all unit tests at once.
    // To run the tests, right click on the class name, and select "Run 'UnitTestSuite'".
    // The tests will run on your device.
}
