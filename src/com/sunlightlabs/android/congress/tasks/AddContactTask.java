package com.sunlightlabs.android.congress.tasks;

import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.android.congress.utils.LegislatorImage;
import com.sunlightlabs.android.congress.utils.ContactManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;

/**
 * A background taks used to add a legislator
 * to the phones contacts
 * */
public class AddContactTask extends AsyncTask<Void, Void, Void> {

    private Context context;

    private Legislator legislator;

    public AddContactTask(Context context, Legislator legislator) {
        super();
        this.context = context;
        this.legislator = legislator;
    }

    protected Void doInBackground(Void... ignored) {
        // download and convert the legislators avator into a byte array
        Drawable d = LegislatorImage.getImage(LegislatorImage.PIC_LARGE, 
            this.legislator.getId(), this.context);
        Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
        byte[] avatar = stream.toByteArray();

        // now that we have that we can add the legislator to the phones
        // contacts
        ContactManager.addLegislatorToContacts(this.context, this.legislator, avatar);

        
        return null;
    }
}
