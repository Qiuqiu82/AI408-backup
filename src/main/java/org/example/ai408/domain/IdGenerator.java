package org.example.ai408.domain;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String prefixed(String prefix) {
        return prefix + "_" + uuid().substring(0, 12);
    }
}
