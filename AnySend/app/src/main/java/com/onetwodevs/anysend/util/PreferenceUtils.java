package com.onetwodevs.anysend.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * created by: 12 Devs
 * date: 31.03.2018 13:48
 */
public class PreferenceUtils extends com.genonbeta.android.framework.util.PreferenceUtils
{
    public static void syncDefaults(Context context)
    {
        syncDefaults(context, true, false);
    }

    public static void syncDefaults(Context context, boolean compare, boolean fromXml)
    {
        SharedPreferences preferences = AppUtils.getDefaultLocalPreferences(context);
        SharedPreferences binaryPreferences = AppUtils.getDefaultPreferences(context);

        if (compare)
            sync(preferences, binaryPreferences);
        else {
            if (fromXml)
                syncPreferences(preferences, binaryPreferences);
            else
                syncPreferences(binaryPreferences, preferences);
        }
    }
}
