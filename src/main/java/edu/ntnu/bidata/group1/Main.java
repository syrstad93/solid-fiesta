package edu.ntnu.bidata.group1;

import edu.ntnu.bidata.group1.network.server.Server;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Main {

  public static void main(String[] args)  {
    try {
      Server server = Server.getInstance();
      server.start(new CountDownLatch(1));
    } catch (IllegalArgumentException e)  {
      System.out.println("Error occurred. Please contact support.\n"
          + "Error description:\n"
          + "Server port number rejected. Rejected port number: "
          + UniversallyAvailableInformation.PORT_NUMBER);
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("Significant error occurred. Please contact support.");
      e.printStackTrace();
    }
  }
}
