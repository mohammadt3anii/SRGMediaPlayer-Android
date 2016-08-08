/**
 * DRMLicenseFilter.java
 *
 * @brief
 *
 *
 * Created on 24/2/2016
 *
 * Copyright(c) 2016 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */
package ch.srg.mediaplayer.nagra.pak;

public class DRMLicenseFilter {
  private boolean mExpired;
  private String[] mPrmSyntaxes;

  public void setExpired(boolean expired) {
    mExpired = expired;
  }

  public boolean getExpired() {
    return mExpired;
  }

  public void setPrmSyntaxes(String[] prmSyntaxes) {
    mPrmSyntaxes = prmSyntaxes;
  }

  public String[] getPrmSyntaxes() {
    return mPrmSyntaxes;
  }
}
