/**
 * DRMHandlerDirectOperationDelegate.java
 *
 * @brief
 * Class to implement direct initialization and acquireLicense by implementing DRMHandlerOperationDelegate interface.
 *
 * Created on 23/2/2016
 *
 * Copyright(c) 2016 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */

package ch.srg.mediaplayer.nagra.pak;

import nagra.nmp.sdk.NMPLog;
import nagra.cpak.api.PakCoreDrmAgent;
import nagra.cpak.api.PakCoreDrmEntitlement;
import nagra.cpak.api.PakCoreDrmSession;
import nagra.cpak.api.PakCoreDrmAgent.EPakState;

public class DRMHandlerDirectOperationDelegate implements DRMHandlerOperationDelegate {
  private final static String TAG = "DRMHandlerDirectOperationDelegate";

  @Override
  public boolean initialize(DRMHandlerRequest request, DRMHandlerResponse response) {
    NMPLog.v(TAG, "Enter");

    PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Instance of drmAgent is null!");
      return false;
    }

    NMPLog.i(TAG, "DeviceUniqueId:" + drmAgent.getDeviceUniqueId());
    EPakState state = (EPakState) drmAgent.getState();
    NMPLog.i(TAG, "drmAgent state " + state);
    switch (state) {
      case ERROR_CONNECTION_REQUIRED:
        drmAgent.silentInitialize(request.getServerUrl(),
                request.getClientPrivateData(),
                request.getClearPrivateData(),
                request.getPersistLicense());

        return true;

      case READY:
        NMPLog.i(TAG, "drmAgent state is READY not need initialize again");
        if (response != null) {
          response.setPrivateData(drmAgent.getServerPrivateData());
          response.finished();
        }

        return true;


      case FATAL_ERROR:
      case FATAL_ERROR_OPERATOR:
        NMPLog.i(TAG, "drmAgent state is fatal error, check opvault.");
        if (response != null) {
          response.finishedWithError(DRMHandlerError.INITIALIZATION_REQUIRED);
        }

        return false;

      case INITIALIZING:
      default:
        break;
    }

    NMPLog.v(TAG, "Leave");
    return false;
  }

  @Override
  public boolean acquireLicense(DRMHandlerRequest request, DRMHandlerResponse response) {
    NMPLog.v(TAG, "Enter");
    PakCoreDrmSession session = DRMHandlerHelper.getRelatedDrmSession();

    if (session == null) {
      NMPLog.w(TAG, "No PakCore DRM session for contentId " + request.getContentId());
      return false;
    }

    PakCoreDrmEntitlement entitlement = session.getRelatedDrmEntitlement();

    if (null == entitlement) {
      NMPLog.e(TAG, "Leave with entitlement is invalid");
      return false;
    }

    boolean result = entitlement.requestLicense(request.getClientPrivateData(),
                                                request.getClearPrivateData(),
                                                request.getServerUrl());
    NMPLog.i(TAG, " requestLicense result " + result);

    if(result) {
      DRMLicense drmLicense = new DRMLicense();
      drmLicense.setContentId(entitlement.getContentId());
      drmLicense.setContentName(entitlement.getContentName());

      if (entitlement.getCreationDate() != null) {
        drmLicense.setCreationDate(entitlement.getCreationDate().getTime());
      }

      if (entitlement.getExpirationDate() != null) {
        drmLicense.setExpirationDate(entitlement.getExpirationDate().getTime());
      }

      if (entitlement.getFirstVisualizationDate() != null) {
        drmLicense.setFirstVisualizationDate(entitlement.getFirstVisualizationDate()
                .getTime());
      }

      drmLicense.setKeyId(entitlement.getKeyId());
      drmLicense.setMetaData(entitlement.getSpecificMetadata());
      drmLicense.setprmSyntax(entitlement.generatePrmSyntax());
      drmLicense.setRelativeViewingWindow(entitlement.isViewingWindowRelative());
      drmLicense.setViewingWindowDuration(entitlement.getViewingWindowDuration());

      response.licenseAdded(drmLicense);
    }
    NMPLog.v(TAG, "Leave with true");
    return true;
  }
}
