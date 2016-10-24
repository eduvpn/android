package net.tuxed.vpnconfigimporter.utils;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        assertEquals("0.00 KB", FormattingUtils.formatBytesTraffic(_context, 0L));
        assertEquals("0.50 KB", FormattingUtils.formatBytesTraffic(_context, 512L));
        assertEquals("1.00 KB", FormattingUtils.formatBytesTraffic(_context, 1024L));
        assertEquals("1.00 MB", FormattingUtils.formatBytesTraffic(_context, 1024*1024L));
        assertEquals("5.00 MB", FormattingUtils.formatBytesTraffic(_context, 5*1024*1024L));
        assertEquals("1.00 GB", FormattingUtils.formatBytesTraffic(_context, 1024*1024*1024L));
        assertEquals("5.00 GB", FormattingUtils.formatBytesTraffic(_context, 5*1024*1024*1024L));
        assertEquals("900.00 GB", FormattingUtils.formatBytesTraffic(_context, 900*1024*1024*1024L));
    }
}
