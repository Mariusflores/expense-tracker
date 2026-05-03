package io.expensetracker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import io.babyredis.client.BabyRedisClient;
import io.babyredis.error.BabyRedisException;
import io.expensetracker.data.ActionType;
import io.expensetracker.data.Expense;
import io.expensetracker.data.UndoRecord;

public class ExpenseTracker {

    private static final String BALANCE_KEY = "balance";
    private static final String PERIODS_KEY = "periods";
    private static final String ACCOUNTS_KEY = "accounts";
    private static final String ACTIVE_ACCOUNT_KEY = "active_account";

    private final BabyRedisClient client;
    private Stack<UndoRecord> undoStack = new Stack<>();

    public ExpenseTracker(BabyRedisClient client) {
        this.client = client;
    }

    // Already done, keeping for reference, could be removed in future
    public void migrate() {
        try {
            String balance = client.get(ExpenseTracker.BALANCE_KEY);
            client.sAdd(ExpenseTracker.ACCOUNTS_KEY, "default");
            client.set(ExpenseTracker.ACTIVE_ACCOUNT_KEY, "default");
            client.set("default:balance", balance);
            client.delete(ExpenseTracker.BALANCE_KEY);
        } catch (BabyRedisException e) {
            System.out.println("No existing balance found. Migration not needed.");
        }
        return;
    }

    // Account management methods

    // Method to create a new account, used for organizing expenses and balances
    // under different accounts (e.g., personal, business, etc.)
    public void createAccount(String accountName) {
        // Add account to accounts set and set it as active account
        client.sAdd(ExpenseTracker.ACCOUNTS_KEY, accountName);
        client.set(ExpenseTracker.ACTIVE_ACCOUNT_KEY, accountName);

        client.set(getBalanceKey(accountName), "0.0");

    }

    public void switchAccount(String accountName) {
        client.set(ExpenseTracker.ACTIVE_ACCOUNT_KEY, accountName);
    }

    public String[] getAccounts() {
        var accounts = client.sMembers(ACCOUNTS_KEY);
        if (accounts != null) {
            return accounts;
        } else {
            return new String[0];
        }
    }

    public void logDeposit(double amount) {
        // Get current balance, if not found initialize to 0 and then add the deposit
        // amount
        double previousBalance = 0.0;
        // If balance key doesn't exist, it means this is the first time we are carrying
        // funds for this account,
        // so we can just set the balance to the deposit amount
        try {
            double balance = Double.parseDouble(client.get(getBalanceKey(getActiveAccount(client))));
            previousBalance = balance;
            balance += amount;
            client.set(getBalanceKey(getActiveAccount(client)), String.valueOf(balance));

        } catch (BabyRedisException e) {
            client.set(getBalanceKey(getActiveAccount(client)), String.valueOf(amount));
        }
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Add period to set of periods (Could helper method to avoid duplication)
        client.sAdd(PERIODS_KEY, period);

        // Add expense to set of expenses for the period, using timestamp as unique
        // identifier (Could helper method to avoid duplication)
        long timestamp = System.currentTimeMillis();

        // Create expense object for the carry action. The note for carry actions can be
        // set to a default value like "carry" or "deposit".
        Expense expense = new Expense(getActiveAccount(client), amount, "deposit", timestamp);

        // Create undo record for the carry action and push it to the undo stack
        UndoRecord undoRecord = new UndoRecord(expense, previousBalance, ActionType.CARRY);
        undoStack.push(undoRecord);

        // Store the carry action as an expense for historical purposes, even though
        // it's not technically an expense,
        // it helps to keep track of all transactions in a consistent way
        client.sAdd(getExpensesKey(period), expense.getKey());
        client.set(expense.getKey(), expense.toStorageString());

    }

    public void logExpense(double amount, String note) {
        double previousBalance = 0.0;
        // Get current balance, if not found initialize to 0 and then subtract the
        // expense amount
        try {
            double balance = Double.parseDouble(client.get(getBalanceKey(getActiveAccount(client))));
            previousBalance = balance;
            balance -= amount;
            client.set(getBalanceKey(getActiveAccount(client)), String.valueOf(balance));
        } catch (BabyRedisException e) {
            System.out.println("No balance found. Please carry funds first.");
            return;
        }

        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long timestamp = System.currentTimeMillis();

        // Add period to set of periods (Could helper method to avoid duplication)
        client.sAdd(ExpenseTracker.PERIODS_KEY, period);

        // Add expense to set of expenses for the period, using timestamp as unique
        // identifier (Could helper method to avoid duplication)
        Expense expense = new Expense(getActiveAccount(client), amount, note, timestamp);

        // Create undo record for the expense action and push it to the undo stack
        UndoRecord undoRecord = new UndoRecord(expense, previousBalance, ActionType.SPENT);
        undoStack.push(undoRecord);

        // Store the expense in Redis
        client.sAdd(getExpensesKey(period), expense.getKey());
        client.set(expense.getKey(), expense.toStorageString());

    }

    public double getBalance() {
        // Get current balance, if not found initialize to 0
        double balance = 0.0;
        try {
            balance = Double.parseDouble(client.get(getBalanceKey(getActiveAccount(client))));
            return balance;
        } catch (BabyRedisException e) {
            System.out.println("No balance found. Please carry funds first.");
        }
        return balance;
    }

