package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Performs unit tests for the {@link MessageWorker} class.
 */
public class MessageWorkerTest  {

  @Test
  public void noArgsConstructor_allFieldsNull() {
    MessageWorker mw = new MessageWorker();

    assertNull(mw.getMessage());
    assertNull(mw.getSource());
    assertNull(mw.getDestination());
    assertNull(mw.getCommand());
    assertNull(mw.getSourceId());
    assertNull(mw.getDestinationId());
    assertNull(mw.getSourcePrefix());
    assertNull(mw.getDestinationPrefix());
  }

  @Test
  public void constructorWithMessage_allFieldsSet() {
    MessageWorker mw = new MessageWorker("CH01/SE02/TEST/=");

    assertEquals("CH01/SE02/TEST/=", mw.getMessage());
    assertEquals("SE02", mw.getSource());
    assertEquals("CH01", mw.getDestination());
    assertEquals("TEST", mw.getCommand());
    assertEquals("SE", mw.getSourcePrefix());
    assertEquals("CH", mw.getDestinationPrefix());
    assertEquals(2, mw.getSourceId());
    assertEquals(1, mw.getDestinationId());

    assertTrue(mw.isMessageComplete());
  }

  @Test
  public void changingAnIndividualPartOfTheMessageUpdatesOnlyThatPart() {
    MessageWorker mw = new MessageWorker("CH01/SE02/TEST/=");

    mw.setSource("SE03").buildMessage();
    assertEquals("SE03", mw.getSource());
    assertEquals(3, mw.getSourceId());
    assertEquals("CH01/SE03/TEST/=", mw.getMessage());

    mw.setDestination("CH04").buildMessage();
    assertEquals("CH04", mw.getDestination());
    assertEquals(4, mw.getDestinationId());
    assertEquals("CH04/SE03/TEST/=", mw.getMessage());

    mw.setCommand("NEW_COMMAND").buildMessage();
    assertEquals("NEW_COMMAND", mw.getCommand());
    assertEquals("CH04/SE03/NEW_COMMAND/=", mw.getMessage());
  }

  @Test
  void constructorRejectsNullMessage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MessageWorker(null)
    );
  }

  @Test
  void setMessageRejectsNullMessage() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setMessage(null)
    );
  }

  @Test
  void setMessageRejectsMessageWithoutValidPrefix() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setMessage("XX01/YY02/TEST/= ")
    );
  }

  @Test
  void setMessageRejectsWrongNumberOfSeparators() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setMessage("CH01/SE02/TEST=")
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setMessage("CH01/SE02/TEST/EXTRA/=")
    );
  }

  @Test
  void setMessageRejectsMissingEndMarker() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setMessage("CH01/SE02/TEST/")
    );
  }

  @Test
  void setDestinationRejectsNullString() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setDestination((String) null)
    );
  }

  @Test
  void setDestinationRejectsNullPrefix() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setDestination(null, 1)
    );
  }

  @Test
  void setSourceRejectsNullString() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setSource((String) null)
    );
  }

  @Test
  void setSourceRejectsNullPrefix() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setSource(null, 1)
    );
  }

  @Test
  void setCommandRejectsNullCommand() {
    MessageWorker mw = new MessageWorker();

    assertThrows(
        IllegalArgumentException.class,
        () -> mw.setCommand(null)
    );
  }
}
