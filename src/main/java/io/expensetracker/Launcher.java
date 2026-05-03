package io.expensetracker;

import java.time.LocalDate;

import io.babyredis.client.BabyRedisClient;
import io.expensetracker.data.Expense;

public class Launcher {

    public static void main(String[] args) {
        if (args.length == 0) {
            Launcher.help();
            return;
        }

        try {
            BabyRedisClient client = new BabyRedisClient("localhost", 6379);
            ExpenseTracker expenseTracker = new ExpenseTracker(client);
            String command = args[0].toLowerCase();

            if (command.equalsIgnoreCase("migrate")) {
                expenseTracker.migrate();
                System.out.println("Migration completed successfully.");
                return;
            }

            switch (command) {

                // Account management commands
                case "create" -> {
                    String accountName = args[1];
                    expenseTracker.createAccount(accountName);
                    System.out.println("Account '" + accountName + "' created and set as active.");
                }
                case "switch" -> {
                    String accountName = args[1];
                    expenseTracker.switchAccount(accountName);
                    System.out.println("Switched to account '" + accountName + "'.");
                }
                case "list-accounts" -> {
                    var accounts = expenseTracker.getAccounts();
                    if (accounts != null) {
                        System.out.println("Accounts:");
                        for (String account : accounts) {
                            System.out.println("  - " + account);
                        }
                    } else {
                        System.out.println("No accounts found.");
                    }
                }
                case "active-account" -> {
                    String activeAccount = expenseTracker.getActiveAccount(client);
                    if (activeAccount != null) {
                        System.out.println("Active account: " + activeAccount);
                    }
                }

                // Balance and expense commands
                case "carry" -> {
                    double amount = Double.parseDouble(args[1]);
                    expenseTracker.logDeposit(amount);
                    System.out.printf("Added kr %.2f to balance of account %s.\n", amount, expenseTracker.getActiveAccount(client));
                }
                case "spent" -> {
                    double amount = Double.parseDouble(args[1]);
                    String note = args[2];
                    expenseTracker.logExpense(amount, note);
                    System.out.printf("Logged expense of kr %.2f with note '%s' for account %s.\n", amount, note, expenseTracker.getActiveAccount(client));
                }
                case "balance" -> {
                    double balance = expenseTracker.getBalance();
                    System.out.printf("Current balance for account %s: kr %.2f\n", expenseTracker.getActiveAccount(client), balance);
                }

                // History command
                case "history" -> {
                    var expenses = expenseTracker.getHistory();
                    if (expenses.isEmpty()) {
                        System.out.println("No expenses found for the current month.");
                    } else {
                        System.out.println("Expenses for account " + expenseTracker.getActiveAccount(client) + " in " + expenseTracker.getCurrentPeriod() + ":");
                        for (Expense expense : expenses) {
                            
                            LocalDate date = LocalDate.ofEpochDay(expense.getTimestamp() / (24 * 60 * 60 * 1000));
                            
                            System.out.printf("- %s: kr %s (%s)\n", date, expense.getAmount(), expense.getNote());
                        }
                    }
                }

                //Admin / Cleanup commands
                case "correct-balance" -> {

                    if(args.length > 2 && args[2].equalsIgnoreCase("--confirm")){
                        double balance = Double.parseDouble(args[1]);
                        expenseTracker.correctBalance(balance);
                        System.out.printf("Balance for account %s corrected to kr %.2f\n", expenseTracker.getActiveAccount(client), balance);
                
                    }else{
                        System.out.println("This command will directly set the balance for the active account. Use with caution.");
                        System.out.println("To confirm, run the command again with the --confirm flag.");
            
                    }
                }

                case "delete-account" -> {
                    String accountName = args[1];
                    if(args.length > 2 && args[2].equalsIgnoreCase("--confirm")){
                        if(args.length > 3 && args[3].equalsIgnoreCase("--delete-history")){
                            expenseTracker.deleteAccount(accountName, true);
                            System.out.println("Account '" + accountName + "' and all associated history deleted.");
                        }else{
                            expenseTracker.deleteAccount(accountName, false);
                            System.out.println("Account '" + accountName + "' deleted. Associated history has been retained.");
                        }
                    }else{

                        System.out.println("This command will permanently delete the account and all associated data. Use with caution.");
                        System.out.println("To confirm, run the command again with the --confirm flag.");
                    }
                }

                // Help command
                case "help" -> {
                    Launcher.help();
                }
                default -> {
                    System.out.println("Unknown command. Use 'help' to see available commands.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
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
}
