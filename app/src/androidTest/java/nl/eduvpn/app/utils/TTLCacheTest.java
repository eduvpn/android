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

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static org.junit.Assert.*;

/**
 * Test which tests the TTL cache.
 * Created by Daniel Zolnai on 2016-10-20.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TTLCacheTest {
    @Test
    public void testPurge() {
        TTLCache<Object> cache = new TTLCache<>(0);
        cache.put("abc", new Object());
        cache.put("cde", new Object());
        cache.purge();
        assertEquals(0, cache.getEntries().size());
    }

    @Test
    public void testNoPurge() {
        TTLCache<Object> cache = new TTLCache<>(3);
        cache.put("abc", new Object());
        cache.put("cde", new Object());
        cache.purge();
        assertEquals(2, cache.getEntries().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInvalidModify() {
        TTLCache<Object> cache = new TTLCache<>(10);
        cache.put("abc", new Object());
        cache.put("cde", new Object());
        // This should not be allowed.
        cache.getEntries().remove("abc");
    }

}