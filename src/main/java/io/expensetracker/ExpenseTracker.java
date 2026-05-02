package io.expensetracker;

public class ExpenseTracker {

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

        

    }
}
