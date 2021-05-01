package org.telegram.messenger.fakepasscode;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakePasscodeMessages {
    public static class FakePasscodeMessage {
        private String message;
        private int date;

        public FakePasscodeMessage() {
        }

        public FakePasscodeMessage(String message, int date) {
            this.message = message;
            this.date = date;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getDate() {
            return date;
        }

        public void setDate(int date) {
            this.date = date;
        }
    }

    public static Map<String, Map<String, FakePasscodeMessage>> hasUnDeletedMessages = new HashMap<>();
    private static final Object sync = new Object();
    private static boolean isLoaded = false;

    public static void loadMessages() {
        synchronized (sync) {
            if (isLoaded) {
                return;
            }

            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("fakepasscodemessages", Context.MODE_PRIVATE);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enableDefaultTyping();
                String hasUnDeletedMessagesString = preferences.getString("hasUnDeletedMessages", null);
                hasUnDeletedMessages = mapper.readValue(hasUnDeletedMessagesString, HashMap.class);
                isLoaded = true;
            } catch (Exception ignored) {
                System.err.println("Error in loading messages!");
            }
        }
    }

    public static void saveMessages() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("fakepasscodemessages", Context.MODE_PRIVATE);
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
