package com.mazebingo;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MazeEventNotificationOverlay {

    private static final Logger log = LoggerFactory.getLogger(MazeEventNotificationOverlay.class);

    private static final int RESIZABLE_CLASSIC_LAYOUT   = (InterfaceID.TOPLEVEL_OSRS_STRETCH << 16) | 13;
    private static final int RESIZABLE_MODERN_LAYOUT    = (InterfaceID.TOPLEVEL_PRE_EOC << 16) | 13;
    private static final int FIXED_CLASSIC_LAYOUT       = (InterfaceID.TOPLEVEL << 16) | 42;
    private static final int NOTIFICATION_DISPLAY_PANEL = (InterfaceID.NOTIFICATION_DISPLAY << 16) | 1;
    // No ScriptID constant exists for this script; it initialises the notification popup widget content.
    private static final int SCRIPT_NOTIFICATION_POPUP_INIT = 3343;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private AudioPlayer audioPlayer;

    private WidgetNode popupWidgetNode;
    private final List<String> queue = new ArrayList<>();

    public synchronized void shutdown() {
        queue.clear();
        if (popupWidgetNode != null) {
            try {
                client.closeInterface(popupWidgetNode, true);
            } catch (Exception ignored) {
            }
            popupWidgetNode = null;
        }
    }

    public synchronized void addNotification(String message, Color ignored) {
        queue.add(message);
        if (queue.size() == 1) {
            showPopup(message);
        }
    }

    private void showPopup(String message) {
        clientThread.invokeLater(() -> {
            try {
                int componentId = client.isResized()
                    ? client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
                        ? RESIZABLE_MODERN_LAYOUT
                        : RESIZABLE_CLASSIC_LAYOUT
                    : FIXED_CLASSIC_LAYOUT;

                popupWidgetNode = client.openInterface(componentId, InterfaceID.NOTIFICATION_DISPLAY, WidgetModalMode.MODAL_CLICKTHROUGH);
                client.runScript(SCRIPT_NOTIFICATION_POPUP_INIT, "Maze Race Bingo", message, -1);

                String lowerMsg = message.toLowerCase();
                MazeSound sound = lowerMsg.contains("completed the end tile") ? MazeSound.BOBER
                    : lowerMsg.contains("has found a key") ? MazeSound.WHIP
                    : lowerMsg.contains("keys") ? MazeSound.SAD_SOUND:
                    MazeSound.SHORT_DOG_BARK;
                InputStream stream = SoundGenerator.generate(sound);
                if (stream != null) {
                    try {
                        audioPlayer.play(stream, 0f);
                    } catch (Exception ex) {
                        log.warn("Failed to play notification sound", ex);
                    }
                }

                clientThread.invokeLater(this::tryClearMessage);
            } catch (IllegalStateException ex) {
                clientThread.invokeLater(this::tryClearMessage);
            }
        });
    }

    private synchronized boolean tryClearMessage() {
        Widget w = client.getWidget(NOTIFICATION_DISPLAY_PANEL);

        if (w != null && w.getWidth() > 0) {
            return false;
        }

        try {
            client.closeInterface(popupWidgetNode, true);
        } catch (Exception ex) {
            // ignored
        }
        popupWidgetNode = null;
        queue.remove(0);

        if (!queue.isEmpty()) {
            clientThread.invokeLater(() -> {
                showPopup(queue.get(0));
                return true;
            });
        }
        return true;
    }
}
