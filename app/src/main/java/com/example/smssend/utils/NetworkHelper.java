package com.example.smssend.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.*;
import androidx.core.net.ConnectivityManagerCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Dylan on 2018/3/21.
 */

public class NetworkHelper {

	public final static boolean DEBUG = false;

	public final static String TAG = "NetworkHelper";

	/**
	 * Network must be unavaliable.
	 */
	public final static int TYPE_UNAVAILABLE = 0;
	/**
	 * Network must be connected and unmetered.
	 */
	public final static int TYPE_UNMETERED = 1;
	/**
	 * This job requires metered connectivity such as most cellular data networks.
	 */
	public final static int TYPE_METERED = 2;
	/**
	 * Network must be roaming, but can be metered.
	 */
	public final static int TYPE_ROAMING = 3;

	public final static int STATE_UNKNOWN = 0;
	public final static int STATE_CONNECTING = 1;
	public final static int STATE_CONNECTED = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({TYPE_UNAVAILABLE, TYPE_UNMETERED, TYPE_METERED, TYPE_ROAMING})
	public @interface NetworkCategory {
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATE_UNKNOWN, STATE_CONNECTING, STATE_CONNECTED})
	public @interface NetworkState {
	}

	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static String getNetworkType(Context context) {
		String strNetworkType = "";

		NetworkInfo networkInfo = ((ConnectivityManager) context.getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected()) {
			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				strNetworkType = "WIFI";
			} else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				String _strSubTypeName = networkInfo.getSubtypeName();

				// TD-SCDMA   networkType is 17
				int networkType = networkInfo.getSubtype();
				switch (networkType) {
					case TelephonyManager.NETWORK_TYPE_GPRS:
					case TelephonyManager.NETWORK_TYPE_GSM:
					case TelephonyManager.NETWORK_TYPE_EDGE:
					case TelephonyManager.NETWORK_TYPE_CDMA:
					case TelephonyManager.NETWORK_TYPE_1xRTT:
					case TelephonyManager.NETWORK_TYPE_IDEN:
						strNetworkType = "2G";
						break;
					case TelephonyManager.NETWORK_TYPE_UMTS:
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
					case TelephonyManager.NETWORK_TYPE_HSDPA:
					case TelephonyManager.NETWORK_TYPE_HSUPA:
					case TelephonyManager.NETWORK_TYPE_HSPA:
					case TelephonyManager.NETWORK_TYPE_EVDO_B:
					case TelephonyManager.NETWORK_TYPE_EHRPD:
					case TelephonyManager.NETWORK_TYPE_HSPAP:
					case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
						strNetworkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_LTE:
					case TelephonyManager.NETWORK_TYPE_IWLAN:
					case 19://TelephonyManager.NETWORK_TYPE_LTE_CA:
						strNetworkType = "4G";
						break;
					default:
						// http://baike.baidu.com/item/TD-SCDMA 中国移动 联通 电信 三种3G制式
						if (_strSubTypeName.equalsIgnoreCase("WCDMA") ||
								_strSubTypeName.equalsIgnoreCase("CDMA2000")) {
							strNetworkType = "3G";
						} else {
							strNetworkType = _strSubTypeName;
						}

						break;
				}
			} else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
				strNetworkType = "ETHERNET";
			} else if (networkInfo.getType() == ConnectivityManager.TYPE_VPN) {
				strNetworkType = "VPN";
			}
		}
		return strNetworkType;
	}


	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static String getOperName(Context context) {
		String operName = "";

		NetworkInfo networkInfo = ((ConnectivityManager) context.getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected()) {
			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {

				return  getSimOperName(context);
			}
		}

		return operName;
	}


	public static String getSimOperName(Context context) {

		try {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			return tm.getSimOperator();
		} catch (Exception e) {

		}

		return  "";


	}


	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		return info != null && info.isAvailable();
	}

	@NetworkCategory
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static int getNetworkCategory(Context context) {

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
//		Log.i("NetworkHelper", "getNetworkCategory: " + info);
		return getNetworkCategory(cm, info);
	}

	@NetworkState
	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public static int getNetworkState(Context context) {

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		return getNetworkState(info);
	}

	@NetworkCategory
	private static int getNetworkCategory(ConnectivityManager cm, NetworkInfo info) {

		if (info == null || !info.isConnectedOrConnecting()) {
			return TYPE_UNAVAILABLE;
		} else if (!ConnectivityManagerCompat.isActiveNetworkMetered(cm)) {
			return TYPE_UNMETERED;
		} else if (info.isRoaming()) {
			return TYPE_ROAMING;
		} else {
			return TYPE_METERED;
		}
	}

	@NetworkState
	private static int getNetworkState(NetworkInfo info) {
		if (info == null || !info.isConnectedOrConnecting()) {
			return STATE_UNKNOWN;
		} else if (info.isConnected()) {
			return STATE_CONNECTED;
		} else {
			return STATE_CONNECTING;
		}
	}

	public interface NetworkChangedListener {

		/**
		 * 第一次回调在调用的线程被调用，之后Android 24以下在主线程中被调用，24及以上在工作线程被调用
		 *
		 * @param category
		 * @param state
		 */
		@AnyThread
		void onNetworkChanged(@NetworkCategory int category, @NetworkState int state);
	}

	private ManyNetworkChangedListener many;
	private NetworkHelperCompatImpl impl;

	public NetworkHelper(Context context) {
		this.many = new ManyNetworkChangedListener();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			impl = new NetworkHelperCompatApi24Impl(context);
		} else {
			impl = new NetworkHelperCompatBroadcastImpl(context);
		}
	}

	@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
	public void register(@NonNull NetworkChangedListener listener) {
		if (many.getRegisteredCount() == 0) {
			impl.observe(many);
		}
		many.register(listener);
	}

	public void unregister(@NonNull NetworkChangedListener listener) {
		many.unregister(listener);
		if (many.getRegisteredCount() == 0) {
			impl.unobserve();
		}
	}

	public int getLastType() {
		return many.lastType;
	}

	public int getLastState() {
		return many.lastState;
	}

	class ManyNetworkChangedListener implements NetworkChangedListener {

		private SparseArray<NetworkChangedListener> listeners;
		private Integer lastType;
		private Integer lastState;

		public ManyNetworkChangedListener() {
			this.listeners = new SparseArray<>();
		}

		public int getRegisteredCount() {
			return listeners.size();
		}

		@Override
		public void onNetworkChanged(@NetworkCategory int type, @NetworkState int state) {

			Log.i(TAG, "onNetworkChanged: " + lastState + ", " + state);
			// 避免重复调用
			if (lastType == null || lastState == null || type != lastType || state != lastState) {

				for (int i = 0; i < listeners.size(); i++) {
					listeners.valueAt(i).onNetworkChanged(type, state);
				}
			}

			lastType = type;
			lastState = state;
		}

		public void register(@NonNull NetworkChangedListener listener) {
			listeners.put(listener.hashCode(), listener);
			if (lastType != null && lastState != null) {
				listener.onNetworkChanged(lastType, lastState);
			}
		}

		public void unregister(@NonNull NetworkChangedListener listener) {
			listeners.remove(listener.hashCode());
		}
	}


	interface NetworkHelperCompatImpl {

		void observe(NetworkChangedListener listener);

		void unobserve();
	}

	static final class NetworkHelperCompatBroadcastImpl extends BroadcastReceiver implements NetworkHelperCompatImpl {


		private Context context;
		private NetworkChangedListener listener;

		public NetworkHelperCompatBroadcastImpl(@NonNull Context context) {
			this.context = context;
		}

		@Override
		@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
		public void observe(NetworkChangedListener listener) {
			this.listener = listener;
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			context.registerReceiver(this, filter);

			onReceive(context, null);
		}

		@Override
		public void unobserve() {
			context.unregisterReceiver(this);
			this.listener = null;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (listener == null) {
				return;
			}
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeInfo = cm.getActiveNetworkInfo();
			NetworkInfo info = activeInfo;
			if (intent != null) {
				NetworkInfo changeInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (DEBUG) {
					Log.i(TAG, "onReceive: " + intent.getAction() + ",\n" + changeInfo + " " + changeInfo.getType() + ",\n" + activeInfo + " " + (activeInfo == changeInfo));
				}

				if (getNetworkState(changeInfo) == STATE_CONNECTED) {
					info = changeInfo;
				}
			}
			listener.onNetworkChanged(getNetworkCategory(cm, info), getNetworkState(info));
		}
	}

	@RequiresApi(Build.VERSION_CODES.N)
	static final class NetworkHelperCompatApi24Impl extends NetworkCallback implements NetworkHelperCompatImpl {

		public final static String TAG = "NetworkHelperApi24";
		private Context context;
		private ConnectivityManager connectivityManager;
		private NetworkChangedListener listener;

		public NetworkHelperCompatApi24Impl(@NonNull Context context) {
			this.context = context;
			this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}

		@Override
		@RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
		public void observe(NetworkChangedListener listener) {
			this.listener = listener;
			this.connectivityManager.registerDefaultNetworkCallback(this);

			onNetworkChanged(connectivityManager.getActiveNetwork());
		}

		@Override
		public void unobserve() {
			this.connectivityManager.unregisterNetworkCallback(this);
			this.listener = null;
		}

		@Override
		public void onAvailable(Network network) {
			super.onAvailable(network);
			ConnectivityManager cm = connectivityManager;
			if (DEBUG) Log.i(TAG, "onAvailable: " + network + ", " + cm.getNetworkInfo(network) + ", " + cm.getActiveNetwork());

			onNetworkChanged(network);
		}

		@Override
		public void onLosing(Network network, int maxMsToLive) {
			super.onLosing(network, maxMsToLive);
			if (DEBUG) Log.i(TAG, "onLosing: " + network + ", " + maxMsToLive + ", " + connectivityManager.getNetworkInfo(network));
			onNetworkChanged(network);
		}

		@Override
		public void onLost(Network network) {
			super.onLost(network);
			if (DEBUG) Log.i(TAG, "onLost: " + network + ", " + connectivityManager.getNetworkInfo(network));
			onNetworkChanged(network);
		}

		@Override
		public void onUnavailable() {
			super.onUnavailable();
			if (DEBUG) Log.i(TAG, "onUnavailable: ");
			onNetworkChanged(connectivityManager.getActiveNetwork());
		}

		@Override
		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
			super.onCapabilitiesChanged(network, networkCapabilities);
			if (DEBUG) Log.i(TAG, "onCapabilitiesChanged: " + network + ", " + networkCapabilities);
		}

		@Override
		public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
			super.onLinkPropertiesChanged(network, linkProperties);
			if (DEBUG) Log.i(TAG, "onLinkPropertiesChanged: " + network + ", " + linkProperties);
		}

		private void onNetworkChanged(Network network) {
			if (listener == null) {
				return;
			}

			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info;
			if (network != null) {
				info = cm.getNetworkInfo(network);
			} else {
				info = cm.getActiveNetworkInfo();
			}

			listener.onNetworkChanged(getNetworkCategory(cm, info), getNetworkState(info));
		}
	}
}
