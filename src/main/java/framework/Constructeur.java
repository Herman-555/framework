package framework;

public final class Constructeur {
    private Constructeur() {
    }

    public static <T> T construireObjet(Class<T> type) throws ReflectiveOperationException {
        return type.getDeclaredConstructor().newInstance();
    }
}
