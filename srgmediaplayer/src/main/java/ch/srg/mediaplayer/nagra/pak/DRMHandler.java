/**
 * DRMHandler.java
 *
 * @brief Created on 24/2/2016
 *   DrmHandler provides sample code and methods for:
 *   1. how to startup CPAK
 *   2. how to do initialize
 *   3. how to do get license
 *   4. how to manage licenses with CPAK
 *   5. how to handle the various state change notifications with CPAK
 * <p/>
 * Copyright(c) 2016 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */

package ch.srg.mediaplayer.nagra.pak;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ch.srg.mediaplayer.R;
import nagra.cpak.api.EPakReleaseAction;
import nagra.cpak.api.IPakCoreNotifListener;
import nagra.cpak.api.PakCore;
import nagra.cpak.api.PakCoreAndroidPlatformParameters;
import nagra.cpak.api.PakCoreDebugSettings;
import nagra.cpak.api.PakCoreDrmAgent;
import nagra.cpak.api.PakCoreDrmAgent.EPakState;
import nagra.cpak.api.PakCoreDrmEntitlement;
import nagra.cpak.api.PakCoreDrmSession;
import nagra.nmp.sdk.NMPLog;

public class DRMHandler {
  private final static String TAG = "DRMHandler";
  private static DRMHandler sDrmHandler = null;

  private DRMHandlerResponse mResponse = null;
  private DRMHandlerListener mListener = null;
  private DRMHandlerOperationDelegate mDelegate = null;

  /**
   * Creates a static instance of the DrmHandler class, used to return the object when getInstance is called.
   * @param listener
   *    The listener for the DrmHandler
   * @param delegate
   *    The delegate for the operation methods
   *@param context
   *
   * @return The DrmHandler instance.
   */
  static public DRMHandler createInstance(DRMHandlerListener listener, DRMHandlerOperationDelegate delegate, Context context) {
    NMPLog.d(TAG, "Enter");
    if (sDrmHandler == null) {
      if (listener == null || delegate == null) {
        NMPLog.e(TAG, "DRMHandlerListener or DRMHandlerOperationDelegate is null!");
        return null;
      }
      sDrmHandler = new DRMHandler();
      boolean ret = sDrmHandler.preparePak(context);
      if (ret == false) {
        NMPLog.e(TAG, "Init pak failed.");
        sDrmHandler = null;
        return null;
      }

      sDrmHandler.initWithListener(listener, delegate);
    }

    NMPLog.d(TAG, "leave");
    return sDrmHandler;
  }

  /**
   * Returns the instance created before in createInstance
   * @return
   *    The DRMHandler instance if createInstance was invoked successfully or nil if not.
   * @note
   *    createInstance must be called once, prior to this.
   */

  public static DRMHandler getInstance() {
    return sDrmHandler;
  }

  /**
   * Release the instance of DrmHandler, also release the instance of PakCore
   *
   * @param eraseDB
   *          {@code true}Erase PAK database when releasing, {@code false}
   *          default PAK release behaviour.
   *
   */
  public static void disposeInstance(boolean eraseDB) {
    EPakReleaseAction action = eraseDB ? EPakReleaseAction.PAK_RELEASE_ERASE_DB
            : EPakReleaseAction.PAK_RELEASE_DEFAULT;
    PakCore.releaseInstance(action);
    sDrmHandler = null;
  }
    /**
     * Creates a new instance of the DRM handler.
     */
    private DRMHandler() {
    }

