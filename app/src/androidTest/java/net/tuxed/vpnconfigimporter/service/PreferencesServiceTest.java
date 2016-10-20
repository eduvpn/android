package net.tuxed.vpnconfigimporter.service;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for the preferences service.
 * Created by Daniel Zolnai on 2016-10-18.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PreferencesServiceTest {

    private PreferencesService _preferencesService;

    @Before
    public void before() {
        SerializerService serializerService = new SerializerService();
        Context context = InstrumentationRegistry.getContext();
        _preferencesService = new PreferencesService(context, serializerService);
    }

    @Test
    public void testConnectionStateSave() {
        String state = "abc2141awksld";
        _preferencesService.currentConnectionState(state);
        String retrievedState = _preferencesService.getCurrentConnectionState();
        assertEquals(state, retrievedState);
    }

    @Test
    public void testInstanceSave() {
        Instance instance = new Instance("http://example.com", "Example", "http://example.com/image.jpg");
        _preferencesService.currentInstance(instance);
        Instance retrievedInstance = _preferencesService.getCurrentInstance();
        assertEquals(instance.getDisplayName(), retrievedInstance.getDisplayName());
        assertEquals(instance.getLogoUri(), retrievedInstance.getLogoUri());
        assertEquals(instance.getBaseUri(), retrievedInstance.getBaseUri());
    }
    @Test
    public void testDiscoveredAPISave() {
        DiscoveredAPI discoveredAPI = new DiscoveredAPI(1, "http://example.com/", "http://example.com/create_config",
                "http://example.com/profile_list", "http://example.com/system_messages", "http://example.com/user_messages");
        _preferencesService.currentDiscoveredAPI(discoveredAPI);
        DiscoveredAPI retrievedDiscoveredAPI = _preferencesService.getCurrentDiscoveredAPI();
        assertEquals(discoveredAPI.getVersion(), retrievedDiscoveredAPI.getVersion());
        assertEquals(discoveredAPI.getAuthorizationEndpoint(), retrievedDiscoveredAPI.getAuthorizationEndpoint());
        assertEquals(discoveredAPI.getCreateConfigAPI(), retrievedDiscoveredAPI.getCreateConfigAPI());
        assertEquals(discoveredAPI.getProfileListAPI(), retrievedDiscoveredAPI.getProfileListAPI());
        assertEquals(discoveredAPI.getSystemMessagesAPI(), retrievedDiscoveredAPI.getSystemMessagesAPI());
        assertEquals(discoveredAPI.getUserMessagesAPI(), retrievedDiscoveredAPI.getUserMessagesAPI());
    }

}