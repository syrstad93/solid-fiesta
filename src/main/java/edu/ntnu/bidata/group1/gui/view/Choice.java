package edu.ntnu.bidata.group1.gui.view;

import java.util.List;
import java.util.Objects;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

/**
 * One entry in a list or drop-down: an id, such as a device or room id, and the text shown for
 * it.
 *
 * @param id the id
 * @param label the text shown
 */
public record Choice(long id, String label) {

  @Override
  public String toString() {
    return label;
  }

  /**
   * Replaces the items of a drop-down, keeping the selected id selected. Does nothing if the
   * items are unchanged, so an open drop-down is not disturbed.
   *
   * @param box the drop-down
   * @param items the new items
   */
  public static void setItems(ComboBox<Choice> box, List<Choice> items) {
    if (box.getItems().equals(items)) {
      return;
    }
    Choice selected = box.getValue();
    box.getItems().setAll(items);
    box.setValue(selected == null ? null : find(items, selected.id()));
  }

  /**
   * Replaces the items of a list, keeping the selected id selected. Does nothing if the items
   * are unchanged.
   *
   * @param list the list
   * @param items the new items
   */
  public static void setItems(ListView<Choice> list, List<Choice> items) {
    if (list.getItems().equals(items)) {
      return;
    }
    Choice selected = list.getSelectionModel().getSelectedItem();
    list.getItems().setAll(items);
    Choice again = selected == null ? null : find(items, selected.id());
    if (again != null) {
      list.getSelectionModel().select(again);
    }
  }

  private static Choice find(List<Choice> items, long id) {
    return items.stream().filter(c -> Objects.equals(c.id(), id)).findFirst().orElse(null);
  }
}
