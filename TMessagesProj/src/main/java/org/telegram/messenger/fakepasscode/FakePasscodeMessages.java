package org.telegram.messenger.fakepasscode;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakePasscodeMessages {
    public static Map<Integer, Map<Integer, String>> hasUnDeletedMessages = new HashMap<>();
    private static final Object sync = new Object();
    private static boolean isLoaded = false;

    public static void loadMessages() {
        synchronized (sync) {
            if (isLoaded) {
                return;
            }

            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("passcodemessages", Context.MODE_PRIVATE);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enableDefaultTyping();
                String hasUnDeletedMessagesString = preferences.getString("hasUnDeletedMessages", null);
                hasUnDeletedMessages = mapper.readValue(hasUnDeletedMessagesString, HashMap.class);
            } catch (Exception ignored) {
                System.err.println("Error in loading messages!");
            } finally {
                isLoaded = true;
            }
        }
    }

    public static void saveMessages() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("passcodemessages", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enableDefaultTyping();
                String hasUnDeletedMessagesString = mapper.writeValueAsString(hasUnDeletedMessages);
                editor.putString("hasUnDeletedMessages", hasUnDeletedMessagesString);
                editor.commit();
            } catch (Exception ignored) {
                System.err.println("Error in commiting messages!");
            }
        }
    }
}
