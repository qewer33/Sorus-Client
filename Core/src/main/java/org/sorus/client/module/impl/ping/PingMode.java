package org.sorus.client.module.impl.ping;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.sorus.client.gui.core.component.Collection;
import org.sorus.client.gui.screen.settings.components.ColorPicker;
import org.sorus.client.gui.screen.settings.components.CustomTextColor;
import org.sorus.client.gui.screen.settings.components.TextBox;
import org.sorus.client.module.Mode;
import org.sorus.client.settings.Setting;

public abstract class PingMode extends Mode {

  public abstract List<List<Pair<String, Color>>> format(int ping);

  public static class LabelPreMode extends PingMode {

    private final Setting<String> preLabel;
    private final Setting<String> postLabel;
    private final Setting<Color> labelMainColor;
    private final Setting<Color> labelExtraColor;
    private final Setting<Color> valueColor;

    public LabelPreMode() {
      this.register(preLabel = new Setting<>("preLabel", "["));
      this.register(postLabel = new Setting<>("postLabel", "]"));
      this.register(labelMainColor = new Setting<>("labelMainColor", Color.WHITE));
      this.register(labelExtraColor = new Setting<>("labelExtraColor", Color.WHITE));
      this.register(valueColor = new Setting<>("valueColor", Color.WHITE));
    }

    @Override
    public List<List<Pair<String, Color>>> format(int ping) {
      return new ArrayList<>(
          Collections.singletonList(
              new ArrayList<>(
                  Arrays.asList(
                      Pair.of(this.preLabel.getValue(), this.labelExtraColor.getValue()),
                      Pair.of("Ping", this.labelMainColor.getValue()),
                      Pair.of(this.postLabel.getValue(), this.labelExtraColor.getValue()),
                      Pair.of(" " + ping, this.valueColor.getValue())))));
    }

    @Override
    public String getName() {
      return "Pre Label";
    }

    @Override
    public void addConfigComponents(Collection collection) {
      collection.add(new TextBox(preLabel, "Pre Label"));
      collection.add(new TextBox(postLabel, "Post Label"));
      collection.add(new ColorPicker(labelMainColor, "Label Color"));
      collection.add(new ColorPicker(labelExtraColor, "Label Extra Color"));
      collection.add(new ColorPicker(valueColor, "Value Color"));
    }
  }

  public static class LabelPostMode extends PingMode {

    private final Setting<Color> labelMainColor;
    private final Setting<Color> valueColor;

    public LabelPostMode() {
      this.register(labelMainColor = new Setting<>("labelMainColor", Color.WHITE));
      this.register(valueColor = new Setting<>("valueColor", Color.WHITE));
    }

    @Override
    public List<List<Pair<String, Color>>> format(int ping) {
      return new ArrayList<>(
          Collections.singletonList(
              new ArrayList<>(
                  Arrays.asList(
                      Pair.of(ping + " ", this.valueColor.getValue()),
                      Pair.of("ms", this.labelMainColor.getValue())))));
    }

    @Override
    public String getName() {
      return "Post Label";
    }

    @Override
    public void addConfigComponents(Collection collection) {
      collection.add(new ColorPicker(labelMainColor, "Label Color"));
      collection.add(new ColorPicker(valueColor, "Value Color"));
    }
  }

  public static class CustomMode extends PingMode {

    private final Setting<List<List<Pair<String, Color>>>> text;

    public CustomMode() {
      this.register(
          text =
              new Setting<>(
                  "text",
                  new ArrayList<>(
                      Collections.singletonList(
                          new ArrayList<>(
                              Collections.singletonList(Pair.of("Ping: $PING", Color.WHITE)))))));
    }

    @Override
    public List<List<Pair<String, Color>>> format(int ping) {
      List<List<Pair<String, Color>>> formattedList = new ArrayList<>();
      for (List<Pair<String, Color>> lineList : this.text.getValue()) {
        List<Pair<String, Color>> formattedLine = new ArrayList<>();
        for (Pair<String, Color> pair : lineList) {
          formattedLine.add(
              Pair.of(pair.getLeft().replace("$PING", String.valueOf(ping)), pair.getRight()));
        }
        formattedList.add(formattedLine);
      }
      return formattedList;
    }

    @Override
    public String getName() {
      return "Custom";
    }

    @Override
    public void addConfigComponents(Collection collection) {
      collection.add(new CustomTextColor(text, "Custom Text"));
    }
  }
}
