/**
 * DRMHandlerRequest.java
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

public class DRMHandlerRequest {
  private boolean mPersistLicense;
  private String mPrmSyntax;
  private boolean mIsPostDelivery;
  private String mClientPrivateData;
  private String mClearPrivateData;
  private String mServerUrl;
  private String mContentId;

  /**
   *  If the request is persistant or not.
   */
  public boolean getPersistLicense() {
    return mPersistLicense;
  }

  /**
   *  Set bool to state if the request is persistant or not.
   */
  public void setPersistLicense(boolean persistLicense) {
    mPersistLicense = persistLicense;
  }

  /**
   *  Get string to give the PRM Syntax
   */
  public String getPrmSyntax() {
    return mPrmSyntax;
  }

  /**
   *  Set string to give the PRM Syntax
   */
  public void setPrmSyntax(String prmSyntax) {
    mPrmSyntax = prmSyntax;
  }

  /**
   *  Is postDelivery
   */
  public boolean getIsPostDelivery() {
    return mIsPostDelivery;
  }

  /**
   *  bool to indicate if the acuireLicense is a post or pre request.
   *  true for on-line encrypted stream playback
   *  false for encrypted stream playback which license has to be fetched before playback.
   *  For "download to go" encryted stream this propery has to be set to false.
   *
   */
  public void setIsPostDelivery(boolean isPostDelivery) {
    mIsPostDelivery = isPostDelivery;
  }

  public String getClientPrivateData() {
    return mClientPrivateData;
  }

  public void setClientPrivateData(String clientPrivateData) {
    mClientPrivateData = clientPrivateData;
  }

  /**
   * Get server Private data
   *
   * @return
   */
  public String getClearPrivateData() {
        return mClearPrivateData;
    }

  /**
   *  String to give the server Private data inserted in clear during any communication with the head end
   *  It should be a string base64 encoded in order to be Json and XML compatible.
   *  It should be a string base64 encoded in order to be Json and XML compatible.
   *  These data can processed by the server which can return an answer in the response.
   *
   * @note
   *  It is used in Direct mode initialize or request license.
   */
  public void setClearPrivateData(String clearPrivateData) {
    mClearPrivateData = clearPrivateData;
  }

  /**
   *  Get string of the server url used by the PAK to communicate.
   */
  public String getServerUrl() {
    return mServerUrl;
  }

  /**
   *  Set string of the server url used by the PAK to communicate.
   */
  public void setServerUrl(String serverUrl) {
    mServerUrl = serverUrl;
  }

  /**
   *  get content ID.
   */
  public String getContentId() {
    return mContentId;
  }

  /**
   *  Set content ID.
   */
  public void setContentId(String contentId) {
    mContentId = contentId;
  }
}
