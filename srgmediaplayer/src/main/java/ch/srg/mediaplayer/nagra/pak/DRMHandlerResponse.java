/**
 * DRMHandlerResponse.java
 *
 * @brief
 *
 *
 * Created on 23/02/2016
 *
 * Copyright(c) 2016 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */

package ch.srg.mediaplayer.nagra.pak;


public interface DRMHandlerResponse {
  /**
   * sets private data
   * @param privateData
   *   base64 string of the encrypted private data
  */
  public void setPrivateData(String privateData);

  /**
   *  License added
   * @param license
   */
  public void licenseAdded(DRMLicense license);

  /**
   * License removed
   * @param license
   */
  public void licenseRemoved(DRMLicense license);

  /**
   *   Finished request without error
   */
  public void finished();

  /**
   * Finished with specified error
   * @param error
   *   the type of error returned
   */
  public void finishedWithError(DRMHandlerError error);
}
