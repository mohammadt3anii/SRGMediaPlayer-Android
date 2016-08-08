/**
 * DRMHandlerOperationDelegate.java
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

public interface DRMHandlerOperationDelegate {
  /**
   * @brief
   *   initializes with the server.
   * @param request
   *   The request with the server details.
   * @param response
   *   The response from the server when request is finished.
   *   Customer application can use xResponse to pass information and in this sample it is not used.
   * @return
   *   Indicates whether the initialization was successful.
   */
   public boolean initialize(DRMHandlerRequest request, DRMHandlerResponse response);

  /**
   * @brief
   *   Request the server to retrieve the license.
   * @param request
   *   The request to be made for the license.
   * @param response
   *   Reply from the server after the request for license.
   *   Customer application can use xResponse to pass information and in this sample it is not used.
   * @return
   *   Indicates whether the license acquisition was successful.
   */
  public boolean acquireLicense(DRMHandlerRequest request, DRMHandlerResponse response);
}
