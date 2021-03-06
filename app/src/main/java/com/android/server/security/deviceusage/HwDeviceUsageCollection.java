package com.android.server.security.deviceusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Slog;

public class HwDeviceUsageCollection {
    private static final String ACTION_CALLS_TABLE_ADD_ENTRY = "com.android.server.telecom.intent.action.CALLS_ADD_ENTRY";
    private static final String CALL_DURATION = "duration";
    private static final long CALL_ENOUGH_TIME = 600;
    private static final long CHARGING_ENOUGH_TIME = 5;
    private static final boolean HW_DEBUG = false;
    private static final long SCREENON_ENOUGH_TIME = 36000;
    private static final String TAG = "HwDeviceUsageCollection";
    public static final int TYPE_GET_TIME = 6;
    public static final int TYPE_HAS_GET_TIME = 7;
    private static final int TYPE_OBTAIN_CALL_LOG = 1;
    private static final int TYPE_OBTAIN_CHARGING = 2;
    private static final int TYPE_OBTAIN_SCREEN_OFF = 4;
    private static final int TYPE_OBTAIN_SCREEN_ON = 3;
    public static final int TYPE_REPORT_TIME = 5;
    private boolean isGetTime;
    private Runnable judgeIsDeviceUseRunnable;
    private final BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private HwDeviceUsageOEMINFO mHwDeviceUsageOEMINFO;
    private long mScreenOnTime;
    private TelephonyManager mTelephonyManager;

