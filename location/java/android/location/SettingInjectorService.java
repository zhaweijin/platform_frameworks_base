/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.location;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference;
import android.util.Log;

/**
 * Dynamically specifies the summary (subtile) and enabled status of a preference injected into
 * the "Settings > Location > Location services" list.
 *
 * The location services list is intended for use only by preferences that affect multiple apps from
 * the same developer. Location settings that apply only to one app should be shown within that app,
 * rather than in the system settings.
 *
 * To add a preference to the list, a subclass of {@link SettingInjectorService} must be declared in
 * the manifest as so:
 * <pre>
 *     <service android:name="com.example.android.injector.MyInjectorService" >
 *         <intent-filter>
 *             <action android:name="com.android.settings.InjectedLocationSetting" />
 *         </intent-filter>
 *
 *         <meta-data
 *             android:name="com.android.settings.InjectedLocationSetting"
 *             android:resource="@xml/my_injected_location_setting" />
 *     </service>
 * </pre>
 * The resource file specifies the static data for the setting:
 * <pre>
 *     <injected-location-setting xmlns:android="http://schemas.android.com/apk/res/android"
 *         android:label="@string/injected_setting_label"
 *         android:icon="@drawable/ic_launcher"
 *         android:settingsActivity="com.example.android.injector.MySettingActivity"
 *     />
 * </pre>
 * Here:
 * <ul>
 *     <li>label: The {@link Preference#getTitle()} value. The title should make it clear which apps
 *     are affected by the setting, typically by including the name of the developer. For example,
 *     "Acme Corp. ads preferences." </li>
 *
 *     <li>icon: The {@link Preference#getIcon()} value. Typically this will be a generic icon for
 *     the developer rather than the icon for an individual app.</li>
 *
 *     <li>settingsActivity: the activity which is launched to allow the user to modify the setting
 *     value  The activity must be in the same package as the subclass of
 *     {@link SettingInjectorService}. The activity should use your own branding to help emphasize
 *     to the user that it is not part of the system settings.</li>
 * </ul>
 *
 * For consistency, the label and {@link #getStatus()} values should be provided in all of the
 * locales supported by the system settings app. The text should not contain offensive language.
 *
 * For compactness, only one copy of a given setting should be injected. If each account has a
 * distinct value for the setting, then the {@link #getStatus()} value should represent a summary of
 * the state across all of the accounts and {@code settingsActivity} should display the value for
 * each account.
 *
 * Apps that violate these guidelines will be taken down from the Google Play Store and/or flagged
 * as malware.
 */
// TODO: is there a public list of supported locales?
// TODO: is there a public list of guidelines for settings text?
public abstract class SettingInjectorService extends IntentService {

    /**
     * Name of the bundle key for the string specifying the status of the setting (e.g., "ON" or
     * "OFF").
     *
     * @hide
     */
    public static final String STATUS_KEY = "status";

    /**
     * Name of the bundle key for the string specifying whether the setting is currently enabled.
     *
     * @hide
     */
    public static final String ENABLED_KEY = "enabled";

    /**
     * Name of the intent key used to specify the messenger
     *
     * @hide
     */
    public static final String MESSENGER_KEY = "messenger";

    private final String mLogTag;

    /**
     * Constructor.
     *
     * @param logTag used for logging, must be less than 23 characters
     */
    public SettingInjectorService(String logTag) {
        super(logTag);

        // Fast fail if log tag is too long
        Log.isLoggable(logTag, Log.WARN);

        mLogTag = logTag;
    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        // Get messenger first to ensure intent doesn't get messed with (in case we later decide
        // to pass intent into getStatus())
        Messenger messenger = intent.getParcelableExtra(MESSENGER_KEY);

        Status status = getStatus();

        // Send the status back to the caller via the messenger
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putString(STATUS_KEY, status.summary);
        bundle.putBoolean(ENABLED_KEY, status.enabled);
        message.setData(bundle);

        if (Log.isLoggable(mLogTag, Log.DEBUG)) {
            Log.d(mLogTag,
                    "received " + intent + " and " + status + ", sending message: " + message);
        }
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(mLogTag, "", e);
        }
    }

    /**
     * Reads the status of the setting.
     */
    protected abstract Status getStatus();

    /**
     * Dynamic characteristics of an injected location setting.
     */
    public static final class Status {

        public final String summary;

        public final boolean enabled;

        /**
         * Constructor.
         *
         * @param summary the {@link Preference#getSummary()} value
         * @param enabled the {@link Preference#isEnabled()} value
         */
        public Status(String summary, boolean enabled) {
            this.summary = summary;
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return "Status{summary='" + summary + '\'' + ", enabled=" + enabled + '}';
        }
    }
}