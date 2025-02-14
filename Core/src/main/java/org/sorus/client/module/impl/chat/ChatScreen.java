package org.sorus.client.module.impl.chat;

import java.awt.*;
import org.sorus.client.Sorus;
import org.sorus.client.event.EventInvoked;
import org.sorus.client.event.impl.client.input.KeyPressEvent;
import org.sorus.client.gui.core.Screen;
import org.sorus.client.gui.core.font.IFontRenderer;
import org.sorus.client.util.MathUtil;
import org.sorus.client.version.IGLHelper;
import org.sorus.client.version.game.IGame;
import org.sorus.client.version.input.IInput;

public class ChatScreen extends Screen {

  private final ChatComponent chatComponent;
  private String message = "";

  private double targetScroll;

  public ChatScreen(ChatComponent chatComponent) {
    this.chatComponent = chatComponent;
    this.chatComponent.setScreenOpen(true);
    Sorus.getSorus().getEventManager().register(this);
  }

  @Override
  public void onRender() {
    final int FPS = Math.max(Sorus.getSorus().getVersion().getData(IGame.class).getFPS(), 1);
    double scrollValue = Sorus.getSorus().getVersion().getData(IInput.class).getScroll();
    double scale = this.chatComponent.getScale();
    targetScroll = targetScroll + scrollValue * 0.1;
    chatComponent
        .getChatScroll()
        .setScroll(
            (targetScroll - chatComponent.getChatScroll().getScroll()) * 7 / FPS
                + chatComponent.getChatScroll().getScroll());
    chatComponent
        .getChatScroll()
        .addMinMaxScroll(
            0, Math.max(0, chatComponent.getChatHeight() - chatComponent.getHeight() + 7));
    targetScroll =
        MathUtil.clamp(
            targetScroll,
            chatComponent.getChatScroll().getMinScroll(),
            chatComponent.getChatScroll().getMaxScroll());
    Sorus.getSorus()
        .getGUIManager()
        .getRenderer()
        .drawRect(
            chatComponent.getInputX(),
            chatComponent.getInputY(),
            this.chatComponent.getChatWidth().getValue() * scale,
            7 * scale,
            new Color(0, 0, 0, 50));
    IFontRenderer fontRenderer =
        Sorus.getSorus().getGUIManager().getRenderer().getRubikFontRenderer();
    Sorus.getSorus()
        .getVersion()
        .getData(IGLHelper.class)
        .beginScissor(
            chatComponent.getInputX() + 2,
            chatComponent.getInputY() + 0.5,
            this.chatComponent.getChatWidth().getValue() * scale,
            8 * scale);
    Sorus.getSorus()
        .getGUIManager()
        .getRenderer()
        .drawString(
            fontRenderer,
            message + ((System.currentTimeMillis() % 1000 > 500) ? "_" : ""),
            chatComponent.getInputX()
                + 2
                - ((fontRenderer.getStringWidth(message + "__") / 2
                        > this.chatComponent.getChatWidth().getValue())
                    ? Math.abs(
                        fontRenderer.getStringWidth(message + "__") / 2
                            - this.chatComponent.getChatWidth().getValue())
                    : 0),
            chatComponent.getInputY() + 4,
            0.5 * scale,
            0.5 * scale,
            true,
            Color.WHITE);
    Sorus.getSorus().getVersion().getData(IGLHelper.class).endScissor();
  }

  @EventInvoked
  public void onKeyPress(KeyPressEvent e) {
    char character = e.getCharacter();
    switch (e.getKey()) {
      case ENTER:
        if (!message.isEmpty()) {
          Sorus.getSorus().getVersion().getData(IGame.class).sendChatMessage(message);
        }
      case ESCAPE:
        Sorus.getSorus().getGUIManager().close(this);
        break;
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return;
      case BACKSPACE:
        if (!message.isEmpty()) {
          this.message = message.substring(0, message.length() - 1);
        }
        break;
      default:
        this.message = message + character;
        break;
    }
    if (message.length() > 100) {
      message = message.substring(0, 100);
    }
  }

  @Override
  public void onExit() {
    this.chatComponent.setScreenOpen(false);
    Sorus.getSorus().getEventManager().unregister(this);
  }

  @Override
  public boolean shouldOpenBlank() {
    return true;
  }
}
