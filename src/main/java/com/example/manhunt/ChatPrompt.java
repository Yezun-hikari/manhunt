package com.example.manhunt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatPrompt {
    private static final Map<UUID, ObjectiveType> prompts = new HashMap<>();

    public static void addPrompt(UUID uuid, ObjectiveType type) {
        prompts.put(uuid, type);
    }

    public static ObjectiveType getPrompt(UUID uuid) {
        return prompts.get(uuid);
    }

    public static void removePrompt(UUID uuid) {
        prompts.remove(uuid);
    }
}
