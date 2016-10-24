package net.tuxed.vpnconfigimporter.service;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.InstanceList;
import net.tuxed.vpnconfigimporter.entity.Profile;
import net.tuxed.vpnconfigimporter.entity.SavedProfile;
import net.tuxed.vpnconfigimporter.entity.SavedToken;
import net.tuxed.vpnconfigimporter.entity.Settings;
import net.tuxed.vpnconfigimporter.entity.message.Maintenance;
import net.tuxed.vpnconfigimporter.entity.message.Message;
import net.tuxed.vpnconfigimporter.entity.message.Notification;
import net.tuxed.vpnconfigimporter.utils.TTLCache;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the serializer service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SerializerServiceTest {

    private SerializerService _serializerService;

    @Before
    public void before() {
        _serializerService = new SerializerService();
    }

    @Test
    public void testAppSettingsSerialization() throws SerializerService.UnknownFormatException {
        Settings settings = new Settings(true, true);
        JSONObject jsonObject = _serializerService.serializeAppSettings(settings);
        Settings deserializedSettings = _serializerService.deserializeAppSettings(jsonObject);
        assertEquals(settings.forceTcp(), deserializedSettings.forceTcp());
        assertEquals(settings.useCustomTabs(), settings.useCustomTabs());
    }

    @Test
    public void testProfileSerialization() throws SerializerService.UnknownFormatException {
        Profile profile = new Profile("displayName", "profileId", true);
        JSONObject serializedProfile = _serializerService.serializeProfile(profile);
        Profile deserializedProfile = _serializerService.deserializeProfile(serializedProfile);
        assertEquals(profile.getDisplayName(), deserializedProfile.getDisplayName());
        assertEquals(profile.getProfileId(), deserializedProfile.getProfileId());
        assertEquals(profile.getTwoFactor(), deserializedProfile.getTwoFactor());
    }

    @Test
    public void testInstanceSerialization() throws SerializerService.UnknownFormatException {
        Instance instance = new Instance("baseUri", "displayName", "logoUri");
        JSONObject serializedInstance = _serializerService.serializeInstance(instance);
        Instance deserializedInstance = _serializerService.deserializeInstance(serializedInstance);
        assertEquals(instance.getDisplayName(), deserializedInstance.getDisplayName());
        assertEquals(instance.getBaseURI(), deserializedInstance.getBaseURI());
        assertEquals(instance.getLogoUri(), deserializedInstance.getLogoUri());
    }

    @Test
    public void testDiscoveredAPISerialization() throws SerializerService.UnknownFormatException {
        DiscoveredAPI discoveredAPI = new DiscoveredAPI(1, "authEndpoint", "createConfig", "profileList",
                "systemMessages", "userMessages");
        JSONObject serializedDiscoveredAPI = _serializerService.serializeDiscoveredAPI(discoveredAPI);
        DiscoveredAPI deserializedDiscoveredAPI = _serializerService.deserializeDiscoveredAPI(serializedDiscoveredAPI);
        assertEquals(discoveredAPI.getVersion(), deserializedDiscoveredAPI.getVersion());
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), deserializedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getCreateConfigAPI(), deserializedDiscoveredAPI.getCreateConfigAPI());
        assertEquals(discoveredAPI.getProfileListAPI(), deserializedDiscoveredAPI.getProfileListAPI());
        assertEquals(discoveredAPI.getSystemMessagesAPI(), deserializedDiscoveredAPI.getSystemMessagesAPI());
        assertEquals(discoveredAPI.getUserMessagesAPI(), deserializedDiscoveredAPI.getUserMessagesAPI());
    }

    @Test
    public void testInstanceListSerialization() throws SerializerService.UnknownFormatException {
        Instance instance1 = new Instance("baseUri", "displayName", "logoUri");
        Instance instance2 = new Instance("baseUri2", "displayName2", "logoUri2");
        InstanceList instanceList = new InstanceList(1, Arrays.asList(instance1, instance2));
        JSONObject serializedInstanceList = _serializerService.serializeInstanceList(instanceList);
        InstanceList deserializedInstanceList = _serializerService.deserializeInstanceList(serializedInstanceList);
        assertEquals(instanceList.getVersion(), deserializedInstanceList.getVersion());
        assertEquals(instanceList.getInstanceList().size(), deserializedInstanceList.getInstanceList().size());
        assertEquals(instanceList.getInstanceList().get(0).getDisplayName(), deserializedInstanceList.getInstanceList().get(0).getDisplayName());
        assertEquals(instanceList.getInstanceList().get(0).getBaseURI(), deserializedInstanceList.getInstanceList().get(0).getBaseURI());
        assertEquals(instanceList.getInstanceList().get(0).getLogoUri(), deserializedInstanceList.getInstanceList().get(0).getLogoUri());
        assertEquals(instanceList.getInstanceList().get(1).getDisplayName(), deserializedInstanceList.getInstanceList().get(1).getDisplayName());
        assertEquals(instanceList.getInstanceList().get(1).getBaseURI(), deserializedInstanceList.getInstanceList().get(1).getBaseURI());
        assertEquals(instanceList.getInstanceList().get(1).getLogoUri(), deserializedInstanceList.getInstanceList().get(1).getLogoUri());
    }

    @Test
    public void testMessageListSerialization() throws SerializerService.UnknownFormatException {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Maintenance maintenance = new Maintenance(utcCalendar.getTime(), utcCalendar.getTime(), utcCalendar.getTime());
        Notification notification = new Notification(utcCalendar.getTime(), "Example notification");
        List<Message> messageList = Arrays.asList(maintenance, notification);
        JSONObject serializedList = _serializerService.serializeMessageList(messageList);
        List<Message> deserializedList = _serializerService.deserializeMessageList(serializedList);
        assertEquals(messageList.size(), deserializedList.size());
        assertEquals(_norm(messageList.get(0).getDate()), _norm(deserializedList.get(0).getDate()));
        assertEquals(_norm(((Maintenance)messageList.get(0)).getStart()), _norm(((Maintenance)deserializedList.get(0)).getStart()));
        assertEquals(_norm(((Maintenance)messageList.get(0)).getEnd()), _norm(((Maintenance)deserializedList.get(0)).getEnd()));
        assertEquals(_norm(messageList.get(1).getDate()), _norm(deserializedList.get(1).getDate()));
        assertEquals(((Notification)messageList.get(1)).getContent(), ((Notification)deserializedList.get(1)).getContent());
    }

    @Test
    public void testTTLCacheSerialization() throws SerializerService.UnknownFormatException {
        TTLCache<DiscoveredAPI> cache = new TTLCache<>(100);
        DiscoveredAPI discoveredAPI1 = new DiscoveredAPI(1, "authEndpoint", "createConfig", "profileList",
                "systemMessages", "userMessages");
        DiscoveredAPI discoveredAPI2 = new DiscoveredAPI(1, "authEndpoint2", "createConfig2", "profileList2",
                "systemMessages2", "userMessages2");
        cache.put("key1", discoveredAPI1);
        cache.put("key2", discoveredAPI2);
        JSONObject serializedCache = _serializerService.serializeDiscoveredAPITTLCache(cache);
        TTLCache<DiscoveredAPI> deserializedCache = _serializerService.deserializeDiscoveredAPITTLCache(serializedCache);
        assertEquals(cache.getPurgeAfterSeconds(), deserializedCache.getPurgeAfterSeconds());
        assertEquals(cache.getEntries().size(), deserializedCache.getEntries().size());
        Iterator<Map.Entry<String, Pair<Date, DiscoveredAPI>>> iterator = cache.getEntries().entrySet().iterator();
        Iterator<Map.Entry<String, Pair<Date, DiscoveredAPI>>> deserializedIterator = deserializedCache.getEntries().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pair<Date, DiscoveredAPI>> entry = iterator.next();
            Map.Entry<String, Pair<Date, DiscoveredAPI>> deserializedEntry = deserializedIterator.next();
            assertEquals(entry.getKey(), deserializedEntry.getKey());
            assertEquals(entry.getValue().first, deserializedEntry.getValue().first);
            assertEquals(entry.getValue().second.getCreateConfigAPI(), deserializedEntry.getValue().second.getCreateConfigAPI());
            assertEquals(entry.getValue().second.getProfileListAPI(), deserializedEntry.getValue().second.getProfileListAPI());
            assertEquals(entry.getValue().second.getAuthorizationEndpoint(), deserializedEntry.getValue().second.getAuthorizationEndpoint());
            assertEquals(entry.getValue().second.getUserMessagesAPI(), deserializedEntry.getValue().second.getUserMessagesAPI());
            assertEquals(entry.getValue().second.getSystemMessagesAPI(), deserializedEntry.getValue().second.getSystemMessagesAPI());
            assertEquals(entry.getValue().second.getVersion(), deserializedEntry.getValue().second.getVersion());
        }
    }

    @Test
    public void testSavedTokenListSerialization() throws SerializerService.UnknownFormatException {
        Instance instance1 = new Instance("baseUri1", "displayName1", null);
        Instance instance2 = new Instance("baseUri2", "displayName2", null);
        SavedToken token1 = new SavedToken(instance1, "accessToken1");
        SavedToken token2 = new SavedToken(instance2, "accessToken2");
        List<SavedToken> list = Arrays.asList(token1, token2);
        JSONObject serializedList = _serializerService.serializeSavedTokenList(list);
        List<SavedToken> deserializedList = _serializerService.deserializeSavedTokenList(serializedList);
        assertEquals(list.size(), deserializedList.size());
        for (int i = 0; i < list.size(); ++i) {
            assertEquals(list.get(i).getInstance().getSanitizedBaseURI(), deserializedList.get(i).getInstance().getSanitizedBaseURI());
            assertEquals(list.get(i).getAccessToken(), deserializedList.get(i).getAccessToken());
        }
    }

    @Test
    public void testSavedProfileListSerialization() throws SerializerService.UnknownFormatException {
        Instance instance1 = new Instance("baseUri1", "displayName1", "logoUri1");
        Instance instance2 = new Instance("baseUri2", "displayName2", "logoUri2");
        Profile profile1 = new Profile("displayName1", "profileId1", false);
        Profile profile2 = new Profile("displayName2", "profileId2", true);
        SavedProfile savedProfile1 = new SavedProfile(instance1, profile1, "profileUUID1");
        SavedProfile savedProfile2 = new SavedProfile(instance2, profile2, "profileUUID2");
        List<SavedProfile> list = Arrays.asList(savedProfile1, savedProfile2);
        JSONObject serializedList = _serializerService.serializeSavedProfileList(list);
        List<SavedProfile> deserializedList = _serializerService.deserializeSavedProfileList(serializedList);
        assertEquals(list.size(), deserializedList.size());
        for (int i = 0; i < list.size(); ++i) {
            assertEquals(list.get(i).getInstance().getBaseURI(), deserializedList.get(i).getInstance().getBaseURI());
            assertEquals(list.get(i).getInstance().getDisplayName(), deserializedList.get(i).getInstance().getDisplayName());
            assertEquals(list.get(i).getInstance().getLogoUri(), deserializedList.get(i).getInstance().getLogoUri());
            assertEquals(list.get(i).getProfile().getDisplayName(), deserializedList.get(i).getProfile().getDisplayName());
            assertEquals(list.get(i).getProfile().getProfileId(), deserializedList.get(i).getProfile().getProfileId());
            assertEquals(list.get(i).getProfileUUID(), deserializedList.get(i).getProfileUUID());
        }
    }

    /**
     * Removes the milliseconds from a date. Required because the parser does not care about milliseconds.
     *
     * @param input The date to remove the milliseconds from.
     * @return The date without the milliseconds.
     */
    public static Date _norm(Date input) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(input);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

}