    public List<Expense> getHistory() {

        // Get all expenses for the current period and return them as a list of Expense
        // objects

        var expenses = client.sMembers(getExpensesKey(getCurrentPeriod()));

        var result = new ArrayList<Expense>();

        if (expenses == null) {
            return result;
        }

        for (String expenseKey : expenses) {
            if (expenseKey.contains(getActiveAccount(client))) {
                String expenseData = client.get(expenseKey);
                Expense expense = Expense.fromStorageString(expenseKey, expenseData);
                result.add(expense);
            }
        }
        return result;
    }

    // Method to undo the last action (carry, spent, or balance correction), used
    // for correcting mistakes or reverting unintended actions
    public void undoLastAction() {
        if (undoStack.isEmpty()) {
            System.out.println("No actions to undo.");
            return;
        }

        UndoRecord lastAction = undoStack.pop();

        switch (lastAction.getActionType()) {
            case CARRY:
                // To undo a carry action, we need to subtract the carried amount from the
                // balance and delete the corresponding expense record
                double previousBalance = lastAction.getPreviousBalance();
                client.set(String.format("%s:balance", lastAction.getAccount()), String.valueOf(previousBalance));

                // Delete the corresponding expense record
                client.sRem(getExpensesKey(getCurrentPeriod()), lastAction.getExpenseKey());
                client.delete(lastAction.getExpenseKey());
                break;

            case SPENT:
                // To undo a spent action, we need to add the spent amount back to the balance
                // and delete the corresponding expense record
                previousBalance = lastAction.getPreviousBalance();
                client.set(getBalanceKey(lastAction.getAccount()), String.valueOf(previousBalance));

                // Delete the corresponding expense record
                client.sRem(getExpensesKey(getCurrentPeriod()), lastAction.getExpenseKey());
                client.delete(lastAction.getExpenseKey());
                break;

            case BALANCE:
                // To undo a balance correction, we need to set the balance back to the previous
                // balance
                client.set(getBalanceKey(lastAction.getAccount()),
                        String.valueOf(lastAction.getPreviousBalance()));
                break;

            default:
                System.out.println("Unknown action type. Cannot undo.");
        }
    }

    // Helper method to get current period in "yyyy-MM" format, used for organizing
    // expenses by month
    public String getCurrentPeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    // Helper method to get active account, used for associating expenses and
    // balance with the correct account
    public String getActiveAccount(BabyRedisClient client) {

        String activeAccount;

        try {
            activeAccount = client.get(ACTIVE_ACCOUNT_KEY);
        } catch (BabyRedisException e) {
            activeAccount = null;
            System.out.println("No active account found. Please create or switch to an account first.");
        }
        return activeAccount;
    }

    // Helper method to get all expenses for a specific account, used for operations
    // like deleting an account and optionally its history
    public List<Expense> getAllExpensesByAccount(String accountName) {
        var result = new ArrayList<Expense>();

        for (String period : client.sMembers(PERIODS_KEY)) {
            var expenses = client.sMembers(getExpensesKey(period));
            if (expenses == null) {
                continue;
            }

            for (String expenseKey : expenses) {
                if (expenseKey.contains(accountName)) {
                    String expenseData = client.get(expenseKey);
                    Expense expense = Expense.fromStorageString(expenseKey, expenseData);
                    result.add(expense);
                }
            }
        }
        return result;
    }

    // Method to correct balance for the active account, used for correcting
    // mistakes or discrepancies in the balance
    public void correctBalance(double balance) {
        double previousBalance = getBalance();

        UndoRecord undoRecord = new UndoRecord(
                null,
                getActiveAccount(client),
                0.0,
                previousBalance,
                "balance correction",
                System.currentTimeMillis(),
                ActionType.BALANCE);

        undoStack.push(undoRecord);

        client.set(getBalanceKey(getActiveAccount(client)), String.valueOf(balance));
    }

    // Method to delete an account, used for removing accounts that are no longer
    // needed. Optionally deletes the history of expenses associated with the
    // account.
    public void deleteAccount(String accountName, boolean deleteHistory) {
        try {
            // Remove account from accounts set
            client.sRem(ACCOUNTS_KEY, accountName);

            // Delete balance key for the account
            client.delete(getBalanceKey(accountName));

            // Get all expenses for the account and delete them
            List<Expense> expenseList = getAllExpensesByAccount(accountName);

            if (deleteHistory) {
                for (Expense expense : expenseList) {
                    // Extract the period from the expense timestamp
                    LocalDate date = LocalDate.ofEpochDay(expense.getTimestamp() / (24 * 60 * 60 * 1000));
                    String period = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));

                    // Delete the expense key and remove it from the period set
                    client.sRem(getExpensesKey(period), expense.getKey());
                    client.delete(expense.getKey());
                }
            }

            // Optionally, could also delete expenses related to the account, but for now we
            // will keep them for historical purposes
        } catch (BabyRedisException e) {
            System.out.println("Error occurred while deleting account.");
        }
    }

    // Helper method to construct balance key for a given account, used for operations
    // that involve accessing or modifying the balance of a specific account
    private String getBalanceKey(String accountName) {
        return String.format("%s:balance", accountName);
    }

    private String getExpensesKey(String period) {
        return String.format("expenses:%s", period);
    }
}
