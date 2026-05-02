package io.expensetracker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.babyredis.client.BabyRedisClient;
import io.babyredis.error.BabyRedisException;

public class ExpenseTracker {

    public static final String BALANCE_KEY = "balance";
    public static final String PERIODS_KEY = "periods";

public static void help() {
    System.out.println("=== Expense Tracker ===");
    System.out.println("Usage:");
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

            switch (command) {
                case "carry"-> {
                        String amountStr = args[1];
                        double amount = Double.parseDouble(amountStr);

                        try{
                                double balance = Double.parseDouble(client.get(BALANCE_KEY));
                                balance += amount;
                                client.set(BALANCE_KEY, String.valueOf(balance));
                            } catch(BabyRedisException e) {
                                client.set(BALANCE_KEY, String.valueOf(amount));
}
                        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

                        // Add period to set of periods (Could helper method to avoid duplication)
                        client.sAdd(PERIODS_KEY, period);
                        // Add expense to set of expenses for the period, using timestamp as unique identifier (Could helper method to avoid duplication)
                        long timestamp = System.currentTimeMillis();
                        client.sAdd("expenses:" + period, "expense:" + timestamp);

                        client.set("expense" + period, String.format("%s|%s|%d", amountStr, "carry", timestamp));

                }

                case "spent"-> {
                        String amountStr = args[1];
                        String note = args[2];
                        double amount = Double.parseDouble(amountStr);
                        String balanceStr = client.get(BALANCE_KEY);
                        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

                        long timestamp = System.currentTimeMillis();

                        // Add period to set of periods (Could helper method to avoid duplication)
                        client.sAdd(PERIODS_KEY, period);
                        // Add expense to set of expenses for the period, using timestamp as unique identifier (Could helper method to avoid duplication)
                        client.sAdd("expenses:" + period, "expense:" + timestamp);

                        client.set("expense" + period, String.format("%s|%s|%d", amountStr, note, timestamp));
                        if(balanceStr.contains("ERR Not found")) {
                            System.out.println("No balance found. Please carry funds first.");
                        } else {
                            double balance = Double.parseDouble(balanceStr);
                            balance -= amount;
                            client.set("balance", String.valueOf(balance));
                        }
                }

                case "balance"-> {
                        String balanceStr = client.get("balance");
                        if(balanceStr.contains("ERR Not found")) {
                            System.out.println("No balance found. Please carry funds first.");
                        } else {
                            double balance = Double.parseDouble(balanceStr);
                            System.out.printf("Current Balance: kr %.2f\n", balance);
                        }

                }

                case "history"-> {
                        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                        var expenses = client.sMembers("expenses:" + period);
                        if(expenses == null) {
                            System.out.println("No expenses found for the current month.");
                        } else {
                            System.out.println("Expenses for " + period + ":");
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
