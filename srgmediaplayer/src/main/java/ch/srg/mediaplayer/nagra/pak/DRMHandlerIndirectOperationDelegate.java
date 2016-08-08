/**
 * DRMHandlerIndirectOperationDelegate.java
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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.protocol.HTTP;
import nagra.cpak.api.IPakCoreLicense;
import nagra.cpak.api.PakCoreDrmAgent;
import nagra.cpak.api.PakCoreDrmEntitlement;
import nagra.cpak.api.PakCoreDrmSession;
import nagra.nmp.sdk.NMPLog;


public class DRMHandlerIndirectOperationDelegate implements DRMHandlerOperationDelegate{
  private final static String TAG = "DRMHandlerIndirectOperationDelegate";
  private long NMP_ENTITLEMENT_VIEW_DURATION = 24 * 60 * 60;

  @Override
  public boolean initialize(DRMHandlerRequest request, final DRMHandlerResponse response) {
    final PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return false;
    }

    String initializationPayload = drmAgent.getInitializationPayloadForServer();
    if (initializationPayload == null) {
      NMPLog.e(TAG, "Leave becasue mInitializationPayload is invalid, initialize failed");
      return false;
    }

    String deviceId = drmAgent.getDeviceId();
    if (deviceId.isEmpty()) {
      deviceId = "0";
    }

    String contentType = "text/xml; charset=utf-8";
    AsyncHttpClient mAsyncHttpClient = createHttpClient();
    HttpEntity entity = buildInitializeSoapRequestEntity(deviceId, initializationPayload);

    mAsyncHttpClient.post(null, request.getServerUrl(), entity, contentType,
            new AsyncHttpResponseHandler() {
              @Override
              public void onSuccess(int statusCode, Header[] headers, byte[] httpResponse) {
                NMPLog.d(TAG, "initialize SOAP response: " + httpResponse);
                NMPLog.d(TAG, "status code: " + statusCode);
                if(statusCode == 200){
                  if (httpResponse == null) {
                    response.finishedWithError(DRMHandlerError.INITIALIZATION_REQUIRED);

                  } else {
                    HashMap<String, String> info = parseInitializeResponse(new String(httpResponse));
                    String payload = info.get("secrets");
                    if (payload != null) {
                      drmAgent.initialize(payload);
                      NMPLog.i(TAG, "initialize pak finished, deviceID: " + drmAgent.getDeviceId());
                    }

                    if (response != null) {
                      response.setPrivateData(drmAgent.getServerPrivateData());
                      response.finished();
                    }
                  }
                }

              }

              @Override
              public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                NMPLog.e(TAG, "initialize with SOAP failed: " + errorResponse);
                if (response != null) {
                  response.finishedWithError(DRMHandlerError.INITIALIZATION_REQUIRED);
                }
              }
            });

    return true;
  }

  @Override
  public boolean acquireLicense(final DRMHandlerRequest request, final DRMHandlerResponse response) {
    final PakCoreDrmAgent drmAgent = DRMHandlerHelper.getDrmAgent();
    if (drmAgent == null) {
      NMPLog.e(TAG, "Leave with Instance of drmAgent is null!");
      return false;
    }

    if(request.getContentId() == null) {
      NMPLog.w(TAG, "contentId is null");
      return false;
    }

    String payload = null;

    if (request.getIsPostDelivery()) {
      PakCoreDrmSession session = DRMHandlerHelper.getRelatedDrmSession();

      if (session == null) {
        NMPLog.w(TAG, "No PakCore DRM session for contentId " + request.getContentId());
        return false;
      }

      final PakCoreDrmEntitlement entitlement = session.getRelatedDrmEntitlement();

      if (null == entitlement) {
        NMPLog.e(TAG, "Leave with entitlement is invalid");
        return false;
      }

      payload = entitlement.getEntitlementPayloadForServer();
    } else {
      payload = drmAgent.getPrefetchLicensesPayloadForServer(request.getClientPrivateData());
    }

    if(payload == null || payload.isEmpty()){
      NMPLog.e(TAG, "Leave becasue Entitlement PayloadForServer is invalid.");
      return false;
    }

    String contentType = "text/xml; charset=utf-8";
    HttpEntity entity =
            buildAcquireLicenseSoapRequestEntity(request.getContentId(), drmAgent.getDeviceId(), payload);
    AsyncHttpClient mAsyncHttpClient = createHttpClient();
    mAsyncHttpClient.post(null, request.getServerUrl(), entity, contentType,
            new AsyncHttpResponseHandler() {
              @Override
              public void onSuccess(int statusCode, Header[] headers, byte[] httpResponse) {
                NMPLog.d(TAG, "status code: " + statusCode);
                if(statusCode == 200){
                  NMPLog.d(TAG, "get liceense SOAP response: " + new String(httpResponse));
                  PAKCoreLicense license = new PAKCoreLicense(request.getPrmSyntax());
                  boolean isSuc = parseLicenseResponse(new String(httpResponse), license);
                  if (isSuc) {
                    if (request.getIsPostDelivery()) {
                      String dcm = license.getDcm();
                      String dmm = license.getDmm();
                      PakCoreDrmSession session = DRMHandlerHelper.getRelatedDrmSession();
                      PakCoreDrmEntitlement entitlement = session.getRelatedDrmEntitlement();
                      if (entitlement != null) {
                        NMPLog.d(TAG, "Set entitlement");
                        entitlement.setEntitlement(dcm, dmm);
                      }
                      else {
                        NMPLog.e(TAG, "entitlement is null, acquireLicense failed");
                      }
                    }
                    else {
                      NMPLog.d(TAG, "import license" + request.getPersistLicense());
                      NMPLog.d(TAG, "xRequest.getPersistLicense()" + request.getPersistLicense());
                      List<IPakCoreLicense> xLicensesList = new ArrayList<IPakCoreLicense>();
                      xLicensesList.add(license);
                      drmAgent.importLicenses(xLicensesList, request.getPersistLicense());
                    }
                  }
                }
              }

              @Override
              public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                NMPLog.e(TAG, "get liceense with SOAP failed: " + new String(errorResponse));
              }
            });

    return true;

  }

  private AsyncHttpClient createHttpClient() {
    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    asyncHttpClient.addHeader("Accept", "text/xml");
    asyncHttpClient.addHeader("Accept-Charset", "utf-8");
    asyncHttpClient.addHeader("Accept", "text/xml,application/text+xml,application/soap+xml");

    return asyncHttpClient;
  }

  private HttpEntity buildInitializeSoapRequestEntity(String deviceId, String initializationPayload) {
    StringBuffer xmlDataBuff = new StringBuffer();
    xmlDataBuff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xmlDataBuff.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sof=\"SoftwareEntitlement-v1\">");
    xmlDataBuff.append("<soapenv:Header/>");
    xmlDataBuff.append("<soapenv:Body>");
    xmlDataBuff.append("<sof:initializeDevice>");

    xmlDataBuff.append("<device>");
    xmlDataBuff.append("<deviceId>" + deviceId + "</deviceId>");
    xmlDataBuff.append("</device>");

    xmlDataBuff.append("<opaque>" + initializationPayload + "</opaque>");

    xmlDataBuff.append("<policy>");
    xmlDataBuff.append("<processOnCompromisedOS>");
    xmlDataBuff.append("<platformConfig>");
    xmlDataBuff.append("<platform>Android</platform>");
    xmlDataBuff.append("<processOnError>true</processOnError>");
    xmlDataBuff.append("</platformConfig>");
    xmlDataBuff.append("</processOnCompromisedOS>");
    xmlDataBuff.append("</policy>");

    xmlDataBuff.append("</sof:initializeDevice>");
    xmlDataBuff.append("</soapenv:Body>");
    xmlDataBuff.append("</soapenv:Envelope>");
    NMPLog.d(TAG, "initialize SOAP request: " + xmlDataBuff.toString());

    HttpEntity entity = new StringEntity(xmlDataBuff.toString(), HTTP.UTF_8);
    return entity;
  }

  private HttpEntity buildAcquireLicenseSoapRequestEntity(String contentId, String deviceId, String payload) {
    Date expireDate = new Date(System.currentTimeMillis() + NMP_ENTITLEMENT_VIEW_DURATION * 10000);
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS", Locale.getDefault());

    StringBuffer xmlDataBuff = new StringBuffer();
    xmlDataBuff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xmlDataBuff
            .append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sof=\"SoftwareEntitlement-v1\">");
    xmlDataBuff.append("<soapenv:Header/>");
    xmlDataBuff.append("<soapenv:Body>");
    xmlDataBuff.append("<sof:createEntitlement>");

    xmlDataBuff.append("<content>");
    xmlDataBuff.append("<contentId>" + contentId + "</contentId>");
    xmlDataBuff.append("</content>");

    xmlDataBuff.append("<device>");
    xmlDataBuff.append("<deviceId>" + deviceId + "</deviceId>");
    xmlDataBuff.append("</device>");

    xmlDataBuff.append("<opaque>" + payload + "</opaque>");

    xmlDataBuff.append("<policy>");
    xmlDataBuff.append("<processOnCompromisedOS>");
    xmlDataBuff.append("<platformConfig>");
    xmlDataBuff.append("<platform>Android</platform>");
    xmlDataBuff.append("<processOnError>true</processOnError>");
    xmlDataBuff.append("</platformConfig>");
    xmlDataBuff.append("</processOnCompromisedOS>");
    xmlDataBuff.append("</policy>");

    xmlDataBuff.append("<validity>");
    xmlDataBuff.append("<expirationDate>" + df.format(expireDate) + "</expirationDate>");
    xmlDataBuff.append("<isViewingWindowFloating>true</isViewingWindowFloating>");
    xmlDataBuff.append("<viewingDuration>86393</viewingDuration>");
    xmlDataBuff.append("</validity>");

    xmlDataBuff.append("</sof:createEntitlement>");
    xmlDataBuff.append("</soapenv:Body>");
    xmlDataBuff.append("</soapenv:Envelope>");
    NMPLog.d(TAG, "get liceense SOAP request: " + xmlDataBuff.toString());

    HttpEntity entity = new StringEntity(xmlDataBuff.toString(), HTTP.UTF_8);

    return entity;
  }

  /**
   * Parse the initialisation response.
   *
   * @param response
   *          xml format initialisation response.
   * @return device information extracted from the initialisation response.
   *
   */
  private HashMap<String, String> parseInitializeResponse(String response) {
    XmlPullParserFactory factory;
    String currentTag = "";
    HashMap<String, String> deviceInfo = new HashMap<String, String>();

    try {
      factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser xpp = factory.newPullParser();

      xpp.setInput(new StringReader(response));
      int eventType = xpp.getEventType();

      while (eventType != XmlPullParser.END_DOCUMENT) {

        switch (eventType) {
          case XmlPullParser.START_DOCUMENT:
            break;

          case XmlPullParser.START_TAG:
            currentTag = xpp.getName();
            break;

          case XmlPullParser.END_TAG:
            currentTag = xpp.getName();
            break;

          case XmlPullParser.TEXT:
            if (currentTag.equalsIgnoreCase("DEVICEID")) {
              deviceInfo.put("deviceId", xpp.getText());
            }
            if (currentTag.equalsIgnoreCase("SECRETS")) {
              deviceInfo.put("secrets", xpp.getText());
            }
            if (currentTag.equalsIgnoreCase("COMPROMISEDOS")) {
              xpp.getText();
            }
            if (currentTag.equalsIgnoreCase("EARLYDEVICE")) {
              xpp.getText();
            }
            if (currentTag.equalsIgnoreCase("LATEDEVICE")) {
              xpp.getText();
            }
            if (currentTag.equalsIgnoreCase("STATUS")) {
              deviceInfo.put("deviceStatus", xpp.getText());
            }
            break;

        }
        eventType = xpp.next();
      }

    } catch (XmlPullParserException xpe) {
      NMPLog.e(TAG, "Parse xml response exception: " + xpe.getMessage());

    } catch (IOException e) {
      NMPLog.e(TAG, "Read xml response exception: " + e.getMessage());
    }


    return deviceInfo;
  }

  /**
   * Parse the license request response.
   *
   * @param response
   *          xml format license response.
   * @param license
   *          parsed license.
   * @return true if the response contains valid license, otherwise false.
   *
   */
  private boolean parseLicenseResponse(String response, PAKCoreLicense license) {
    XmlPullParserFactory factory;
    String currentTag = "";
    String statusTag = "";
    boolean isSuc = false;

    try {
      factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser xpp = factory.newPullParser();

      xpp.setInput(new StringReader(response));
      int eventType = xpp.getEventType();

      while (eventType != XmlPullParser.END_DOCUMENT) {

        switch (eventType) {
          case XmlPullParser.START_DOCUMENT:
            break;

          case XmlPullParser.START_TAG:
            currentTag = xpp.getName();
            break;

          case XmlPullParser.END_TAG:
            xpp.getName();
            break;

          case XmlPullParser.TEXT:
            if (currentTag.equalsIgnoreCase("STATUS")) {
              statusTag = xpp.getText();
              NMPLog.d(TAG, "status " + statusTag);
              if (statusTag.equals("OK")) {
                isSuc = true;
              }
            }
            if (currentTag.equalsIgnoreCase("DCM")) {
              license.setDcm(xpp.getText());
            }
            if (currentTag.equalsIgnoreCase("DMM")) {
              license.setDmm(xpp.getText());
            }
            break;
        }
        eventType = xpp.next();
      }

    } catch (XmlPullParserException xpe) {
      NMPLog.e(TAG, "Parse xml response exception: " + xpe.getMessage());

    } catch (IOException e) {
      NMPLog.e(TAG, "Read xml response exception: " + e.getMessage());
    }

    return isSuc;
  }
}
