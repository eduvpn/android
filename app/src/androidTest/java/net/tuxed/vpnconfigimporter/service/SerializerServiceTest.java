package net.tuxed.vpnconfigimporter.service;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import net.tuxed.vpnconfigimporter.entity.DiscoveredAPI;
import net.tuxed.vpnconfigimporter.entity.Instance;
import net.tuxed.vpnconfigimporter.entity.InstanceList;
import net.tuxed.vpnconfigimporter.entity.Profile;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

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
    public void testProfileSerialization() throws SerializerService.UnknownFormatException {
        Profile profile = new Profile("displayName", "poolId", true);
        JSONObject serializedProfile = _serializerService.serializeProfile(profile);
        Profile deserializedProfile = _serializerService.deserializeProfile(serializedProfile);
        assertEquals(profile.getDisplayName(), deserializedProfile.getDisplayName());
        assertEquals(profile.getPoolId(), deserializedProfile.getPoolId());
        assertEquals(profile.getTwoFactor(), deserializedProfile.getTwoFactor());
    }

    @Test
    public void testInstanceSerialization() throws SerializerService.UnknownFormatException {
        Instance instance = new Instance("baseUri", "displayName", "logoUri");
        JSONObject serializedInstance = _serializerService.serializeInstance(instance);
        Instance deserializedInstance = _serializerService.deserializeInstance(serializedInstance);
        assertEquals(instance.getDisplayName(), deserializedInstance.getDisplayName());
        assertEquals(instance.getBaseUri(), deserializedInstance.getBaseUri());
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
        assertEquals(instanceList.getInstanceList().get(0).getBaseUri(), deserializedInstanceList.getInstanceList().get(0).getBaseUri());
        assertEquals(instanceList.getInstanceList().get(0).getLogoUri(), deserializedInstanceList.getInstanceList().get(0).getLogoUri());
        assertEquals(instanceList.getInstanceList().get(1).getDisplayName(), deserializedInstanceList.getInstanceList().get(1).getDisplayName());
        assertEquals(instanceList.getInstanceList().get(1).getBaseUri(), deserializedInstanceList.getInstanceList().get(1).getBaseUri());
        assertEquals(instanceList.getInstanceList().get(1).getLogoUri(), deserializedInstanceList.getInstanceList().get(1).getLogoUri());
    }

}