    /**
     * Initialise the Pak used by the DRM handlder.
     *
     * Must be called before any other method. In order to use the DRM handler,
     * {@link #initialize(DRMHandlerRequest, DRMHandlerResponse)} should be called
     * straight after.
     *
     * @param context
     *          Used to get the apps opvault.
     *
     * @return boolean {@code true} the PAK has initialized with no issues
     *         {@code false} the PAK failed to initialize successfully, see logcat
     *         for further details.
     *
     */
    private boolean preparePak(Context context) {
        NMPLog.v(TAG, "Enter");

        PakCore.createInstance();
        PakCore pakCore = PakCore.getInstance();
        if (pakCore == null) {
            NMPLog.e(TAG, "Leave, PakCore createInstance failed!");
            return false;
        }
        NMPLog.i(TAG, "CPAK version: " + pakCore.getVersion());

        //set operator's opVault.json to CPAK.
        PakCoreAndroidPlatformParameters params = (PakCoreAndroidPlatformParameters) pakCore.getPlatformParameters();
        if (params == null) {
            NMPLog.e(TAG, "PakCore getPlatformParameters failed");
            return false;
        }
        params.setContext(context);
        InputStream inStream = context.getResources().openRawResource(R.raw.opvault);
        if (inStream == null) {
            NMPLog.e(TAG, "PakCore openRawResource failed");
            return false;
        }
        try {
            boolean isSuc = false;
            isSuc = params.setOperatorVaultBuffer(getBytes(inStream));
            isSuc &= params.setUserStoreFilePath(context.getFilesDir().toString());
            inStream.close();

            if (!isSuc) {
                NMPLog.w(TAG, "Set operator vault fail.");
                return false;
            }
        } catch (IOException e) {
            NMPLog.e(TAG, "PakCore settings failed");
            e.printStackTrace();
            return false;
        }

        // Configure Pak DebugSettings
        PakCoreDebugSettings debugSettings = pakCore.getDebugSettings();
        debugSettings.setLogProvider(NMPCLogger.instance(context));
        debugSettings.setLevel(PakCoreDebugSettings.EPakCoreDebugLevel.WARNING);

        if (!pakCore.start()) {
            NMPLog.e(TAG, "pakCore.start() failed");
            return false;
        }

        NMPLog.v(TAG, "Leave");
        return true;
    }

    private void initWithListener(DRMHandlerListener listener, DRMHandlerOperationDelegate delegate) {
        mListener = listener;
        mDelegate = delegate;

        PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
        if (drmAgent == null) {
            NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
            return;
        }

        drmAgent.addPakStateChangedListener(mPakStateChangedListener);
        drmAgent.addSessionsChangedListener(mPakSessionsChangedListener);
        drmAgent.addPrefetchLicensesStateChangedListener(mPrefetchLicensesStateChangedListener);
        drmAgent.addLicenseImportationStateChangedListener(mLicenseImportationStateChangedListener);
        drmAgent.addStorageErrorListener(mStorageErrorListener);
    }

    /**
   *  Initialise on the selected server.
   *
   *  @param request
   *     The request object
   *  @param response
   *     The response delegate
   *  @return
   *     Initialisation success state
   */

  public boolean initialize(DRMHandlerRequest request, DRMHandlerResponse response) {
    if (request == null || response == null) {
      NMPLog.e(TAG, "request or response is null!");
      return  false;
    }
    //keep response for state change later.
    mResponse = response;
    return mDelegate.initialize(request, response);
  }

  /**
   * Request to get a license.
   * License acquisition is a two stage process,
   * and the listener will receive a status change when the licenses have been received.
   *
   * @param request
   *             The request object
   * @param response
   *             The response delegate
   * @return Success state of the call.
   */
  public boolean acquireLicense(DRMHandlerRequest request, DRMHandlerResponse response) {
    if(mDelegate == null) {
      NMPLog.e(TAG, "Operation delegator is null!");
      return false;
    }

    return mDelegate.acquireLicense(request, response);
  }

  /**
   * This method returns a set of DRMLicense in order to provide access to the
   * stored DRM license and their state at the time of the request or to remove
   * DRM license from persistent storage.
   *
   * @param filter
   *          if filter is null, this will return all stored DRM license from
   *          persistent storage.
   *          <p>
   *          If filter is not null and
   *          {@link DRMLicenseFilter#getPrmSyntaxes()} is not null as well, it
   *          will return the stored licenses for all prmSyntaxs.
   *          <p>
   *          If filter is not null and
   *          {@link DRMLicenseFilter#getPrmSyntaxes()} is null. It will return
   *          all expired license when {@link DRMLicenseFilter#getExpired()} is
   *          true. Otherwise it will return all unexpired DRMLicenses.
   *
   * @return a set of DRMLicense from persistent storage.
   */
  public DRMLicense[] getLicenses(DRMLicenseFilter filter) {
    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return null;
    }

    List<DRMLicense> licenseList = new ArrayList<DRMLicense>();
    List<PakCoreDrmEntitlement> list = drmAgent.generateListOfStoredDrmEntitlements(false);
    String[] prmSyntaxes = filter.getPrmSyntaxes();

    for(PakCoreDrmEntitlement entitlement: list) {
      DRMLicense license = new DRMLicense();
      if (!license.initWithEntitlement(entitlement)){
        continue;
      }

      if (filter == null) {
        //no filter; add license;
        licenseList.add(license);

      } else {
        // filter out depends prmSyntax or expired;
        if (prmSyntaxes != null && prmSyntaxes.length > 0) {
          // filter based on prmSyntaxes;
          for (String syntax : prmSyntaxes) {
            if (syntax != null && syntax.equals(license.getPrmSyntax())) {
              licenseList.add(license);
              break;
            }
          }

        } else {
          // filter based on expired or not;
          if (filter.getExpired() == license.getExpired()) {
            licenseList.add(license);
          }
        }
      }
    }

