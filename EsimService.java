package md.moldcell.selfservice.utils.esim;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.List;

import static md.moldcell.selfservice.utils.StringUtil.isNotNullAndNotEmpty;

@RequiresApi(api = Build.VERSION_CODES.P)
public class EsimService {

    private static final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    private static final String ACTION_SIM_RESULT = "sim_result";
    public static final String ESIM_TAG = "EsimService";
    private final OnEsimDownloadListener downloadListener;
    private final Context context;
    private final Activity activity;

    public EsimService(Context context, OnEsimDownloadListener downloadListener, Activity activity) {
        this.context = context;
        this.downloadListener = downloadListener;
        this.activity = activity;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = getResultCode();
            int detailCode = intent.getIntExtra(
                    EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);

            Log.d(ESIM_TAG, "onReceive: detailedCode: " + detailCode);
            Log.d(ESIM_TAG, "onReceive: resultCode: " + resultCode);

            boolean isDownloadAction = isNotNullAndNotEmpty(intent.getAction()) &&
                    intent.getAction().equals(ACTION_DOWNLOAD_SUBSCRIPTION);

            boolean isSimResultAction = isNotNullAndNotEmpty(intent.getAction()) &&
                    intent.getAction().equals(ACTION_SIM_RESULT);

            if (isDownloadAction) {
                if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
                    downloadListener.onEsimSuccess("eSIM is active");
                } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                    EuiccManager euiccManager = (EuiccManager) context
                            .getSystemService(Context.EUICC_SERVICE);

                    PendingIntent callbackIntent = PendingIntent.getBroadcast(context, 0,
                            new Intent(ACTION_SIM_RESULT),
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    context.registerReceiver(receiver, new IntentFilter(ACTION_SIM_RESULT),
                            null, null);
                    try {
                        euiccManager.startResolutionActivity(activity, 0,
                                intent, callbackIntent);
                    } catch (IntentSender.SendIntentException e) {
                        downloadListener.onEsimFailure("Resolution activity error");
                        e.printStackTrace();
                    }
                } else {
                    downloadListener
                            .onEsimFailure("Download profile was failed " + detailCode);
                }
            } else if (isSimResultAction) {
                if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
                    downloadListener.onEsimSuccess("eSim is active");
                } else {
                    downloadListener
                            .onEsimFailure("Download profile was failed " + detailCode);
                }
            }
        }
    };

    /**
     * Download eSIM profile.
     *
     * @param code LPA code.
     */
    public void downloadEsim(String code) {
        EuiccManager euiccManager = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);

        if (!euiccManager.isEnabled()) {
            downloadListener.onEsimFailure("eSIM installation is unsupported on your device");
            return;
        }

        try {
            DownloadableSubscription subscription = DownloadableSubscription.forActivationCode(code);
            Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION);

            PendingIntent callbackIntent = PendingIntent.getBroadcast(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            context.registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION),
                    null, null);

            euiccManager.downloadSubscription(subscription, true, callbackIntent);
        } catch (Exception e) {
            downloadListener.onEsimFailure("Error with download subscription");
            e.printStackTrace();
        }
    }
    
    /**
     * Destroy broadcast receiver.
     */
    public void onDestroy() {
        context.unregisterReceiver(receiver);
    }

}