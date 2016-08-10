package ch.srg.mediaplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ch.srg.mediaplayer.nagra.pak.DRMHandler;
import ch.srg.mediaplayer.nagra.pak.DRMHandlerDirectOperationDelegate;
import ch.srg.mediaplayer.nagra.pak.DRMHandlerError;
import ch.srg.mediaplayer.nagra.pak.DRMHandlerListener;
import ch.srg.mediaplayer.nagra.pak.DRMHandlerRequest;
import ch.srg.mediaplayer.nagra.pak.DRMHandlerResponse;
import ch.srg.mediaplayer.nagra.pak.DRMLicense;
import nagra.nmp.sdk.NMPSDK;
import nagra.nmp.sdk.NMPTrackInfo;
import nagra.nmp.sdk.NMPVideoView;

/**
 * Created by seb on 08/08/16.
 */
public class NagraDelegate implements PlayerDelegate {
    private static final String TAG = "NagraDelegate";
    private static final String SERVER_URL = "http://ssolab1.nagra.com/srg/";
    public static final String SERVER_PRIVATE_DATA = "";
    private static final String SERVER_CLEAR_PRIVATE_DATA = "";

    private static boolean initializationStarted;
    private static DRMHandler drmHandler;
    @NonNull
    private final OnPlayerDelegateListener listener;
    @Nullable
    private NMPVideoView videoView;
    private Uri pendingPrepareUri;
    private boolean pendingStart;

    private static NagraDelegate pendingDelegate;
    private static boolean licenseInitialized;

    public NagraDelegate(Context context, @NonNull OnPlayerDelegateListener listener) {
        if (!initializationStarted) {
            throw new IllegalStateException("Initialization must be called first");
        }
        this.listener = listener;
    }

    @MainThread
    public static void initialization(final Context applicationContext) {
        if (!initializationStarted) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("Invalid thread");
            }
            initializationStarted = true;
            NMPSDK.load(applicationContext);
            DRMHandler.createInstance(new DRMHandlerListener() {
                private Toast toast;

                @Override
                public void licenseAcquisitionNeeded(DRMHandlerRequest request) {
                    Log.v(TAG, "License acquisition: " + request);
                    request.setServerUrl(SERVER_URL);
                    request.setClientPrivateData(SERVER_PRIVATE_DATA);
                    request.setClearPrivateData(SERVER_CLEAR_PRIVATE_DATA);
                    showToast("LicenseRequest: " + request.getContentId());
                    DRMHandler.getInstance().acquireLicense(request, new DRMHandlerResponse() {

                        @Override
                        public void setPrivateData(String privateData) {

                        }

                        @Override
                        public void licenseAdded(DRMLicense license) {
                            showToast("LicenseAdded: " + license.getContentName());
                        }

                        @Override
                        public void licenseRemoved(DRMLicense license) {
                            showToast("LicenseRemoved: " + license.getContentName());
                        }

                        @Override
                        public void finished() {
                        }

                        @Override
                        public void finishedWithError(DRMHandlerError error) {
                        }
                    });
                }

                public void showToast(String text) {
                    if (toast != null) {
                        toast.cancel();
                    }
                    toast = Toast.makeText(applicationContext, text, Toast.LENGTH_LONG);
                    toast.show();
                }
            }, new DRMHandlerDirectOperationDelegate(), applicationContext);

            DRMHandlerRequest request = new DRMHandlerRequest();
            request.setServerUrl(SERVER_URL);
            request.setClientPrivateData(SERVER_PRIVATE_DATA);
            request.setClearPrivateData(SERVER_PRIVATE_DATA);
            Log.v(TAG, "DRM Initialize : " + request);
            DRMHandler.getInstance().initialize(request, new DRMHandlerResponse() {
                @Override
                public void setPrivateData(String privateData) {

                }

                @Override
                public void licenseAdded(DRMLicense license) {
                }

                @Override
                public void licenseRemoved(DRMLicense license) {
                }

                @Override
                public void finished() {
                    licenseInitialized = true;
                    if (pendingDelegate != null) {
                        pendingDelegate.startPendingIfReady();
                    }
                }

                @Override
                public void finishedWithError(DRMHandlerError error) {
                }
            });
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
        startPendingIfReady();
        if (pendingStart) {
            videoView.start();
            pendingStart = false;
        }
        return videoView;
    }

    public void startPendingIfReady() {
        if (pendingPrepareUri != null && videoView != null && licenseInitialized) {
            listener.onPlayerDelegatePreparing(this);
            videoView.setVideoURI(pendingPrepareUri);
            pendingPrepareUri = null;
        }
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
        pendingPrepareUri = videoUri;
        pendingDelegate = this;
        startPendingIfReady();
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

    public NMPTrackInfo[] getNMPTrackInfo(){
        if (videoView != null){
            return videoView.getNMPTrackInfo();
        }
        return null;
    }

    public void selectTrack(int trackID) {
        if (videoView != null){
            videoView.selectTrack(trackID);
        }
    }
}
