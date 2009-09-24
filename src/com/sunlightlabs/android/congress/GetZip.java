package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class GetZip extends Activity {
	private EditText zipCode;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.get_zip);
        
        setupControls();
    }
	
	public void setupControls() {
		zipCode = (EditText) this.findViewById(R.id.zip_code);
		Button ok = (Button) this.findViewById(R.id.ok_zip_code);
		Button cancel = (Button) this.findViewById(R.id.cancel_zip_code);
		
		ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String zip = zipCode.getText().toString();
				
				Bundle data = new Bundle();
				data.putString("zip_code", zip);
				Intent i = new Intent();
				i.putExtras(data);
				
				setResult(RESULT_OK, i);
				finish();
			}
		});
		
		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
}
