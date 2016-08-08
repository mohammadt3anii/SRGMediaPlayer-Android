/**
 * NMPCLogger.java
 *
 * @brief
 *    An object that implements an interface that allows the PAK to print logging messages.
 *
 * Created on 12/01/2014
 *
 * Copyright(c) 2014 Nagravision S.A, All Rights Reserved.
 * This software is the proprietary information of Nagravision S.A.
 */
package ch.srg.mediaplayer.nagra.pak;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import nagra.cpak.api.IPakCoreLogProvider;
import nagra.nmp.sdk.NMPLog;

/**
 * An object that implements an interface that allows the PAK to print logging
 * messages.
 *
 */
@SuppressLint("SimpleDateFormat")
public class NMPCLogger implements IPakCoreLogProvider {
  private final static String TAG = "IPakCoreLog";
  
  private static final String PREFIX = "log";
  private static final int MAX_LOG_FILE_SIZE = 1024 * 1024; // 1 MB

  private String mLogFileName = "";
  private static NMPCLogger sLogger = null;
  private Context mContext = null;


  /**
   * Get the singleton instance of this object.
   * 
   * @return The singleton instance.
   */
  public static NMPCLogger instance(Context context) {
    if (sLogger == null) {
      sLogger = new NMPCLogger(context);
    }
    return sLogger;
  }


  /**
   * Create a new instance of this object.
   */
  private NMPCLogger(Context context) {
    mContext = context;
    initFilePath();
  }


  /**
   * Create a new log file on the devices external storage with the current
   * date/time stamp.
   */
  private void initFilePath() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
    String logFilePath = mContext.getExternalFilesDir(null)
                         + File.separator + "NagraMediaPlayerSample" + File.separator
                         + "Logs";
    File file = new File(logFilePath);
    if (!file.exists()) {
      file.mkdirs();
    }
    mLogFileName = logFilePath + File.separator + PREFIX + sdf.format(new Date());
  }


  /**
   * Log a String message to the console and to the current log file. If the log
   * file is too large then create another log file with current date/timp
   * stamp.
   * 
   * @param message
   *          The string message to log.
   */
  @Override
  public synchronized void logMessage(String message) {
    if (message.contains("[Int-")) {
      NMPLog.i(TAG, message);
    } else if (message.contains("[Pak-WAR")) {
      NMPLog.w(TAG, message);
    } else if (message.contains("[Pak-ERR") || message.contains("[Paf-ERR")) {
      NMPLog.e(TAG, message);
    } else {
      NMPLog.v(TAG, message);
    }
    try {
      File logfile = new File(mLogFileName);
      if (logfile.length() > MAX_LOG_FILE_SIZE) {
        initFilePath();
        logfile = new File(mLogFileName);
      }
      writeFileToSDcard(mLogFileName, message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Attempt to APPEND a message to the specified log file.
   * 
   * @param fileName
   *          the name of the file which stores the log message.
   * @param message
   *          the message need be stored.
   * @throws IOException
   */
  private void writeFileToSDcard(String fileName, String message)
  throws IOException {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS  ");
      RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
      String contents = sdf.format(new Date()) + message + "\r\n";
      raf.seek(raf.length());
      raf.write(contents.getBytes("UTF-8"));
      raf.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
