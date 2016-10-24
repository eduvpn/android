package nl.eduvpn.app.entity;

/**
 * Stores the data of a saved profile.
 * Created by Daniel Zolnai on 2016-10-20.
 */
public class SavedProfile {

    private Instance _instance;
    private Profile _profile;
    private String _profileUUID;

    public SavedProfile(Instance instance, Profile profile, String profileUUID) {
        _instance = instance;
        _profile = profile;
        _profileUUID = profileUUID;
    }

    public Instance getInstance() {
        return _instance;
    }

    public Profile getProfile() {
        return _profile;
    }

    public String getProfileUUID() {
        return _profileUUID;
    }
}