    /* renamed from: com.android.server.security.deviceusage.HwDeviceUsageCollection.3 */
    class AnonymousClass3 extends Handler {
        AnonymousClass3(Looper $anonymous0) {
            super($anonymous0);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HwDeviceUsageCollection.TYPE_OBTAIN_CALL_LOG /*1*/:
                    if (msg.obj != null) {
                        HwDeviceUsageCollection.this.handleCallLog(((Long) msg.obj).longValue());
                        break;
                    }
                case HwDeviceUsageCollection.TYPE_OBTAIN_CHARGING /*2*/:
                    HwDeviceUsageCollection.this.handleCharging();
                    break;
                case HwDeviceUsageCollection.TYPE_OBTAIN_SCREEN_ON /*3*/:
                    HwDeviceUsageCollection.this.handleScreenOn();
                    break;
                case HwDeviceUsageCollection.TYPE_OBTAIN_SCREEN_OFF /*4*/:
                    HwDeviceUsageCollection.this.handleScreenOff();
                    break;
                case HwDeviceUsageCollection.TYPE_HAS_GET_TIME /*7*/:
                    if (msg.obj != null) {
                        HwDeviceUsageCollection.this.handleHasGetTime(((Long) msg.obj).longValue());
                        break;
                    }
                default:
                    Slog.e(HwDeviceUsageCollection.TAG, "obtain error message");
                    break;
            }
        }
    }

    static {
        /* JADX: method processing error */
/*
        Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.security.deviceusage.HwDeviceUsageCollection.<clinit>():void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:113)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:281)
	at jadx.api.JavaClass.decompile(JavaClass.java:59)
	at jadx.api.JadxDecompiler$1.run(JadxDecompiler.java:161)
Caused by: jadx.core.utils.exceptions.DecodeException:  in method: com.android.server.security.deviceusage.HwDeviceUsageCollection.<clinit>():void
	at jadx.core.dex.instructions.InsnDecoder.decodeInsns(InsnDecoder.java:46)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:98)
	... 5 more
Caused by: java.lang.IllegalArgumentException: bogus opcode: 0073
	at com.android.dx.io.OpcodeInfo.get(OpcodeInfo.java:1197)
	at com.android.dx.io.OpcodeInfo.getFormat(OpcodeInfo.java:1212)
	at com.android.dx.io.instructions.DecodedInstruction.decode(DecodedInstruction.java:72)
	at jadx.core.dex.instructions.InsnDecoder.decodeInsns(InsnDecoder.java:43)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.security.deviceusage.HwDeviceUsageCollection.<clinit>():void");
    }

    public HwDeviceUsageCollection(Context context) {
        this.mScreenOnTime = -1;
        this.isGetTime = HW_DEBUG;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (HwDeviceUsageCollection.HW_DEBUG) {
                    Slog.d(HwDeviceUsageCollection.TAG, "action  " + action);
                }
                if ("android.intent.action.ACTION_POWER_CONNECTED".equals(action)) {
                    HwDeviceUsageCollection.this.obtainCharging();
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    HwDeviceUsageCollection.this.obtainScreenOff();
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    HwDeviceUsageCollection.this.obtainScreenOn();
                    if (HwDeviceUsageCollection.HW_DEBUG) {
                        Slog.d(HwDeviceUsageCollection.TAG, "mScreenOnTime  action " + HwDeviceUsageCollection.this.mScreenOnTime);
                    }
                } else if (!HwDeviceUsageCollection.ACTION_CALLS_TABLE_ADD_ENTRY.equals(action)) {
                    Slog.e(HwDeviceUsageCollection.TAG, "Receive error broadcast");
                } else if (HwDeviceUsageCollection.this.mTelephonyManager != null && HwDeviceUsageCollection.this.mTelephonyManager.getSimState() != HwDeviceUsageCollection.TYPE_OBTAIN_CALL_LOG) {
                    HwDeviceUsageCollection.this.obtainCallLog(intent.getLongExtra(HwDeviceUsageCollection.CALL_DURATION, 0));
                }
            }
        };
        this.judgeIsDeviceUseRunnable = new Runnable() {
            public void run() {
                if (!HwDeviceUsageCollection.this.isGetTime) {
                    boolean isDeviceUsed = (HwDeviceUsageCollection.this.getScreenOnTime() < HwDeviceUsageCollection.SCREENON_ENOUGH_TIME || HwDeviceUsageCollection.this.getChargeTime() < HwDeviceUsageCollection.CHARGING_ENOUGH_TIME) ? HwDeviceUsageCollection.HW_DEBUG : HwDeviceUsageCollection.this.getTalkTime() >= HwDeviceUsageCollection.CALL_ENOUGH_TIME ? true : HwDeviceUsageCollection.HW_DEBUG;
                    if (HwDeviceUsageCollection.HW_DEBUG) {
                        Slog.d(HwDeviceUsageCollection.TAG, "isDeviceUsed = " + isDeviceUsed);
                    }
                    if (isDeviceUsed) {
                        HwDeviceUsageCollection.this.isFirstUseDevice();
                    }
                }
            }
        };
        if (HW_DEBUG) {
            Slog.d(TAG, TAG);
        }
        this.mContext = context;
        this.mHwDeviceUsageOEMINFO = HwDeviceUsageOEMINFO.getInstance();
    }

    public void onStart() {
        if (HW_DEBUG) {
            Slog.d(TAG, "onStart");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        intentFilter.addAction(ACTION_CALLS_TABLE_ADD_ENTRY);
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mHandlerThread = new HandlerThread("HwDeviceUsageCollectionThread");
        this.mHandlerThread.start();
        this.mHandler = new AnonymousClass3(this.mHandlerThread.getLooper());
        this.mHandler.post(this.judgeIsDeviceUseRunnable);
    }

    private void obtainCallLog(long duration) {
        if (duration > 0) {
            Message msg = Message.obtain();
            msg.what = TYPE_OBTAIN_CALL_LOG;
            msg.obj = Long.valueOf(duration / 1000);
            this.mHandler.sendMessage(msg);
        }
    }

    private void obtainCharging() {
        Message msg = Message.obtain();
        msg.what = TYPE_OBTAIN_CHARGING;
        this.mHandler.sendMessage(msg);
    }

    private void obtainScreenOn() {
        Message msg = Message.obtain();
        msg.what = TYPE_OBTAIN_SCREEN_ON;
        this.mHandler.sendMessage(msg);
    }

    private void obtainScreenOff() {
        Message msg = Message.obtain();
        msg.what = TYPE_OBTAIN_SCREEN_OFF;
        this.mHandler.sendMessage(msg);
    }

    private boolean isTimeNull() {
        return getFristUseTime() == 0 ? true : HW_DEBUG;
    }

    private void handleCharging() {
        long mChargeTime = getChargeTime() + 1;
        if (HW_DEBUG) {
            Slog.d(TAG, "mChargeTime " + mChargeTime);
        }
        setChargeTime(mChargeTime);
        this.mHandler.post(this.judgeIsDeviceUseRunnable);
    }

    private void handleScreenOn() {
        this.mScreenOnTime = SystemClock.elapsedRealtime();
    }

    private void handleScreenOff() {
        if (this.mScreenOnTime >= 0) {
            long onTime = (SystemClock.elapsedRealtime() - this.mScreenOnTime) / 1000;
            if (HW_DEBUG) {
                Slog.d(TAG, "mOnTime " + onTime);
            }
            this.mScreenOnTime = -1;
            if (onTime > 0) {
                long screenOnTime = onTime + getScreenOnTime();
                if (HW_DEBUG) {
                    Slog.d(TAG, "screenOnTime " + screenOnTime);
                }
                setScreenOnTime(screenOnTime);
                this.mHandler.post(this.judgeIsDeviceUseRunnable);
            }
        }
    }

    private void handleCallLog(long talkTime) {
        long mTalkTime = talkTime + getTalkTime();
        if (HW_DEBUG) {
            Slog.d(TAG, "mTalkTime " + mTalkTime);
        }
        setTalkTime(mTalkTime);
        this.mHandler.post(this.judgeIsDeviceUseRunnable);
    }

    private void isFirstUseDevice() {
        if (isTimeNull()) {
            getTime();
        }
    }

    private void reportTime(long time) {
        HwDeviceUsageReport mHwDeviceUsageReport = new HwDeviceUsageReport(this.mContext);
        if (!isTimeNull()) {
            mHwDeviceUsageReport.reportFirstUseTime(time);
        }
    }

    private void getTime() {
        if (!this.isGetTime) {
            this.isGetTime = true;
            HwDeviceFirstUseTime mHwDeviceFirstUseTime = new HwDeviceFirstUseTime(this.mContext, this.mHandler);
            mHwDeviceFirstUseTime.start();
            mHwDeviceFirstUseTime.triggerGetFirstUseTime();
        }
    }

    private void handleHasGetTime(long time) {
        if (setFristUseTime(time) == TYPE_OBTAIN_CALL_LOG) {
            reportTime(time);
        }
    }

    protected boolean getOpenFlag() {
        return this.mHwDeviceUsageOEMINFO.getOpenFlag();
    }

    protected long getScreenOnTime() {
        return this.mHwDeviceUsageOEMINFO.getScreenOnTime();
    }

    protected long getChargeTime() {
        return this.mHwDeviceUsageOEMINFO.getChargeTime();
    }

    protected long getTalkTime() {
        return this.mHwDeviceUsageOEMINFO.getTalkTime();
    }

    protected long getFristUseTime() {
        return this.mHwDeviceUsageOEMINFO.getFristUseTime();
    }

    protected void setOpenFlag(int flag) {
        this.mHwDeviceUsageOEMINFO.setOpenFlag(flag);
    }

    protected void setScreenOnTime(long time) {
        this.mHwDeviceUsageOEMINFO.setScreenOnTime(time);
    }

    protected void setChargeTime(long time) {
        this.mHwDeviceUsageOEMINFO.setChargeTime(time);
    }

    protected void setTalkTime(long time) {
        this.mHwDeviceUsageOEMINFO.setTalkTime(time);
    }

    protected int setFristUseTime(long time) {
        return this.mHwDeviceUsageOEMINFO.setFristUseTime(time);
    }
}
