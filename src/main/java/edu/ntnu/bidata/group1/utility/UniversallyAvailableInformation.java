package edu.ntnu.bidata.group1.utility;

/**
 * A utility class that contains information that should be available to all parts of the code.
 *
 * <p>
 *   This class is somewhat artificial in the sense that in a real life scenario, the different
 *   parts of the code that uses the information from here would get the information in other ways.
 *   However, since we have not implemented every real life component, we have made this class to
 *   provide the necessary information.
 * </p>
 * <p>
 *   For example: Connection between sockets would, in real life, use things like DNS-servers and
 *   IP-addresses to find the correct destination. But in this project, we simply
 *   provide a port number in this class and use localhost.
 * </p>
 */
public abstract class UniversallyAvailableInformation {

  public static int PORT_NUMBER = 1238;
}
