/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app;

import nl.eduvpn.app.service.HistoryServiceTest;
import nl.eduvpn.app.service.PreferencesServiceTest;
import nl.eduvpn.app.service.SecurityServiceTest;
import nl.eduvpn.app.service.SerializerServiceTest;
import nl.eduvpn.app.utils.FormattingUtilsTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite used to run all unit tests.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({SerializerServiceTest.class, PreferencesServiceTest.class, HistoryServiceTest.class,
        SecurityServiceTest.class, FormattingUtilsTest.class})
public class UnitTestSuite {
    // Test suite used to run all unit tests at once.
    // To run the tests, right click on the class name, and select "Run 'UnitTestSuite'".
    // The tests will run on your device.
}
