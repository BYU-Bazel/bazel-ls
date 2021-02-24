package server.utils;

public class Exceptions {
    private Exceptions() {
    }

    public static class Unimplemented extends RuntimeException {
        public Unimplemented() {
        }

        public Unimplemented(String msg) {
            super(msg);
        }
    }
}
