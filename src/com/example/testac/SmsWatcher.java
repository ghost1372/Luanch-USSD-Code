package com.example.testac;

import android.database.Cursor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import java.util.ArrayList;

public class SmsWatcher extends BroadcastReceiver {
    public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String MY_CONTACTS_ID = "my_contacts";
    public static final String PHONE_NUMBER_ID = "09010000000";
    
    public static final String[] mContactProjection = {
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
    };

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_SMS_RECEIVED.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) {
                return;
            }

            StringBuilder messageBody = new StringBuilder();
            SmsMessage sms = null;
            for (Object p : pdus) {
                sms = SmsMessage.createFromPdu((byte[]) p);
                messageBody.append(sms.getDisplayMessageBody());
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean onlyFromMyContacts = prefs.getBoolean(MY_CONTACTS_ID, false);
            String from = sms.getDisplayOriginatingAddress();
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(from));
            Cursor cur = ctx.getContentResolver().query(lookupUri, mContactProjection, null, null, null);
            String contactName = null;
            if (cur.moveToNext()) {
                contactName = cur.getString(cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            }
            cur.close();

            if (onlyFromMyContacts && (contactName == null)) {
                return;
            }

            String prefix = ctx.getString(R.string.fwd_from) + " "
                    + ((contactName == null || "".equals(contactName)) ? "" : contactName + " ")
                    + "<" + from + ">:";
            String forwardedMessageBody = prefix + " " + messageBody.toString();
            String to = prefs.getString(PHONE_NUMBER_ID, null);

            SmsManager manager = SmsManager.getDefault();
            manager.sendMultipartTextMessage(to, null, manager.divideMessage(forwardedMessageBody), null, null);
        }
    }
}
