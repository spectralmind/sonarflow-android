package com.spectralmind.sf4android;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.DISPLAY;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_COMMENT;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/*
@ReportsCrashes(formKey = "", // will not be used
mailTo = "android.crashes@spectralmind.com", 
customReportContent = { USER_COMMENT, ANDROID_VERSION, APP_VERSION_NAME, BRAND,  PHONE_MODEL, DISPLAY, CUSTOM_DATA, STACK_TRACE, LOGCAT }, 
mode = ReportingInteractionMode.DIALOG,
resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
resDialogText = R.string.crash_dialog_text,
resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
		)
		    */
public class SonarflowApplication extends Application {

	private static Context context;

	public void onCreate() {
		super.onCreate();
		SonarflowApplication.context = getApplicationContext();

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}

	public static Context getAppContext() {
		return SonarflowApplication.context;
	}
	
	/** Compiles details about the user's device such as OS version
	 * and device name. Returned as human readable string
	 * @return
	 */
	public static String getUserDeviceDetailsAsString() {
		StringBuffer sb = new StringBuffer(getAppContext().getString(R.string.userdeviceintro));
		PackageInfo pinfo;
		DisplayMetrics dm = getAppContext().getResources().getDisplayMetrics();

		sb.append( SonarflowApplication.getAppContext().getString(R.string.ANDROID_VERSION)	+ android.os.Build.VERSION.RELEASE 	+ "\n");
		sb.append(SonarflowApplication.getAppContext().getString(R.string.BRAND)	+ android.os.Build.BRAND 			+ "\n");
		sb.append(SonarflowApplication.getAppContext().getString(R.string.MODEL)		+ android.os.Build.MODEL 			+ "\n");
		sb.append(SonarflowApplication.getAppContext().getString(R.string.DENSITY)	 + dm.xdpi + "x" +  dm.ydpi + " dpi\n");
		sb.append(SonarflowApplication.getAppContext().getString(R.string.RESOLUTION)	+ dm.widthPixels + "x" +  dm.heightPixels + " px\n");
		
		try {
			pinfo = getAppContext().getPackageManager().getPackageInfo(getAppContext().getPackageName(), 0);
			sb.append(SonarflowApplication.getAppContext().getString(R.string.APP_VERSION_NAME)	+ pinfo.versionName 				+ "\n");
		} catch (NameNotFoundException e) {
			sb.append( SonarflowApplication.getAppContext().getString(R.string.COULD_NOT_RETRIEVE_APP_VERSION_NAME)+"\n");
		}
		
		return sb.toString();

	}
	
	
	// taken from http://stackoverflow.com/a/9563438
	/**
	 * This method convets dp unit to equivalent device specific value in pixels. 
	 * 
	 * @param dp A value in dp(Device independent pixels) unit. Which we need to convert into pixels
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent Pixels equivalent to dp according to device
	 */
	public static float convertDpToPixel(float dp,Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float px = dp * (metrics.densityDpi/160f);
	    return px;
	}
	/**
	 * This method converts device specific pixels to device independent pixels.
	 * 
	 * @param px A value in px (pixels) unit. Which we need to convert into db
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent db equivalent to px value
	 */
	public static float convertPixelsToDp(float px,Context context){
	    Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;

	}

}
