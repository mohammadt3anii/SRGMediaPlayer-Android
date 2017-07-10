package ch.srg.mediaplayer.testutils.condition;

import android.support.annotation.NonNull;

import ch.srg.mediaplayer.SRGMediaPlayerController;

/**
 * Copyright (c) SRG SSR. All rights reserved.
 * <p>
 * License information is available from the LICENSE file.
 */
public class PlayerStateInstruction extends Instruction implements SRGMediaPlayerController.Listener {

    private final SRGMediaPlayerController.Event.Type eventToWait;
    private boolean conditionChecked;

    public PlayerStateInstruction(@NonNull SRGMediaPlayerController.Event.Type eventToWait) {
        this.eventToWait = eventToWait;
        SRGMediaPlayerController.registerGlobalEventListener(this);
    }

    @Override
    public String getDescription() {
        return "Wait for player event: " + eventToWait;
    }

    @Override
    public boolean checkCondition() {
        return conditionChecked;
    }

    @Override
    public void onMediaPlayerEvent(SRGMediaPlayerController mp, SRGMediaPlayerController.Event event) {
        if (mp != null) {
            if (eventToWait.equals(event.type)) {
                SRGMediaPlayerController.unregisterGlobalEventListener(this);
                conditionChecked = true;
            }
        }
    }
}
