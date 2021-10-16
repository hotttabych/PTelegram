package org.telegram.messenger.fakepasscode;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.messenger.ApplicationLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoveAsReadMessages {
    public static class RemoveAsReadMessage {
        private int id;
        private long readTime = -1;
        private int scheduledTimeMs;

        public RemoveAsReadMessage() {
        }

        public RemoveAsReadMessage(int id, int scheduledTimeMs) {
            this.id = id;
            this.scheduledTimeMs = scheduledTimeMs;
        }

        public RemoveAsReadMessage(int id, int scheduledTimeMs, long readTime) {
            this.id = id;
            this.scheduledTimeMs = scheduledTimeMs;
            this.readTime = readTime;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getScheduledTimeMs() {
            return scheduledTimeMs;
        }

        public void setScheduledTimeMs(int scheduledTimeMs) {
            this.scheduledTimeMs = scheduledTimeMs;
        }

        public long getReadTime() {
            return readTime;
        }

        public void setReadTime(long readTime) {
            this.readTime = readTime;
        }
    }

    public static Map<String, Map<String, List<RemoveAsReadMessage>>> messagesToRemoveAsRead = new HashMap<>();
    public static Map<String, Integer> delays = new HashMap<>();
    private static final Object sync = new Object();
    private static boolean isLoaded = false;

    public static void load() {
        synchronized (sync) {
            if (isLoaded) {
                return;
            }

            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("removeasreadmessages", Context.MODE_PRIVATE);
                ObjectMapper mapper = new ObjectMapper();
                mapper.enableDefaultTyping();
                String messagesToRemoveAsReadString = preferences.getString("messagesToRemoveAsRead", null);
                String delaysString = preferences.getString("delays", null);
                messagesToRemoveAsRead = mapper.readValue(messagesToRemoveAsReadString, HashMap.class);
                delays = mapper.readValue(delaysString, HashMap.class);
                isLoaded = true;
            } catch (Exception ignored) {
                System.err.println("Error in loading messages!");
            }
        }
    }

    public static void save() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("removeasreadmessages", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enableDefaultTyping();
                String messagesToRemoveAsReadString = mapper.writeValueAsString(messagesToRemoveAsRead);
                String delaysString = mapper.writeValueAsString(delays);
                editor.putString("messagesToRemoveAsRead", messagesToRemoveAsReadString);
                editor.putString("delays", delaysString);
                editor.commit();
            } catch (Exception ignored) {
                System.err.println("Error in commiting messages!");
            }
        }
    }
}
