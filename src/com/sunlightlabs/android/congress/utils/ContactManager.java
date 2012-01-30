package com.sunlightlabs.android.congress.utils;

import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.android.congress.providers.BatchOperation;
import com.sunlightlabs.android.congress.providers.LegislatorOperations;

import android.text.TextUtils;
import android.content.Context;
import android.content.ContentResolver;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class ContactManager {

    private static final String TAG = "ContactManager";

    public static void addLegislatorToContacts(Context context, Legislator legislator, 
        byte[] avatar) {
        if (legislator == null) {
            Log.w(TAG, "legislator == null");
            return;
        }
        Log.v(TAG, legislator.getName());
        ContentResolver resolver = context.getContentResolver();
        BatchOperation batchOperation = new BatchOperation(context, resolver);

        LegislatorOperations legislatorOperation = LegislatorOperations.createNewLegislator(
            context, true, batchOperation);

        legislatorOperation.addName(legislator.getName(), legislator.firstName(), 
            legislator.last_name)
            .addAvatar(avatar);

        if (!TextUtils.isEmpty(legislator.phone)) {
            legislatorOperation.addPhone(legislator.phone, Phone.TYPE_WORK);
        }

        batchOperation.execute();
            
    }
}
