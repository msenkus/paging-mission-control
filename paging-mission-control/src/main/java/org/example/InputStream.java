package org.example;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class InputStream {
    private int lineIndex = 0;

    public InputStream(){
    }

    // Import Text from file.
    public String nextLine() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/test"));
            //This method for steaming data is only for testing.
            String inputLine = reader.readLine();
            for(int i = 0; i < lineIndex; i++){
                inputLine = reader.readLine();
            }
            reader.close();
            lineIndex++;
            return inputLine;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
