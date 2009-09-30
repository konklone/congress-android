package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GetText extends Activity {
	private EditText responseField;
	
	private String ask, hint;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.get_text);
        
        Bundle extras = getIntent().getExtras();
        ask = extras.getString("ask");
        hint = extras.getString("hint");
        
        setupControls();
    }
	
	public void setupControls() {
		if (ask != null) {
			TextView askView = (TextView) this.findViewById(R.id.get_text_ask);
			askView.setText(ask);
		}
		
		responseField = (EditText) this.findViewById(R.id.get_text_response);
		if (hint != null)
			responseField.setHint(hint);
		
		
		Button ok = (Button) this.findViewById(R.id.get_text_ok);
		Button cancel = (Button) this.findViewById(R.id.get_text_cancel);
		
		ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String response = responseField.getText().toString();
				
				Bundle data = new Bundle();
				data.putString("response", response);
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
