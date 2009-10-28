package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

public class Avatar extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.avatar);
        
        String id = getIntent().getStringExtra("id");
        
        ImageView avatar = (ImageView) this.findViewById(R.id.big_avatar);
        BitmapDrawable drawable = LegislatorProfile.getImage(LegislatorProfile.PIC_LARGE, id, this);
        avatar.setImageDrawable(drawable);
        
        avatar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Avatar.this.finish();
			}
		});
    }
	
}