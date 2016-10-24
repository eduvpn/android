package nl.eduvpn.app.entity;

/**
 * Contains the application settings.
 * Created by Daniel Zolnai on 2016-10-22.
 */
public class Settings {
    private boolean _useCustomTabs;
    private boolean _forceTcp;

    public Settings(boolean useCustomTabs, boolean forceTcp) {
        _useCustomTabs = useCustomTabs;
        _forceTcp = forceTcp;
    }

    public boolean useCustomTabs() {
        return _useCustomTabs;
    }

    public boolean forceTcp() {
        return _forceTcp;
    }
}
