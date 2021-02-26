package server.utils;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Object, Object> injectables = new HashMap<>();

    private Context() {
    }

    public static Context empty() {
        return new Context();
    }

    public void clear() {
        injectables.clear();
    }

    public <T> void put(Class<T> clazz, T value) {
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(value);
        injectables.put(clazz, value);
    }

    public <T> boolean remove(Class<T> clazz) {
        Preconditions.checkNotNull(clazz);
        final Object instance = injectables.remove(clazz);
        return instance != null;
    }

    public <T> T get(Class<T> clazz) {
        Preconditions.checkNotNull(clazz);
        if (!injectables.containsKey(clazz)) {
            return null;
        }

        final Object instance = injectables.get(clazz);
        if (clazz.isInstance(instance)) {
            return clazz.cast(instance);
        }

        throw new ClassCastException();
    }

    public <T> boolean contains(Class<T> clazz) {
        Preconditions.checkNotNull(clazz);
        return injectables.containsKey(clazz);
    }

    @Override
    public String toString() {
        return "Context{" +
                "injectables=" + injectables +
                '}';
    }
}
