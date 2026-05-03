package io.expensetracker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.babyredis.client.BabyRedisClient;
import io.babyredis.error.BabyRedisException;

public class ExpenseTracker {

    private static final String BALANCE_KEY = "balance";
    private static final String PERIODS_KEY = "periods";
    private static final String ACCOUNTS_KEY = "accounts";
    private static final String ACTIVE_ACCOUNT_KEY = "active_account";

    private final BabyRedisClient client;

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
        } catch ( BabyRedisException e) {
            System.out.println("No existing balance found. Migration not needed.");
        }
        return;
    }

    public void createAccount(String accountName) {
        client.sAdd(ExpenseTracker.ACCOUNTS_KEY, accountName);
        client.set(ExpenseTracker.ACTIVE_ACCOUNT_KEY, accountName);
        
    }

    public void switchAccount(String accountName) {
        client.set(ExpenseTracker.ACTIVE_ACCOUNT_KEY, accountName);        
    }

    public String[] getAccounts() {
        var accounts = client.sMembers(ACCOUNTS_KEY);
        if(accounts != null){
            return accounts;
        }else{
            return new String[0];
        }
    }

    public void logDeposit(double amount) {
        try{
            double balance = Double.parseDouble(client.get(String.format("%s:balance", getActiveAccount(client))));
            balance += amount;
            client.set(String.format("%s:balance", getActiveAccount(client)), String.valueOf(balance));
        } catch(BabyRedisException e) {
            client.set(String.format("%s:balance", getActiveAccount(client)), String.valueOf(amount));
        }
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    
        // Add period to set of periods (Could helper method to avoid duplication)
        client.sAdd(PERIODS_KEY, period);
        // Add expense to set of expenses for the period, using timestamp as unique identifier (Could helper method to avoid duplication)
        long timestamp = System.currentTimeMillis();
        client.sAdd("expenses:" + period, String.format("expense:%s:%d", getActiveAccount(client), timestamp));
    
        client.set(String.format("expense:%s:%d", getActiveAccount(client), timestamp), String.format("%s|%s|%d", amount, "carry", timestamp));
    
    }

    public void logExpense(double amount, String note) {
        try{
            double balance = Double.parseDouble(client.get(String.format("%s:balance", getActiveAccount(client))));
            balance -= amount;
            client.set(String.format("%s:balance", getActiveAccount(client)), String.valueOf(balance));
        } catch(BabyRedisException e) {
            System.out.println("No balance found. Please carry funds first.");
            return;
        }
    
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long timestamp = System.currentTimeMillis();
    
        // Add period to set of periods (Could helper method to avoid duplication)
        client.sAdd(ExpenseTracker.PERIODS_KEY, period);
        // Add expense to set of expenses for the period, using timestamp as unique identifier (Could helper method to avoid duplication)
        client.sAdd("expenses:" + period, String.format("expense:%s:%d", getActiveAccount(client), timestamp));
        client.set(String.format("expense:%s:%d", getActiveAccount(client), timestamp), String.format("%s|%s|%d", amount, note, timestamp));

    
    }

    public double getBalance() {
        double balance = 0.0;
        try{
            balance = Double.parseDouble(client.get(String.format("%s:balance", getActiveAccount(client))));
            System.out.printf("Current Balance for account %s: kr %.2f\n", getActiveAccount(client), balance);
            return balance;
        } catch(BabyRedisException e) {
            System.out.println("No balance found. Please carry funds first.");
        }
        return balance;
    }

    public String[] getHistory(){
            
        var expenses = client.sMembers("expenses:" + getCurrentPeriod());

        return expenses != null ? expenses : new String[0];
    }

    public String getCurrentPeriod(){

    return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    public String getActiveAccount(BabyRedisClient client) {
        
        String activeAccount;

        try{
            activeAccount = client.get(ACTIVE_ACCOUNT_KEY);
        }catch(BabyRedisException e) {
            activeAccount = null;
            System.out.println("No active account found. Please create or switch to an account first.");
        }
        return activeAccount;
    }
}
