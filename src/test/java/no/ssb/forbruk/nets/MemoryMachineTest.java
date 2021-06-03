package no.ssb.forbruk.nets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

@ExtendWith(SpringExtension.class)
public class MemoryMachineTest {
    @Test
    void Test(){
        try {
            File myObj = new File(System.getProperty("user.dir")+"/src/test/resources/testNetsResponse.csv");
            Scanner myReader = new Scanner(myObj);
            String line1 = myReader.nextLine();
            do {
                String data = myReader.nextLine();
                if (!data.startsWith("01/02")) {

                } else {
                    System.out.println(line1+data);
                }

            } while(myReader.hasNextLine());
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
