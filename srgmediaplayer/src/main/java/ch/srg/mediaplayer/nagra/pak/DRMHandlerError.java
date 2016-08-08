/**
 * DRMHandlerError.java
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


public enum DRMHandlerError {
  INITIALIZATION_REQUIRED(1),
  COMMUNICATION_XXX(2),
  LICENSE_XXX(3),
  OPERATOR_VAULT_INVALID(4),
  STORAGE_NOT_ALLOWED(5),
  STORAGE_FULL(6),
  STORAGE_ACCESS_FAILED(7);

  private int value;

  private DRMHandlerError(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
