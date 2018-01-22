package ch.srg.mediaplayer;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Axel on 09/03/2015.
 * <p>
 * <p>
 * TODO Check that the following is no longer a problem
 * <p>
 * Problematic use cases:
 * SRGMediaPlayerView.applyOverlayMode(playerControlView, SRGMediaPlayerView.LayoutParams.OVERLAY_CONTROL);
 * playerControlView.setVisibility(View.VISIBLE);
 * -> the second visibility should not have any impact. the overlay mode itself should be applied directly
 * <p>
 * The potential race condition is when the app does:
 * A) play.showControls()
 * B) SRGMediaPlayerView.applyOverlayMode(playerControlView, SRGMediaPlayerView.LayoutParams.OVERLAY_CONTROL);
 * The order of execution of A / B will have an impact on whether the player controls are displayed or not, this is bad.
 */
/*package*/ class OverlayController implements ControlTouchListener, SRGMediaPlayerController.Listener, Handler.Callback {

    private static final String TAG = SRGMediaPlayerController.TAG;

    private static final int MSG_HIDE_CONTROLS = 1;

    static final int DEFAULT_AUTO_HIDE_DELAY = 3000;
    private static int overlayAutoHideDelay = DEFAULT_AUTO_HIDE_DELAY;

    @Nullable
    private SRGMediaPlayerView videoContainer;
    @NonNull
    private final SRGMediaPlayerController playerController;

    private boolean showingControlOverlays = true;
    private boolean showingLoadings = true;

    private final Handler handler = new Handler(this);
    @Nullable
    private Boolean loadingForced;
    @Nullable
    private Boolean controlsForced;

    private boolean doesPlayerStateRequiresControls() {
        SRGMediaPlayerController.State state = playerController.getState();

        boolean notPlaying = !playerController.isPlaying();
        boolean paused = state == SRGMediaPlayerController.State.READY && notPlaying;
        boolean remote = playerController.isRemote();

        return remote || paused || playerController.isReleased();
    }

    OverlayController(@NonNull SRGMediaPlayerController playerController) {
        this.playerController = playerController;
        showControlOverlays();
        updateWithPlayer();
    }

    @Override
    public void onMediaControlTouched() {
        if (!showingControlOverlays) {
            showControlOverlays();
        } else {
            postponeControlsHiding();
        }
    }

    @Override
    public void onMediaControlBackgroundTouched() {
        if (!showingControlOverlays) {
            showControlOverlays();
        } else {
            hideControlOverlaysImmediately();
        }
    }

    /**
     * Bind the SRGMediaPlayerView touch event to the overlayController for handle touch and switch
     * visibility of views if necessary.
     *
     * @param videoContainer video container to bind to
     */
    void bindToVideoContainer(SRGMediaPlayerView videoContainer) {
        if (videoContainer == this.videoContainer) {
            return;
        }
        if (this.videoContainer != null) {
            this.videoContainer.setControlTouchListener(null);
        }
        this.videoContainer = videoContainer;
        if (videoContainer != null) {
            videoContainer.setControlTouchListener(this);
        }
        propagateOverlayVisibility();
        updateLoadings();
    }

    private void updateLoadings() {
        if (loadingForced != null) {
            updateLoadings(loadingForced);
        } else {
            updateLoadings(playerController.isLoading());
        }
    }

    boolean isControlsVisible() {
        return showingControlOverlays;
    }

    private void setLoadingsVisibility(boolean visible) {
        if (showingLoadings != visible) {
            showingLoadings = visible;
            propagateOverlayVisibility();
        }
    }

    void showControlOverlays() {
        if (!showingControlOverlays) {
            showingControlOverlays = true;
            if (controlsForced == null) {
                playerController.broadcastEvent(SRGMediaPlayerController.Event.Type.OVERLAY_CONTROL_DISPLAYED);
            }
            propagateOverlayVisibility();
            postponeControlsHiding();
        }
    }

    void hideControlOverlaysImmediately() {
        if (!doesPlayerStateRequiresControls()) {
            if (showingControlOverlays) {
                showingControlOverlays = false;
                if (controlsForced == null) {
                    playerController.broadcastEvent(SRGMediaPlayerController.Event.Type.OVERLAY_CONTROL_HIDDEN);
                }
                handler.removeMessages(MSG_HIDE_CONTROLS);
                propagateOverlayVisibility();
            }
        }
    }

    @Override
    public void onMediaPlayerEvent(SRGMediaPlayerController mp, SRGMediaPlayerController.Event event) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Invalid thread");
        }
        if (mp != playerController) {
            throw new IllegalArgumentException("Unexpected controller");
        }
        switch (event.type) {
            case STATE_CHANGE:
            case PLAYING_STATE_CHANGE:
            case WILL_SEEK:
            case DID_SEEK:
                updateWithPlayer();
                break;
            case MEDIA_COMPLETED:
                break;
        }
    }

    private void updateWithPlayer() {
        if (controlsForced == null) {
            if (doesPlayerStateRequiresControls()) {
                showControlOverlays();
            } else {
                ensureControlsHiding();
            }
        }
        if (loadingForced == null) {
            updateLoadings();
        }
    }

    private void updateLoadings(boolean loading) {
        setLoadingsVisibility(loading);
    }

    private void postponeControlsHiding() {
        handler.removeMessages(MSG_HIDE_CONTROLS);
        handler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLS, overlayAutoHideDelay);
    }

    private void ensureControlsHiding() {
        if (showingControlOverlays && !handler.hasMessages(MSG_HIDE_CONTROLS)) {
            postponeControlsHiding();
        }
    }

    void propagateOverlayVisibility() {
        if (playerController.isDebugMode()) {
            Log.v(TAG, "visibility: " + showingControlOverlays + ", " + controlsForced + " | " + showingLoadings + ", " + loadingForced + " for " + (videoContainer != null ? videoContainer.hashCode() : "(null)"));
        }
        if (videoContainer != null) {
            int childCount = videoContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = videoContainer.getChildAt(i);
                ViewGroup.LayoutParams vlp = child.getLayoutParams();

                if (vlp instanceof SRGMediaPlayerView.LayoutParams) {
                    SRGMediaPlayerView.LayoutParams lp = (SRGMediaPlayerView.LayoutParams) vlp;
                    switch (lp.overlayMode) {
                        case SRGMediaPlayerView.LayoutParams.OVERLAY_CONTROL:
                            child.setVisibility(isShowingControlOverlays() ? View.VISIBLE : View.GONE);
                            break;
                        case SRGMediaPlayerView.LayoutParams.OVERLAY_ALWAYS_SHOWN:
                            child.setVisibility(View.VISIBLE);
                            break;
                        case SRGMediaPlayerView.LayoutParams.OVERLAY_LOADING:
                            child.setVisibility(isShowingLoadingOverlays() ? View.VISIBLE : View.GONE);
                            break;
                        case SRGMediaPlayerView.LayoutParams.OVERLAY_UNMANAGED:
                        default:
                            // Do nothing.
                            break;
                    }
                }
            }
        }
    }

    private boolean isShowingLoadingOverlays() {
        return loadingForced != null ? loadingForced : showingLoadings;
    }

    boolean isShowingControlOverlays() {
        return controlsForced != null ? controlsForced : showingControlOverlays;
    }

    /**
     * Configure auto hide delay (delay to change visibility for overlay of OVERLAY_CONTROL type)
     *
     * @param overlayAutoHideDelay auto hide delay in ms
     */
    static void setOverlayAutoHideDelay(int overlayAutoHideDelay) {
        OverlayController.overlayAutoHideDelay = overlayAutoHideDelay;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_HIDE_CONTROLS:
                if (!doesPlayerStateRequiresControls()) {
                    hideControlOverlaysImmediately();
                }
                return true;
            default:
                return false;
        }
    }

    void setForceControls(Boolean controlsForced) {
        this.controlsForced = controlsForced;
        if (controlsForced != null) {
            handler.removeMessages(MSG_HIDE_CONTROLS);
        } else {
            if (showingControlOverlays) {
                ensureControlsHiding();
            }
        }
        propagateOverlayVisibility();
    }

    void setForceLoaders(Boolean loadingForced) {
        this.loadingForced = loadingForced;
        propagateOverlayVisibility();
    }
}
