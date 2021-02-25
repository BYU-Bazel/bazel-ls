package server.bazel.interp;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

class Utility {
    private Utility() {
    }

    static String hash(String value) {
        Preconditions.checkNotNull(value);
        return Hashing.sha256().hashString(value, StandardCharsets.UTF_8).toString();
    }
}
