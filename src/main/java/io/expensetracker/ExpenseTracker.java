package io.expensetracker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.babyredis.client.BabyRedisClient;
import io.babyredis.error.BabyRedisException;

public class ExpenseTracker {

    public static final String BALANCE_KEY = "balance";
    public static final String PERIODS_KEY = "periods";
    public static final String ACCOUNTS_KEY = "accounts";
    public static final String ACTIVE_ACCOUNT_KEY = "active_account";

    public static String getActiveAccount(BabyRedisClient client) {
        
        String activeAccount;

        try{
            activeAccount = client.get(ACTIVE_ACCOUNT_KEY);
        }catch(BabyRedisException e) {
            activeAccount = null;
            System.out.println("No active account found. Please create or switch to an account first.");
        }
        return activeAccount;
    }

public static void help() {
    System.out.println("=== Expense Tracker ===");
    System.out.println("Usage:");
    System.out.println("  migrate              one-time setup migrate one-balance model to multi-account model");
    System.out.println("  create <name>               Create a new account");
    System.out.println("  switch <account>            Switch to a different account");
    System.out.println("  list-accounts               List all accounts");
    System.out.println("  active-account              Show the active account");
    System.out.println("  carry <amount>              Add funds to balance");
    System.out.println("  spent <amount> <note>       Record an expense");
    System.out.println("  balance                     Show current balance");
    System.out.println("  history                     Show expenses for current month");
    System.out.println("  help                        Show this message");
    System.out.println("=======================");
}
    public static void main(String[] args) {
        
        if(args.length == 0) {
            help();
        }

        try (BabyRedisClient client = new BabyRedisClient("localhost", 6379)) {
            String command = args[0].toLowerCase();

            if (command.equalsIgnoreCase("migrate")) 
                {
                    // Already done, keeping for reference, could be removed in future
                    try {
                        String balance = client.get(BALANCE_KEY);
                        client.sAdd(ACCOUNTS_KEY, "default");
                        client.set(ACTIVE_ACCOUNT_KEY, "default");
                        client.set("default:balance", balance);
                        client.delete(BALANCE_KEY);
                        System.out.println("Migration completed successfully.");
                    } catch ( BabyRedisException e) {
                        System.out.println("No existing balance found. Migration not needed.");
                    }
                    return;
                
            }

            switch (command) {
                case "create" -> {
                    String accountName = args[1];
                    client.sAdd(ACCOUNTS_KEY, accountName);
                    client.set(ACTIVE_ACCOUNT_KEY, accountName);
                    System.out.println("Account '" + accountName + "' created and set as active.");
                }

                case "switch" -> {
                    String accountName = args[1];
                    client.set(ACTIVE_ACCOUNT_KEY, accountName);
                    System.out.println("Switched to account '" + accountName + "'.");
                }

                case "list-accounts" -> {
                    var accounts = client.sMembers(ACCOUNTS_KEY);
                    if(accounts != null) {
                        System.out.println("Accounts:");
                        for(String account : accounts) {
                            System.out.println("  - " + account);
                        }
                    } else {
                        System.out.println("No accounts found.");
                    }
                }
                case "active-account" -> {
                    String activeAccount = getActiveAccount(client);
                    if(activeAccount != null) {
                        System.out.println("Active account: " + activeAccount);
                    }
                }

                case "carry"-> {
                        String amountStr = args[1];
                        double amount = Double.parseDouble(amountStr);

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
                        client.sAdd("expenses:" + period, "expense:" + timestamp);

                        client.set("expense:" + timestamp, String.format("%s|%s|%d", amountStr, "carry", timestamp));

                }

                case "spent"-> {
                        String amountStr = args[1];
                        String note = args[2];
                        double amount = Double.parseDouble(amountStr);
                        try{
                            double balance = Double.parseDouble(client.get(String.format("%s:balance", getActiveAccount(client))));
                            balance -= amount;
                            client.set(String.format("%s:balance", getActiveAccount(client)), String.valueOf(balance));
                        } catch(BabyRedisException e) {
                            System.out.println("No balance found. Please carry funds first.");
                        }

                        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

                        long timestamp = System.currentTimeMillis();

                        // Add period to set of periods (Could helper method to avoid duplication)
                        client.sAdd(PERIODS_KEY, period);
                        // Add expense to set of expenses for the period, using timestamp as unique identifier (Could helper method to avoid duplication)
                        client.sAdd("expenses:" + period, "expense:" + timestamp);

                        client.set("expense:" + timestamp, String.format("%s|%s|%d", amountStr, note, timestamp));
                }

                case "balance"-> {
                        try{
                            double balance = Double.parseDouble(client.get(String.format("%s:balance", getActiveAccount(client))));
                            System.out.printf("Current Balance for account %s: kr %.2f\n", getActiveAccount(client), balance);
                            } catch(BabyRedisException e) {
                                System.out.println("No balance found. Please carry funds first.");

                            }

                }

                case "history"-> {
                        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        var expenses = client.sMembers("expenses:" + period);
                        if(expenses == null) {
                            System.out.println("No expenses found for the current month.");
                        } else {
                            System.out.println("Expenses for account " + getActiveAccount(client) + " in " + period + ":");
                            for(String expenseKey : expenses) {
                                String expenseData = client.get(expenseKey);
                                String[] parts = expenseData.split("\\|");
                                String amount = parts[0];
                                String note = parts[1];
                                long timestamp = Long.parseLong(parts[2]);
                                LocalDate date = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000));
                                System.out.printf("- %s: kr %s (%s)\n", date, amount, note);
                            }
                        }
                }

                case "help"-> {
                        help();
                }
                default-> {
                        System.out.println("Unknown command. Use 'help' to see available commands.");

                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }



    }
}
