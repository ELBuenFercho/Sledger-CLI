import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LedgerCLICopycat {
  // Map of commands to their corresponding implementation
  private static final Map<String, Runnable> COMMANDS;

  // Map of options to their corresponding values
  private static final Map<String, String> OPTIONS;

  static {
    COMMANDS = new HashMap<>();
    COMMANDS.put("balance", LedgerCLICopycat::printBalance);
    COMMANDS.put("bal", LedgerCLICopycat::printBalance);
    COMMANDS.put("register", LedgerCLICopycat::printRegister);
    COMMANDS.put("reg", LedgerCLICopycat::printRegister);
    COMMANDS.put("print", LedgerCLICopycat::printTransactions);

    OPTIONS = new HashMap<>();
  }

  public static void main(String[] args) {
    // Parse options and arguments from command line
    String command = null;
    for (String arg : args) {
      if (arg.startsWith("--")) {
        // Option
        int equalsIndex = arg.indexOf('=');
        if (equalsIndex == -1) {
          // Flag option
          OPTIONS.put(arg, null);
        } else {
          // Value option
          String option = arg.substring(0, equalsIndex);
          String value = arg.substring(equalsIndex + 1);
          OPTIONS.put(option, value);
        }
      } else {
        // Command or argument
        if (command == null) {
          command = arg;
        } else {
          System.err.println("Unrecognized argument: " + arg);
          System.exit(1);
        }
      }
    }

    if (command == null) {
      // No command specified
      System.err.println("No command specified");
      System.exit(1);
    }

    // Get ledger file
    File ledgerFile = null;
    String ledgerFilePath = OPTIONS.get("--file");
    if (ledgerFilePath != null) {
      ledgerFile = new File(ledgerFilePath);
    } else {
      // Use default ledger file
      String homeDir = System.getProperty("user.home");
      ledgerFile = new File(homeDir, ".ledger");
      if (!ledgerFile.exists()) {
        ledgerFile = new File("ledger.dat");
      }
    }

    // Get price database file
    File priceDbFile = null;
    String priceDbFilePath = OPTIONS.get("--price-db");
    if (priceDbFilePath != null) {
      priceDbFile = new File(priceDbFilePath);
    } else {
      // Use default price database file
      String homeDir = System.getProperty("user.home");
      priceDbFile = new File(homeDir, ".pricedb");
    }

    // Get sort option
    boolean sort = OPTIONS.containsKey("--sort");

    // Execute command
    Runnable commandImpl = COMMANDS.get(command);
    if (commandImpl == null) {
      // Unrecognized command
      System.err.println("Unrecognized command: " + command);
      System.exit(1);
    }
    // Execute command
    commandImpl.run();
  }

  private static void printBalance() {
    // Read ledger file
    List<Transaction> transactions = readTransactions(ledgerFile);

    // Read price database file
    Map<String, Price> prices = readPrices(priceDbFile);

    // Calculate balance
    Map<String, Long> balance = new HashMap<>();
    for (Transaction t : transactions) {
      long amount = t.amount;
      if (t.commodity != null && prices.containsKey(t.commodity)) {
        // Adjust amount for inflation using price in price database file
        amount = prices.get(t.commodity).convert(amount, t.commodity, "USD");
      }
      Long currentAmount = balance.get(t.account);
      if (currentAmount == null) {
        currentAmount = 0L;
      }
      balance.put(t.account, currentAmount + amount);
    }

    // Display balance
    System.out.println("Balance:");
    List<String> accountNames = new ArrayList<>(balance.keySet());
    if (sort) {
      Collections.sort(accountNames);
    }
    for (String account : accountNames) {
      long amount = balance.get(account);
      System.out.printf("  %-30s %10d\n", account, amount);
    }
  }

  private static void printRegister() {
     // Read ledger file
    List<Transaction> transactions = readTransactions(ledgerFile);

    // Read price database file
    Map<String, Price> prices = readPrices(priceDbFile);

    // Display transactions in register format
    System.out.println("Register:");
    for (Transaction t : transactions) {
      String date = t.date;
      String payee = t.payee;
      String account = t.account;
      String commodity = t.commodity;
      long amount = t.amount;
      String note = t.note;
      if (commodity != null && prices.containsKey(commodity)) {
        // Adjust amount for inflation using price in price database file
        amount = prices.get(commodity).convert(amount, commodity, "USD");
      }
      System.out.printf("%s %-20s %-30s %10d %s\n", date, payee, account, amount, note);
    }
    System.out.println("Displaying balance of accounts in ledger file");
  }

    private static void printTransactions() {
    // Read ledger file
    List<Transaction> transactions = readTransactions(ledgerFile);

    // Read price database file
    Map<String, Price> prices = readPrices(priceDbFile);

    // Display transactions in human-readable format
    System.out.println("Transactions:");
    for (Transaction t : transactions) {
      String date = t.date;
      String payee = t.payee;
      String account = t.account;
      String commodity = t.commodity;
      long amount = t.amount;
      String note = t.note;
      if (commodity != null && prices.containsKey(commodity)) {
        // Adjust amount for inflation using price in price database file
        amount = prices.get(commodity).convert(amount, commodity, "USD");
      }
      System.out.printf("%s %-20s %10d %s\n", date, payee, amount, note);
    }
  }

}