package org.telegram.messenger.fakepasscode;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.List;

class Utils {
    static Location getLastLocation() {
        LocationManager lm = (LocationManager) ApplicationLoader.applicationContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location l = null;
        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) {
                break;
            }
        }
        return l;
    }

    static String getLastLocationString() {
        Location loc = Utils.getLastLocation();
        if (loc != null) {
            return " " + LocaleController.getString("Geolocation", R.string.Geolocation) + ":" + loc.getLatitude() + ", " + loc.getLongitude();
        } else {
            return "";
        }
    }
}
