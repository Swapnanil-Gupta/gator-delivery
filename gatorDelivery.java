import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class gatorDelivery {
  private static Path getOutputFilePath(Path inputFilePath) {
    // Get the parent path is provided so that the output file
    // can be written to the same parent folder.
    Path parentPath = inputFilePath.getParent();
    String inputFilename = inputFilePath
        .getFileName()
        .toString()
        .split("\\.")[0];

    // Create the output file with the proper suffix.
    String outputFilename = inputFilename + "_output_file.txt";
    if (parentPath != null)
      return inputFilePath
          .getParent()
          .resolve(outputFilename);
    else
      return Paths.get(outputFilename);
  }

  private static List<String> execMethod(
      OrderManager orderManager,
      String methodName,
      List<Integer> methodArgs) {
    // Match the method name with the available methods of OrderManager
    // and pass the parsed arguments.
    switch (methodName) {
      case "createOrder":
        return orderManager.createOrder(
            methodArgs.get(0),
            methodArgs.get(1),
            methodArgs.get(2),
            methodArgs.get(3));
      case "print":
        if (methodArgs.size() == 1)
          return orderManager.print(
              methodArgs.get(0));
        else
          return orderManager.print(methodArgs.get(0), methodArgs.get(1));
      case "getRankOfOrder":
        return orderManager.getRankOfOrder(methodArgs.get(0));
      case "cancelOrder":
        return orderManager.cancelOrder(methodArgs.get(0), methodArgs.get(1));
      case "updateTime":
        return orderManager.updateTime(
            methodArgs.get(0),
            methodArgs.get(1),
            methodArgs.get(2));
      case "Quit":
        return orderManager.quit();
      default:
        System.err.println("Invalid operation: " + methodName);
        return List.of();
    }
  }

  private static List<String> processInputLines(List<String> inputLines) {
    List<String> outputLines = new ArrayList<>();
    OrderManager orderManager = new OrderManager();

    // Iterate through the input lines and process each line.
    for (String line : inputLines) {
      // Extract the method name and the parameters by finding the indices of '(' and
      // ')'.
      int openParenIndex = line.indexOf('(');
      String methodName = line.substring(0, openParenIndex);
      // Strip, remove empty strings and parse the numbers
      List<Integer> methodArgs = Stream
          .of(line.substring(openParenIndex + 1, line.length() - 1).split(","))
          .map(String::strip)
          .filter(p -> !p.isEmpty())
          .map(Integer::parseInt)
          .collect(Collectors.toList());

      try {
        // Execute the corresponding method.
        System.out.println("Executing line: " + line);
        List<String> output = execMethod(orderManager, methodName, methodArgs);
        // Add the output to the output lines to be printed.
        if (output == null || output.isEmpty())
          continue;
        outputLines.addAll(output);
      } catch (Exception e) {
        System.err.println("Could not process input line: " + line);
        e.printStackTrace();
      } finally {
        System.out.println("-------------------------------------------------------------------");
      }
    }

    return outputLines;
  }

  public static void main(String[] args) {
    if (args.length != 1)
      throw new IllegalArgumentException(
          "An input file must be specified");

    try {
      // Get the input file path from the provided args.
      Path inputFilePath = Paths.get(args[0]);
      // Get the output file path from the input file path.
      Path outputFilePath = getOutputFilePath(inputFilePath);
      // Read all the input lines.
      List<String> inputLines = Files.readAllLines(inputFilePath);
      // Process the input lines.
      List<String> outputLines = processInputLines(inputLines);
      // Write the output lines to the output file.
      Files.write(outputFilePath, outputLines);
    } catch (IOException e) {
      System.err.println("Error handling input/output files");
      e.printStackTrace();
    }
  }
}
