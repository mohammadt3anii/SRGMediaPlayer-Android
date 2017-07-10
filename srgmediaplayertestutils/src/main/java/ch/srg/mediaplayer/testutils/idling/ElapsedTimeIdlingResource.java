package ch.srg.mediaplayer.testutils.idling;

import android.support.test.espresso.IdlingResource;
import android.util.Log;

/**
 * Created by npietri on 30.06.16.
 */
public class ElapsedTimeIdlingResource implements IdlingResource {
    private static final String TAG = "ElapsedTimeIdling";

    private final long startTime;
    private final long waitingTime;
    private ResourceCallback resourceCallback;

    public ElapsedTimeIdlingResource(long waitingTime) {
        this.startTime = System.currentTimeMillis();
        this.waitingTime = waitingTime;
    }

    @Override
    public String getName() {
        return ElapsedTimeIdlingResource.class.getName() + ":" + startTime +":" + waitingTime;
    }

    @Override
    public boolean isIdleNow() {
        long elapsed = System.currentTimeMillis() - startTime;
        boolean idle = (elapsed >= waitingTime);
        if (idle) {
            Log.d(TAG, "waiting done, transition to idle.");
            resourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }
}
