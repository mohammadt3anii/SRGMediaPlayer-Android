package ch.srg.mediaplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import nagra.nmp.sdk.NMPSDK;
import nagra.nmp.sdk.NMPVideoView;

/**
 * Created by seb on 08/08/16.
 */
public class NagraDelegate implements PlayerDelegate {
    private static final String TAG = "NagraDelegate";

    private static boolean initialized;
    private final OnPlayerDelegateListener listener;
    @Nullable
    private NMPVideoView videoView;
    private Uri pendingPrepareUri;
    private boolean pendingStart;

    public NagraDelegate(Context context, OnPlayerDelegateListener listener) {
        initialization(context);
        this.listener = listener;
    }

    public static void initialization(Context applicationContext) {
        if (!initialized) {
            initialized = true;
            NMPSDK.load(applicationContext);
        }
    }

    @Override
    public boolean canRenderInView(View view) {
        return view instanceof NMPVideoView;
    }

    @Override
    public View createRenderingView(Context parentContext) {
        if (videoView == null) {
            videoView = new NMPVideoView(parentContext);
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    listener.onPlayerDelegateCompleted(NagraDelegate.this);
                }
            });
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    listener.onPlayerDelegateError(NagraDelegate.this, new SRGMediaPlayerException(i + "/" + i1));
                    return false;
                }
            });
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    listener.onPlayerDelegateReady(NagraDelegate.this);
                }
            });
            Log.v(TAG, "create (pending: " + pendingPrepareUri + ") " + videoView);
        } else {
            // TODO Shouldn't this be done in the controller or MediaPlayerView as bind is done there?
            if (videoView != null && videoView.getParent() != null) {
                ((ViewGroup) videoView.getParent()).removeView(videoView);
            }
            Log.v(TAG, "not creating (pending: " + pendingPrepareUri + ") " + videoView);
        }
        if (pendingPrepareUri != null) {
            listener.onPlayerDelegatePreparing(this);
            videoView.setVideoURI(pendingPrepareUri);
            pendingPrepareUri = null;
        }
        if (pendingStart) {
            videoView.start();
            pendingStart = false;
        }
        return videoView;
    }

    @Override
    public void bindRenderingViewInUiThread(SRGMediaPlayerView mediaPlayerView) throws SRGMediaPlayerException {
        View videoRenderingView = mediaPlayerView.getVideoRenderingView();
        if (!(videoRenderingView instanceof NMPVideoView)) {
            throw new SRGMediaPlayerException("Invalid video view: " + videoView);
        }
        Log.v(TAG, "bound " + mediaPlayerView + " to " + videoView);
    }

    @Override
    public void unbindRenderingView() {
        Log.v(TAG, "unbind " + videoView);
    }

    @Override
    public void prepare(Uri videoUri) throws SRGMediaPlayerException {
        Log.v(TAG, "prepare " + videoUri + " " + videoView);
        if (videoView != null) {
            listener.onPlayerDelegatePreparing(this);
            videoView.setVideoURI(videoUri);
        } else {
            pendingPrepareUri = videoUri;
        }
    }

    @Override
    public void playIfReady(boolean playIfReady) throws IllegalStateException {
        Log.v(TAG, "playIfReady " + playIfReady + " " + videoView);
        if (videoView != null) {
            if (playIfReady) {
                videoView.start();
            } else {
                videoView.stopPlayback();
                pendingStart = false;
            }
        } else {
            pendingStart = playIfReady;
        }
    }

    @Override
    public void seekTo(long positionInMillis) throws IllegalStateException {
        Log.v(TAG, "seekTo" + positionInMillis + " " + videoView);
        if (videoView != null) {
            videoView.seekTo((int) positionInMillis);
        }
    }

    @Override
    public boolean isPlaying() {
        return videoView != null && videoView.isPlaying();
    }

    @Override
    public void setMuted(boolean muted) {
    }

    @Override
    public long getCurrentPosition() {
        return videoView != null ? videoView.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return videoView != null ? videoView.getDuration() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return videoView != null ? videoView.getBufferPercentage() : 0;
    }

    @Override
    public long getBufferPosition() {
        return videoView != null ? videoView.getBufferPercentage() * videoView.getDuration() / 100 : 0;
    }

    @Override
    public int getVideoSourceHeight() {
        return 0; // oh oh oh
    }

    @Override
    public void release() throws IllegalStateException {
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public long getPlaylistStartTime() {
        return 0;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public SRGMediaPlayerController.Event.ScreenType getScreenType() {
        return SRGMediaPlayerController.Event.ScreenType.DEFAULT;
    }

    @Override
    public void setQualityOverride(Long quality) {
    }

    @Override
    public void setQualityDefault(Long quality) {
    }

    @Override
    public Long getBandwidthEstimate() {
        return null;
    }

    @Override
    public long getPlaylistReferenceTime() {
        return 0;
    }
}
