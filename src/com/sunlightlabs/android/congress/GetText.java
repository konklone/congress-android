package com.sunlightlabs.android.congress;

import com.sunlightlabs.android.congress.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GetText extends Activity {
	private EditText responseField;
	
	private String ask, hint, startValue;
	private int inputType;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.get_text);
        
        Bundle extras = getIntent().getExtras();
        ask = extras.getString("ask");
        hint = extras.getString("hint");
        inputType = extras.getInt("inputType", InputType.TYPE_CLASS_TEXT);
        startValue = extras.getString("startValue");
        
        setupControls();
    }
	
	public void setupControls() {
		if (ask != null) {
			TextView askView = (TextView) findViewById(R.id.get_text_ask);
			askView.setText(ask);
		}
		
		responseField = (EditText) findViewById(R.id.get_text_response);
		if (hint != null)
			responseField.setHint(hint);
		if (startValue != null)
			responseField.setText(startValue);
		
		responseField.setInputType(inputType);
		
		
		Button ok = (Button) findViewById(R.id.get_text_ok);
		Button cancel = (Button) findViewById(R.id.get_text_cancel);
		
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
