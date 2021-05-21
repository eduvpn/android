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

import android.net.Uri;

import java.util.Locale;

/**
 * Contains application-wide constant values.
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class Constants {
    public static final boolean DEBUG = BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug");

    public static final Uri HELP_URI = Uri.parse("https://www.eduvpn.org/faq.html");

    public static final String API_DISCOVERY_POSTFIX = "/info.json";

    public static final String API_SYSTEM_MESSAGES_PATH = "system_messages";
    public static final String API_USER_MESSAGES_PATH = "user_messages";
    public static final String API_PROFILE_LIST_PATH = "profile_list";
    public static final String API_CREATE_KEYPAIR = "create_keypair";
    public static final String API_PROFILE_CONFIG = "profile_config";
    public static final String API_CHECK_CERTIFICATE = "check_certificate";

    public static final int[] GONE_HTTP_CODES = new int[] {404, 410};

    public static final Locale LOCALE = Locale.getDefault();
    public static final Locale ENGLISH_LOCALE = Locale.ENGLISH;

    public static final long SERVER_LIST_VALID_FOR_MS = 3_600_000; // 1 hour

    public static final String CERT_EXPIRY_NOTIFICATION_CHANNEL_ID = "cert_expiry";
    public static final int CERT_EXPIRY_NOTIFICATION_ID = 1;
}
