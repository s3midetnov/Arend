package org.arend.ext.error;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;


public class FileListErrorReporter extends ListErrorReporter implements ErrorReporter{
  final private List<GeneralError> myErrorList;

  public FileListErrorReporter() {
    myErrorList = new ArrayList<>();
    Path filePath = Path.of("/Users/artem.semidetnov/Dev/mcpArendServer/src/main/kotlin/errorList.txt");
    try {
      Files.write(
        filePath,
        "_SSSShello\n".getBytes(),
        StandardOpenOption.APPEND
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void report(GeneralError error) {
    myErrorList.add(error);
    Path filePath = Path.of("/Users/artem.semidetnov/Dev/mcpArendServer/src/main/kotlin/errorList.txt");
    try {
      if (Files.notExists(filePath)) {
        Files.createFile(filePath);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String text = error.toString() + "\n";
    try {
      Files.write(
        filePath,
        text.getBytes(),
        StandardOpenOption.APPEND
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<GeneralError> getErrorList() {
    return myErrorList;
  }

  public void reportTo(ErrorReporter errorReporter) {
    for (GeneralError error : myErrorList) {
      errorReporter.report(error);
    }
  }
}
