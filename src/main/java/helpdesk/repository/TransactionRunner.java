package helpdesk.repository;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> action);

    default void runInTransaction(Runnable action) {
        inTransaction(() -> {
            action.run();
            return null;
        });
    }
}
