package dk.digitalidentity.sofd.util;

import java.util.function.Supplier;

public class NullChecker {
    public static <E> E getValue(Supplier<E> supplier) {
        return getValue(supplier, null);
    }

    public static <E> E getValue(Supplier<E> supplier, E defaultValue) {
        try {
            E value = supplier.get();
            return value != null ? value : defaultValue;
        } catch (NullPointerException npe) {
            return defaultValue;
        }
    }
}