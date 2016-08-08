/**
 * DRMLicense.java
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

import java.util.Calendar;
import java.util.Date;

import nagra.cpak.api.PakCoreDrmEntitlement;
import nagra.nmp.sdk.NMPLog;

public class DRMLicense {
  private final static String TAG = "DRMLicense";

  private String mPrmSyntax;
  private String mContentName;
  private String mContentId;
  private String mKeyId;
  private Date mCreationDate;
  private Date mExpirationDate;
  private Date mFirstVisualizationDate;
  private int mViewingWindowDuration;
  private boolean mRelativeViewingWindow;
  private String mMetaData;
  private boolean mExpired;

  public String getPrmSyntax() {
    return mPrmSyntax;
  }

  protected void setprmSyntax(String prmSyntax) {
    mPrmSyntax = prmSyntax;
  }

  public String getContentName() {
    return mContentName;
  }

  protected void setContentName(String contentName) {
    mContentName = contentName;
  }

  public String getContentId() {
    return mContentId;
  }

  protected void setContentId(String contentId) {
    mContentId = contentId;
  }

  public String getKeyId() {
    return mKeyId;
  }

  protected void setKeyId(String keyId) {
    mKeyId = keyId;
  }

  public Date getCreationDate() {
    return mCreationDate;
  }

  protected void setCreationDate(Date creationDate) {
    mCreationDate = creationDate;
  }

  public Date getExpirationDate() {
    return mExpirationDate;
  }

  protected void setExpirationDate(Date expirationDate) {
    mExpirationDate = expirationDate;
  }

  public Date getFirstVisualizationDate() {
    return mFirstVisualizationDate;
  }

  protected void setFirstVisualizationDate(Date firstVisualizationDate) {
    mFirstVisualizationDate = firstVisualizationDate;
  }

  public int getViewingWindowDuration() {
    return mViewingWindowDuration;
  }

  protected void setViewingWindowDuration(int viewingWindowDuration) {
    mViewingWindowDuration = viewingWindowDuration;
  }

  public boolean getRelativeViewingWindow() {
    return mRelativeViewingWindow;
  }

  protected void setRelativeViewingWindow(boolean relativeViewingWindow) {
    mRelativeViewingWindow = relativeViewingWindow;
  }

  public String getMetaData() {
    return mMetaData;
  }

  protected void setMetaData(String metaData) {
    mMetaData = metaData;
  }

  public boolean getExpired() {
    if(mExpirationDate == null ){
      mExpired = false;

    }else{
      Calendar cal = Calendar.getInstance();
      cal.setTime(mExpirationDate);

      mExpired = (cal.getTimeInMillis() < System.currentTimeMillis());
    }

    return mExpired;
  }

  protected void setExpired(boolean expired) {
    mExpired = expired;
  }

  protected boolean initWithEntitlement(PakCoreDrmEntitlement entitlement) {
    if (entitlement == null) {
      NMPLog.e(TAG, "Can't init DRMLicense due to entitlement is null!");
      return false;
    }

    mPrmSyntax = entitlement.generatePrmSyntax();
    mContentName = entitlement.getContentName();
    mContentId = entitlement.getContentId();
    mKeyId = entitlement.getKeyId();
    mCreationDate = entitlement.getCreationDate().getTime();
    mExpirationDate = entitlement.getExpirationDate().getTime();
    mFirstVisualizationDate = entitlement.getFirstVisualizationDate().getTime();
    mViewingWindowDuration = entitlement.getViewingWindowDuration();
    mRelativeViewingWindow = entitlement.isViewingWindowRelative();
    mMetaData = entitlement.getSpecificMetadata();

    return true;
  }
}
