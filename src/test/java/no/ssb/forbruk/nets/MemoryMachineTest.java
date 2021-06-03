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
                if (line1.startsWith("01/02") && !data.startsWith("01/02")) {
                    System.out.println(line1+data);
                } else if (line1.startsWith("01/02") && data.startsWith("01/02")){

                    System.out.println(line1);
                } else {
                    //DO nothing
                }
                line1=data;

            } while(myReader.hasNextLine());

            // process last line
            System.out.println(line1);

            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
