package ch.srg.mediaplayer.nagra.pak;

public class PAKCoreLicense implements nagra.cpak.api.IPakCoreLicense {

  private String mDcm = null;
  private String mDmm = null;
  private String mPrmSyntax = null;

  PAKCoreLicense() {
  }
  
  PAKCoreLicense(String xPrmSyntax) {
    mPrmSyntax = xPrmSyntax;
  }
  
  PAKCoreLicense(String xDcm, String xDmm, String xPrmSyntax) {
    mDcm = xDcm;
    mDmm = xDmm;
    mPrmSyntax = xPrmSyntax;
  }

  public void setDcm(String Dcm) {
    mDcm = Dcm;
  }

  public void setDmm(String Dmm) {
    mDmm = Dmm;
  }

  public void setPrmSyntax(String prmSyntax) {
    mPrmSyntax = prmSyntax;
  }

  @Override
  public String getDcm() {
    return mDcm;
  }

  @Override
  public String getDmm() {
    return mDmm;
  }

  @Override
  public String getPrmSyntax() {
    return mPrmSyntax;
  }
}
