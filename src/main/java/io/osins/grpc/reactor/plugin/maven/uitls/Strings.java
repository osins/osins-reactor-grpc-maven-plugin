package io.osins.grpc.reactor.plugin.maven.uitls;

public final class Strings {
    public static String firstToUpperCase(String str) {
        return (str == null || str.isEmpty()) ? str
                : str.transform(s -> s.substring(0, 1).toUpperCase() + s.substring(1));
    }

    public static String firstToLowerCase(String str) {
        return (str == null || str.isEmpty()) ? str
                : str.transform(s -> s.substring(0, 1).toLowerCase() + s.substring(1));
    }
}
