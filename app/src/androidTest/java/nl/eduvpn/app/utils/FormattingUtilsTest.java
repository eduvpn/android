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

package nl.eduvpn.app.utils;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static org.junit.Assert.assertEquals;

/**
 * Tests some of the formatting utils methods.
 * Created by Daniel Zolnai on 2016-10-19.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FormattingUtilsTest {

    private Context _context;

    @Before
    public void before() {
        _context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testSecondsFormatting() {
        assertEquals("N/A", FormattingUtils.formatDurationSeconds(_context, null));
        assertEquals("00:00", FormattingUtils.formatDurationSeconds(_context, 0L));
        assertEquals("01:00", FormattingUtils.formatDurationSeconds(_context, 60L));
        assertEquals("59:59", FormattingUtils.formatDurationSeconds(_context, 3599L));
        assertEquals("01:00:00", FormattingUtils.formatDurationSeconds(_context, 3600L));
        assertEquals("111:00:00", FormattingUtils.formatDurationSeconds(_context, 399600L));
    }

    @Test
    public void testByteFormatting() {
        assertEquals("N/A", FormattingUtils.formatBytesTraffic(_context, null));
        assertEquals("0.00 kB", FormattingUtils.formatBytesTraffic(_context, 0L));
        assertEquals("0.50 kB", FormattingUtils.formatBytesTraffic(_context, 512L));
        assertEquals("1.00 kB", FormattingUtils.formatBytesTraffic(_context, 1024L));
        assertEquals("1.00 MB", FormattingUtils.formatBytesTraffic(_context, 1024*1024L));
        assertEquals("5.00 MB", FormattingUtils.formatBytesTraffic(_context, 5*1024*1024L));
        assertEquals("1.00 GB", FormattingUtils.formatBytesTraffic(_context, 1024*1024*1024L));
        assertEquals("5.00 GB", FormattingUtils.formatBytesTraffic(_context, 5*1024*1024*1024L));
        assertEquals("900.00 GB", FormattingUtils.formatBytesTraffic(_context, 900*1024*1024*1024L));
    }
}
