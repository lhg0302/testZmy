package com.telpo.idcard;

import com.example.idcardchecktest.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	// private FingerPreview fingercamera;
	private Button serial_mode, usb_mode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_acard_main);
		serial_mode = (Button) findViewById(R.id.tserial_mode);
		usb_mode = (Button) findViewById(R.id.tusb_mode);
		serial_mode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// Intent intent = new Intent(MainActivity.this,
				// SerialIdCardActivity.class);
				// startActivity(intent);
			}
		});
		usb_mode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this, USBIDCardActivity_1.class);
				startActivity(intent);
			}
		});

		// Algorithm Config for STQC

	}
}
