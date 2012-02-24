package com.sunlightlabs.android.congress.providers;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

/**
 * Handles execution of batch operations on a Contacts provider
 * */
final public class BatchOperation {

	private static final String TAG = "BatchOperation";

	private final ContentResolver resolver;

	private final ArrayList<ContentProviderOperation> operations;

	public BatchOperation(Context c, ContentResolver resolver) {
		this.resolver = resolver;
		this.operations = new ArrayList<ContentProviderOperation>();
	}

	public int size() {
		return this.operations.size();
	}

	public void add(ContentProviderOperation op) {
		this.operations.add(op);
	}

	/**
	 * Execute all operations against the content resolver
	 * */
	public Uri execute() {
		Uri result = null;

		if (this.operations.size() == 0) {
			return result;
		}


		try {
			ContentProviderResult[] results = this.resolver.applyBatch(ContactsContract.AUTHORITY,
				this.operations);
			if ((results != null) && (results.length > 0)) {
				result = results[0].uri;
			}
		} catch (final OperationApplicationException oaEx) {
			Log.e(TAG, "storing athlete data failed", oaEx);
		} catch (final RemoteException rEx) {
			Log.e(TAG, "storing athlete data failed", rEx);
		}
		this.operations.clear();
		return result;
	}
}
