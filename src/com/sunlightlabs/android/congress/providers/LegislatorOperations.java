package com.sunlightlabs.android.congress.providers;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;


/**
 * Helper for storing data in content providers
 * */
public class LegislatorOperations {

    private final ContentValues values;
  
    private final BatchOperation batchOperation;
  
    private final Context context;
  
    private long rawContactId;
  
    private int backReference;
  
    private boolean isNewContact;
  
    private boolean isSyncOperation;
 
    /**
    * Since we're sending a lot of contact provider operations in a single 
    * batched operation, we want to make sure that we "yield" periodically
    * so that the Contact Provider can write changes to the DB, and can
    * open a new transaction.  This prevents ANR (application not responding)
    * errors.  The recommended time to specify that a yield is permitted is
    * with the first operation on a particular contact.  So if we're updating
    * multiple fields for a single legislator, we make sure that we call
    * withYieldAllowed(true) on the first field that we update. We use
    * isYieldAllowed to keep track of what value we should pass to
    * withYieldAllowed().
    * */
    private boolean isYieldAllowed;

    public static LegislatorOperations createNewLegislator(Context context, boolean isSyncOperation, BatchOperation batchOperation) {
        return new LegislatorOperations(context, isSyncOperation, batchOperation);        
    }

    public LegislatorOperations(Context context, boolean isSyncOperation, BatchOperation batchOperation) {
        this.values = new ContentValues();
        this.isYieldAllowed = true;
        this.isSyncOperation = isSyncOperation;
        this.context = context;
        this.batchOperation = batchOperation;
        this.backReference = this.batchOperation.size();
        this.isNewContact = true;

        ContentProviderOperation.Builder builder = newInsertOperation(RawContacts.CONTENT_URI, this.isSyncOperation, true)
            .withValues(this.values);

        this.batchOperation.add(builder.build());
    }

    /** 
    * Adds a contact name. We can take either a full name ("Bob Smith") or separated
    * first-name and last-name ("Bob" and "Smith").
    * 
    * @param fullName The full name of the contact.
    *      Can be null if firstName/lastName are specified.
    * @param firstName The first name of the contact - can be null if fullName
    *      is specified.
    * @param lastName The last name of the contact - can be null if fullName
    *      is specified.
    * @return instance of LegislatorOperations
    **/
    public LegislatorOperations addName(String fullName, String firstName, String lastName) {
        this.values.clear();

        if (!TextUtils.isEmpty(fullName)) {
            this.values.put(StructuredName.DISPLAY_NAME, fullName);
            this.values.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        } else {
            if (!TextUtils.isEmpty(firstName)) {
                this.values.put(StructuredName.GIVEN_NAME, firstName);
                this.values.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            }
            if (!TextUtils.isEmpty(lastName)) {
                this.values.put(StructuredName.FAMILY_NAME, lastName);
                this.values.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            }
        }

        if (this.values.size() > 0) {
            this.addInsertOperation();
        }

        return this;
    }

    /**
     * Adds a phone number with an optional label
     * */
    public LegislatorOperations addPhone(String number, int type, String label) {
        this.values.clear();
        if (!TextUtils.isEmpty(number)) {
            this.values.put(Phone.NUMBER, number);
            this.values.put(Phone.TYPE, type);
            if (!TextUtils.isEmpty(label)) {
                this.values.put(Phone.LABEL, label);
            }
            this.values.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            this.addInsertOperation();
        }
 
        return this;
    }

    /**
     * Adds a phone number with no label
     * */
    public LegislatorOperations addPhone(String number, int type) {
        this.addPhone(number, type, null);
 
        return this;
    }

    public LegislatorOperations addEmail(String email) {
        if (!TextUtils.isEmpty(email)) {
            this.values.clear();
            this.values.put(Email.DATA, email);
            this.values.put(Email.TYPE, Email.TYPE_OTHER);
            this.values.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            this.addInsertOperation();
        }

        return this;
    }

    public LegislatorOperations addAvatar(byte[] avatar) {
        if (avatar != null) {
            this.values.clear();
            this.values.put(Photo.PHOTO, avatar);
            this.values.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            this.addInsertOperation();
        }

        return this;
    }

    private void addInsertOperation() {
        ContentProviderOperation.Builder builder = newInsertOperation(Data.CONTENT_URI,
            this.isSyncOperation, this.isYieldAllowed);

        builder.withValues(this.values);
        if (this.isNewContact) {
            builder.withValueBackReference(Data.RAW_CONTACT_ID, this.backReference);
        }
        this.isYieldAllowed = false;
        this.batchOperation.add(builder.build());

    }

    public static ContentProviderOperation.Builder newInsertOperation(Uri uri, boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
            .withYieldAllowed(isYieldAllowed);
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            return uri.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        }
        return uri;
    }
} 
