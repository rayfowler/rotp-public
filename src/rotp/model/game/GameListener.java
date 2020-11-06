package rotp.model.game;

import rotp.ui.notifications.TurnNotification;

import java.util.List;

/**
 * Callbacks for game state changes
 */
public interface GameListener {
    default void clearAdvice() {
    }
    default void processNotifications(List<TurnNotification> notifications) {
    }
    default void allocateSystems() {
    }
}
