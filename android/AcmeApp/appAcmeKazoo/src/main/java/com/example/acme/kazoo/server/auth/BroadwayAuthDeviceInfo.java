package com.example.acme.kazoo.server.auth;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.location.Location;

import com.blackmoonit.androidbits.utils.BitsAppUtils;
import com.example.acme.kazoo.R;

import java.util.Date;

public class BroadwayAuthDeviceInfo extends com.blackmoonit.androidbits.auth.BroadwayAuthDeviceInfo
{
	/**
	 * Gather various device information to send to the server for use in BroadwayAuth mechanism.
	 *
	 * @param aContext - context used for getting app and device info.
	 */
	public BroadwayAuthDeviceInfo(Context aContext) {
		super(aContext);
	}

	/**
	 * Standard device information sent to the server to determine auth status.
	 * Non-volatile information that should not change between API calls.
	 * @return Returns the various data collected to present to the server.
	 */
	@SuppressWarnings("deprecation")
	public String[] getMyDeviceFingerprints() {
		Context theContext = mContext;
		//app environment
		PackageInfo pi = BitsAppUtils.getAppPackageInfo(theContext, PackageManager.GET_SIGNATURES);
		//app resources
		Resources theResources = theContext.getResources();
		//device info
		ActivityManager theActMgr = (ActivityManager) theContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo myDeviceMemInfo = new ActivityManager.MemoryInfo();
		theActMgr.getMemoryInfo(myDeviceMemInfo);

		int theSigHash;
		if (pi.signatures.length > 1) {
			StringBuilder theSigsBuilder = new StringBuilder();

			for (Signature theSig : pi.signatures) {
				theSigsBuilder.append(theSig.toCharsString()).append(",");
			}
			theSigsBuilder.deleteCharAt(theSigsBuilder.length() - 1);
			theSigHash = theSigsBuilder.toString().hashCode();
		} else {
			theSigHash = pi.signatures[0].toCharsString().hashCode();
		}

		//List these values in the same order the server will expect them.
		return new String[] {
				String.valueOf(theSigHash),
				BitsAppUtils.getAndroidID(theContext),
				BitsAppUtils.getDeviceID(theContext),
				theResources.getConfiguration().locale.toString(),
				String.valueOf(myDeviceMemInfo.threshold), //API 1+, whereas totalMem is 16+
		};
	}

	/**
	 * Volatile device information sent to the server to determine auth status.
	 * This covers information such as GPS location and timestamp.
	 * @return Returns the various data collected to present to the server.
	 */
	public String[] getMyDeviceCircumstances() {
		Context theContext = mContext;
		Location theLoc = null;
		String theName = BitsAppUtils.getAppVersionName(theContext);
		if (theContext!=null) {
			theName = theContext.getString(R.string.app_name)+" "+theName;
			//gps, if available, but only if it is less than a week old
			long theRememberMeUntilDate = (new Date()).getTime() - (1000 * 60 * 60 * 24 * 7); // 7 day old data
			theLoc = BitsAppUtils.getLastKnownLocation(theContext, theRememberMeUntilDate);
		}

		//List these values in the same order the server will expect them.
		return new String[]{
				String.valueOf(System.currentTimeMillis()),
				theName,
				((theLoc!=null) ? String.valueOf(theLoc.getLatitude()) : "?"),
				((theLoc!=null) ? String.valueOf(theLoc.getLongitude()) : "?"),
		};
	}

}
