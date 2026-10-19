package helpdesk.repository.memory;

import helpdesk.repository.TransactionRunner;

import java.util.function.Supplier;

public class InMemoryTransactionRunner implements TransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return action.get();
    }
}
