/**
 * DRMHandlerHelper.java
 *
 * @brief
 *
 *
 * Created on 23/2/2016
 *
 * Copyright(c) 2016 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */

package ch.srg.mediaplayer.nagra.pak;

import java.util.List;

import nagra.cpak.api.PakCore;
import nagra.cpak.api.PakCoreDrmAgent;
import nagra.cpak.api.PakCoreDrmEntitlement;
import nagra.cpak.api.PakCoreDrmSession;
import nagra.nmp.sdk.NMPLog;

public class DRMHandlerHelper {
  private final static String TAG = "DRMHandlerHelper";
  /**
   * Get the PakCore DRM agent.
   *
   * @return the instance of DRM Agent of Pak Core, otherwise return null.
   */
  public static PakCoreDrmAgent getDrmAgent() {
    PakCore pakCore = PakCore.getInstance();
    if (pakCore == null) {
      NMPLog.e(TAG, "Enter & Leave with Instance of PakCore is null!");
      return null;
    }

    return pakCore.getDrmAgent();
  }

  /**
   * Get PakCore DRM Session for specified stream.
   *
   * @return the DRM session used for specified stream if find one, otherwise
   *         return null;
   */
  public static PakCoreDrmSession getRelatedDrmSession() {
    List<PakCoreDrmSession> sessions = getDrmAgent().getDrmSessions();
    PakCoreDrmSession session = null;

    if( sessions != null && sessions.size()>0) {
      session = sessions.get(0);
      if (session != null) {
        NMPLog.d(TAG, "Leave with session");
        return session;
      }
    }

    NMPLog.w(TAG, "Leave with no matched session");
    return null;
  }
}
