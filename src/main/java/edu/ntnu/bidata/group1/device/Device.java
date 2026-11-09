package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.network.server.Messenger;
import java.io.IOException;

public abstract class Device implements Messenger<String, ClientConnectionHolder> {
  public ClientConnectionHolder connection;
  private long Id;

  public Device() throws IOException, InterruptedException {
    this.connection = new ClientConnectionHolder<>(this);
  }

  public void setId(long id) {
    Id = id;
  }

  public long getId() {
    return Id;
  }

  @Override
  public void onDisconnect(ClientConnectionHolder client) {

  }
}
