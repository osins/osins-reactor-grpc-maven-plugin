package io.osins.grpc.reactor.plugin.maven.uitls;

import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public final class SpoonUtils {
    @SuppressWarnings("unchecked")
    public static <T> CtFieldReference<T> castFieldReference(CtFieldReference<?> ref) {
        return (CtFieldReference<T>) ref;
    }


    @SuppressWarnings("unchecked")
    public static <T> CtTypeReference<T> castTypeReference(CtTypeReference<?> ref) {
        return (CtTypeReference<T>) ref;
    }
}