    return (DRMLicense[]) licenseList.toArray();
  }

  /**
   * Request to remove a license from the PAK
   * @param request
   *   The request object
   * @param response
   *   The response delegate
   * @return
   *   Success state of the call.
   */
  public boolean removeLicense(DRMHandlerRequest request, DRMHandlerResponse response) {
    NMPLog.v(TAG, "Enter");

    if (request== null || request.getContentId() == null || request.getContentId().isEmpty()) {
      NMPLog.e(TAG, "Leave with contentId is invalid!");
      return false;
    }

    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return false;
    }

    List<PakCoreDrmEntitlement> list = drmAgent.generateListOfStoredDrmEntitlements(null, request.getContentId());
      NMPLog.i(TAG, "list size with contentd " + list.size());

    if (!list.isEmpty()) {
      drmAgent.eraseStoredEntitlements(list);
    }

    for(PakCoreDrmEntitlement entitlement: list) {
      DRMLicense license = new DRMLicense();
      if (!license.initWithEntitlement(entitlement)) {
        continue;
      }

      response.licenseRemoved(license);
    }

    NMPLog.v(TAG, "Leave");
    return true;
  }

  /**
   * Prefetch a license from the PAK
   * @param request
   *   The request object
   * @return
   *   Success state of the call.
   */
  public boolean prefetchLicense(DRMHandlerRequest request) {
    NMPLog.v(TAG, "Enter");
    if (request == null) {
      NMPLog.e(TAG, "Leave because DRMHandlerRequest is invalid");
      return false;
    }

    String prmSyntax = request.getPrmSyntax();
    NMPLog.i(TAG, "prmSyntax " + prmSyntax);
    if (prmSyntax == null) {
      NMPLog.d(TAG, "Leave because prmSyntax is invalid couldn't prefetch license.");
      return false;
    }

    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return false;
    }

    EPakState state = (EPakState) drmAgent.getState();
    NMPLog.d(TAG, "state: " + state);

    if (state != EPakState.READY) {
      NMPLog.d(TAG, "state: is not READY");
      return false;
    }

    drmAgent.prefetchLicenses(request.getContentId(), request.getClearPrivateData(), request.getServerUrl(), request.getPersistLicense());

    NMPLog.v(TAG, "Leave");
    return true;
  }

  /**
   * Create a Pak Sessions Changed Listener.
   */
  private IPakCoreNotifListener mPakSessionsChangedListener = new IPakCoreNotifListener() {
    public void onNotification() {
      NMPLog.d(TAG, "SessionsChanged callback Enter and Leave.");
      sendChangedMessage(MessageState.MESSAGE_SESSIONS_CHANGED);
    }
  };

  /**
   * Create a Pak State Changed Listener.
   */
  private IPakCoreNotifListener mPakStateChangedListener = new IPakCoreNotifListener() {
    public void onNotification() {
      NMPLog.d(TAG, "PakStateChanged onNotification callback");
      sendChangedMessage(MessageState.MESSAGE_STATE_CHANGED);
    }
  };

  /**
   * Create a Pak Access Changed Listener.
   */
  private IPakCoreNotifListener mPakAccessChangedListener = new IPakCoreNotifListener() {
    public void onNotification() {
      NMPLog.d(TAG, "AccessChanged onNotification callback");
      sendChangedMessage(MessageState.MESSAGE_ACCESS_CHANGED);
    }
  };

  /**
   * Create a Pak Prefetch Licenses State Changed Listener.
   */
  private IPakCoreNotifListener mPrefetchLicensesStateChangedListener = new IPakCoreNotifListener() {
    public void onNotification() {
      NMPLog.d(TAG, "mPrefetchLicensesStateChangedListener onNotification callback");
      sendChangedMessage(MessageState.MESSAGE_PREFETCHLICENSE_STATE_CHANGED);
    }
  };

  /**
   * Create a Pak License Importation State Changed Listener.
   */
  private IPakCoreNotifListener mLicenseImportationStateChangedListener = new IPakCoreNotifListener() {
    @Override
    public void onNotification() {
      NMPLog.d(TAG, "mLicenseImportationStateChangedListener callback");
      sendChangedMessage(MessageState.MESSAGE_IMPORTATION_STATE_CHANGED);
    }
  };

  /**
   * Create a Pak Storage Error Listener.
   */
  private IPakCoreNotifListener mStorageErrorListener = new IPakCoreNotifListener() {
    @Override
    public void onNotification() {
      NMPLog.d(TAG, "mStorageErrorListener callback");
      sendChangedMessage(MessageState.MESSAGE_STORAGE_ERROR);
    }
  };


  /**
   * Using the message handler, send a specified message.
   */
  private void sendChangedMessage(int messageID) {
    if (mDAHandler != null) {
      Message msg = Message.obtain(mDAHandler, messageID);
      mDAHandler.sendMessage(msg);
    }
  }

  /**
   * Create a new handler for messages sent to this object.
   */
  @SuppressLint("HandlerLeak")
  private Handler mDAHandler = new Handler() {
    public void handleMessage(Message msg) {
      int message = msg.what;
      NMPLog.d(TAG, "message id: " + message);

      switch (message) {
        case MessageState.MESSAGE_SESSIONS_CHANGED:
          handleSessionChanged();
          break;

        case MessageState.MESSAGE_STATE_CHANGED:
          handleStateChanged();
          break;

        case MessageState.MESSAGE_ACCESS_CHANGED:
          handleAccessChanged();
          break;

        case MessageState.MESSAGE_PREFETCHLICENSE_STATE_CHANGED:
          handleprefetchLicenseChanged();
          break;

        case MessageState.MESSAGE_IMPORTATION_STATE_CHANGED:
          handleImportationStateChanged();
          break;

        case MessageState.MESSAGE_STORAGE_ERROR:
          handleStorageError();
          break;

        default:
          NMPLog.e(TAG, "unhandled message happens... ");
          break;
      }
    }
  };

  /**
   * A receiver method for handling the PAK State Changed notification outside
   * the PAK callback context to prevent PAK core instance deadlock.
   */
  private void handleStateChanged() {
    NMPLog.v(TAG, "Enter.");

    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      return;
    }

    EPakState state = (EPakState) drmAgent.getState();
    NMPLog.i(TAG, "state: " + state);
    switch (state) {
      case READY:
        NMPLog.i(TAG, "Server return private data: " + drmAgent.getServerPrivateData());
        mResponse.finished();
        break;

      case ERROR_CONNECTION_REQUIRED:
        NMPLog.w(TAG, "Last communication status: " + drmAgent.getLastCommunicationStatus());
        mResponse.finishedWithError(DRMHandlerError.INITIALIZATION_REQUIRED);
        break;

      case INITIALIZING:
      default:
        break;
    }
  }

  /**
   * A receiver method for handling the Importation License State Changed
   * notification outside the PAK callback context to prevent PAK core instance
   * deadlock.
   */
  private void handleImportationStateChanged() {
    NMPLog.v(TAG, "Enter.");
    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();

    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return;
    }

    NMPLog.d(TAG, "ELicenseImportationState: " + drmAgent.getLicenseImportationState());
    NMPLog.v(TAG, "Leave.");
  }

  /**
   * A receiver method for handling the PAK secure Storage Error notification
   * outside the PAK callback context to prevent PAK core instance deadlock.
   */
  private void handleStorageError() {
    NMPLog.v(TAG, "Enter.");
    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return;
    }

    NMPLog.i(TAG, "EPakStorageError: " + drmAgent.getLastStorageError());
    NMPLog.v(TAG, "Leave.");
  }

  /**
   * A receiver method for handling the PAK Prefetch License Changed
   * notification outside the PAK callback context to prevent PAK core instance
   * deadlock.
   */
  private void handleprefetchLicenseChanged() {
    NMPLog.v(TAG, "Enter.");
    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return;
    }

    PakCoreDrmAgent.ELicensePrefetchingState state = drmAgent.getLicensePrefetchingState();
    NMPLog.d(TAG, "ELicensePrefetchingState: " + state);
    switch (state) {
      case UNSTARTED:
        break;

      case IN_PROGRESS:
        break;

      case ERROR_COMMUNICATION_FAILED:
        NMPLog.e(TAG, "Communication failed");
        break;

      case DONE:
        break;

      default:
        NMPLog.w(TAG, "Unknown state");
        break;
    }
    NMPLog.v(TAG, "Leave.");
  }

  /**
   * A receiver method for handling the PAK Session Changed notification outside
   * the PAK callback context to prevent PAK core instance deadlock.
   */
  private void handleSessionChanged() {
    NMPLog.v(TAG, "Enter");

    PakCoreDrmSession session = DRMHandlerHelper.getRelatedDrmSession();
    if (session == null) {
      return;
    }

    PakCoreDrmEntitlement entitlement = session.getRelatedDrmEntitlement();
    if (null == entitlement) {
      NMPLog.e(TAG, "Leave with entitlement is invalid");
      return;
    }
    session.addAccessChangedListener(mPakAccessChangedListener);

    NMPLog.d(TAG, "entitlement - contentName: " + entitlement.getContentName());
    NMPLog.d(TAG, "entitlement - contentID: " + entitlement.getContentId());

    PakCoreDrmSession.EDRMSessionStatus sessionStatus = session.getStatus();
    PakCoreDrmEntitlement.EEntitlementState entitlementState = (PakCoreDrmEntitlement.EEntitlementState) entitlement
        .getState();

    NMPLog.d(TAG, "EDRMSessionStatus :" + sessionStatus);
    NMPLog.d(TAG, "entitlementState :" + entitlementState);
    NMPLog.d(TAG, "access :" + session.getAccess());

    switch (sessionStatus) {
      case WAITING_FOR_ENTITLEMENT:
        switch (entitlementState) {
          case MISSING:
            DRMHandlerRequest request = new DRMHandlerRequest();
            request.setIsPostDelivery(true);
            request.setContentId(entitlement.getContentId());
            mListener.licenseAcquisitionNeeded(request);

            break;

          case USABLE:
            logEntitlementDetail(entitlement);
            break;

          case UNREADABLE:
          case EXPIRED:
          default:
            break;
        }
        break;

      case OPENED:
      case FAILED:
      case FAILED_NOT_ALLOWED:
      default:
        break;
    }

    NMPLog.v(TAG, "Leave");
  }


  /**
   * Log current Entitlement details.
   *
   * @param entitlement
   *          target entitlement.
   */
  private void logEntitlementDetail(PakCoreDrmEntitlement entitlement) {
    if (entitlement.getCreationDate() != null) {
      NMPLog.d(TAG, "creation date " + entitlement.getCreationDate().toString());
    }

    NMPLog.d(TAG, "viewing duration " + entitlement.getViewingWindowDuration());

    if (entitlement.getExpirationDate() != null) {
      NMPLog.d(TAG, "expiration date " + entitlement.getExpirationDate().toString());
    }

    if (entitlement.getFirstVisualizationDate() != null) {
      NMPLog.d(TAG, "first visualization " + entitlement.getFirstVisualizationDate().toString());
    }

    NMPLog.d(TAG, "viewing window relative " +
                 (entitlement.isViewingWindowRelative() ? "first view" : "entitlement creation"));
  }

  /**
   * A receiver method for handling the PAK Access Changed notification outside
   * the PAK callback context to prevent PAK core instance deadlock.
   */
  private void handleAccessChanged() {
    NMPLog.v(TAG, "Enter.");
    PakCoreDrmSession session = DRMHandlerHelper.getRelatedDrmSession();
    if (session == null) {
      NMPLog.e(TAG, "Leave with session is invalid");
      return;
    }

    PakCoreDrmEntitlement entitlement = session.getRelatedDrmEntitlement();
    if (null == entitlement) {
      NMPLog.e(TAG, "Leave with entitlement is invalid");
      return;
    }

    PakCoreDrmSession.EDRMAccess access = session.getAccess();
    NMPLog.d(TAG, "session access Changed:" + access);
    switch (access) {
      case DENIED:
      case DENIED_INVALID_ENTITLEMENT:
      case DENIED_EXPIRED:
        DRMHandlerRequest request = new DRMHandlerRequest();
        request.setIsPostDelivery(true);
        request.setContentId(entitlement.getContentId());
        mListener.licenseAcquisitionNeeded(request);
        break;

      case GRANTED:
      case DENIED_RETRIEVING_LICENSE:
      default:
        break;
    }
    NMPLog.v(TAG, "Leave.");
  }

  /**
   * Read an InputStream into a byte array
   *
   * @param inStream
   *          the input stream source.
   * @return the byte array store the data from input stream.
   *
   * @throws IOException
   */
  private static byte[] getBytes(InputStream inStream) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    byte buffer[] = new byte[1024];
    int len;
    while ((len = inStream.read(buffer)) != -1) {
      outStream.write(buffer, 0, len);
    }
    byte[] data = outStream.toByteArray();
    outStream.close();
    return data;
  }
}
