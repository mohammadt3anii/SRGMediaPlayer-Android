/**
 * DRMHandlerListener.java
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


public interface DRMHandlerListener {
  /**
   * Tells the listener that it needs to fetch the license
   * @param request
   *   Contains the details for the request the client needs to make
   */
  public void licenseAcquisitionNeeded(DRMHandlerRequest request);
}
