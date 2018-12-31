package com.telpo.idcard;

import java.util.HashMap;
import java.util.Iterator;

import com.common.pos.api.util.posutil.PosUtil;
import com.example.idcardchecktest.R;
import com.telpo.idcheck.ReaderUtils;
import com.telpo.idcheck.UsbIdCard;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.IdentityInfo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class USBIDCardActivity_new extends Activity {
	private SoundPool soundPool;
	private Button getPermission, btn_read_once;
	private TextView idcardInfo, hint_view, def_view, test_info_view;
	private ImageView imageView1, imageView2;
	private View info_view;
	private CountryMap countryMap = CountryMap.getInstance();
	private static long play_time = 0;
	private byte[] image;
	private byte[] fringerprint;
	private String fringerprintData;
	private UsbManager mUsbManager;
	private UsbDevice idcard_reader;
	private UsbIdCard reader;
	private boolean isUsingUsb = false;
	private boolean hasReader = false;
	private boolean isFinish = false;
	private GetIDInfoTask mAsyncTask;
	private IdentityInfo info;
	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private Handler handler1 = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				mAsyncTask = new GetIDInfoTask();
				mAsyncTask.execute();
				break;
			case 2:
				String text = hint_view.getText().toString();
				int index = text.indexOf("\n");
				if (index != -1) {
					text = text.substring(0, index);
				}
				hint_view.setText(text + "\n" + getString(R.string.idcard_djs) + msg.arg1);
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.idcard_main_new);
		PosUtil.setIdCardPower(PosUtil.IDCARD_POWER_ON);// 身份证上电
		idcardInfo = (TextView) findViewById(R.id.showData);
		imageView1 = (ImageView) findViewById(R.id.imageView1);
		imageView2 = (ImageView) findViewById(R.id.imageView2);
		info_view = findViewById(R.id.relativeLayout);
		hint_view = (TextView) findViewById(R.id.hint_view);
		def_view = (TextView) findViewById(R.id.default_view);
		test_info_view = (TextView) findViewById(R.id.test_info_view);

		soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		soundPool.load(this, R.raw.read_card, 1);
		soundPool.load(this, R.raw.verify_fail, 2);
		soundPool.load(this, R.raw.verify_success, 3);
		soundPool.load(this, R.raw.please_verify, 4);
		soundPool.load(this, R.raw.read_fail, 5);

		getPermission = (Button) findViewById(R.id.requestPermission);
		getPermission.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				openUsbIdCard();
			}
		});

		btn_read_once = (Button) findViewById(R.id.read_once);
		btn_read_once.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Toast.makeText(USBIDCardActivity_new.this, "点击",
				// Toast.LENGTH_SHORT).show();
				handler1.sendMessage(handler1.obtainMessage(1, ""));
			}
		});

		mPermissionIntent = PendingIntent.getBroadcast(USBIDCardActivity_new.this, 0, new Intent(ACTION_USB_PERMISSION),
				0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		// beepManager = new BeepManager(this, R.raw.read_card_success);
		ReaderUtils.idcard_poweron();
		getPermission.setVisibility(View.VISIBLE);
		def_view.setText(getString(R.string.idcard_qhqqx));
		openUsbIdCard();
		hint_view.setText("");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (handler1.hasMessages(1)) {
			handler1.removeMessages(1);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
		PosUtil.setIdCardPower(PosUtil.IDCARD_POWER_OFF);// 身份证上电
	}

	private class GetIDInfoTask extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			info = null;
			fringerprintData = "";
			fringerprint = null;
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {

			try {
				info = reader.checkIdCard();
				if (info != null) {
					if ("".equals(info.getName())) {
						return false;
					}
					image = UsbIdCard.getIdCardImage(info);
					fringerprint = UsbIdCard.getFringerPrint(info);
					fringerprintData = ReaderUtils.get_finger_info(USBIDCardActivity_new.this, fringerprint);
					return true;
				} else {
					Log.i("checkIdCard fail---------->", "info = null");
				}

			} catch (TelpoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				imageView2.setVisibility(View.INVISIBLE);
				Log.i("info-------->", "" + info.getName());
				Log.i("info-------->", "" + info.getName());
				Log.i("info-------->", "" + info.getName());
				Log.i("info-------->", "" + info.getName());
				Log.i("info-------->", "" + info.getName());
				Log.i("success-------->", "success");
				test_info_view.setText(getString(R.string.idcard_xm) + info.getName() + "\n"
						+ getString(R.string.idcard_xb) + info.getSex() + "\n" + getString(R.string.idcard_mz)
						+ info.getNation() + "\n" + getString(R.string.idcard_csrq) + info.getBorn() + "\n"
						+ getString(R.string.idcard_dz) + info.getAddress() + "\n" + getString(R.string.idcard_sfhm)
						+ info.getNo() + "\n" + getString(R.string.idcard_qzjg) + info.getApartment() + "\n"
						+ getString(R.string.idcard_yxqx) + info.getPeriod() + "\n" + getString(R.string.idcard_zwxx)
						+ fringerprintData);
				try {
					imageView1.setImageBitmap(UsbIdCard.decodeIdCardImage(image));
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				showInfoView();
				playSound(1);
			} else {
				Log.e("fail-------->", "fail");
				playSound(5);
			}
		}
	}

	private void showDefaultView(String content) {
		info_view.setVisibility(View.GONE);
		imageView1.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		idcardInfo.setText("");
		def_view.setVisibility(View.VISIBLE);
		def_view.setText(content);
	}

	private void showInfoView() {
		info_view.setVisibility(View.VISIBLE);
		def_view.setVisibility(View.GONE);
		def_view.setText("");
	}

	public void openUsbIdCard() {
		isUsingUsb = true;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
		Iterator<UsbDevice> iterator = deviceHashMap.values().iterator();
		hasReader = false;

		while (iterator.hasNext()) {
			UsbDevice usbDevice = iterator.next();
			int pid = usbDevice.getProductId();
			int vid = usbDevice.getVendorId();

			if (pid == UsbIdCard.READER_PID && vid == UsbIdCard.READER_VID) {
				hasReader = true;
				idcard_reader = usbDevice;
				if (mUsbManager.hasPermission(usbDevice)) {
					// do your work
					getPermission.setVisibility(View.INVISIBLE);
					def_view.setText(getString(R.string.idcard_hqqxcg));
					try {
						reader = new UsbIdCard(mUsbManager, usbDevice);
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				} else {
					getPermission.setVisibility(View.VISIBLE);
					def_view.setText(getString(R.string.idcard_qhqqx));
					mUsbManager.requestPermission(usbDevice, mPermissionIntent);
				}
			}

		}
		isUsingUsb = false;
		if (!hasReader) {
			hint_view.setText(getString(R.string.idcard_wfljdkq));
			if (handler1.hasMessages(1)) {
				handler1.removeMessages(1);
			}
		}
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (idcard_reader != null) {
							getPermission.setVisibility(View.INVISIBLE);
							def_view.setText(getString(R.string.idcard_hqqxcg));
							hint_view.setText(getString(R.string.idcard_qfk));
							btn_read_once.setEnabled(true);
							try {
								reader = new UsbIdCard(mUsbManager, idcard_reader);
							} catch (TelpoException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						if (handler1.hasMessages(1)) {
							handler1.removeMessages(1);
						}
						idcard_reader = null;
						getPermission.setVisibility(View.VISIBLE);
						def_view.setText(getString(R.string.idcard_qhqqx));
						if (handler1.hasMessages(1)) {
							handler1.removeMessages(1);
						}
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				// openUsbIdCard();
				// fingercamera.isworking = false;
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// openUsbIdCard();
			}
		}
	};

	void playSound(int src) {
		if (src == 4) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			soundPool.play(4, 1, 1, 0, 0, 1);
			return;
		}
		if (System.currentTimeMillis() - play_time >= 2 * 1000) {
			play_time = System.currentTimeMillis();
			soundPool.play(src, 1, 1, 0, 0, 1);
		}
	}
}
