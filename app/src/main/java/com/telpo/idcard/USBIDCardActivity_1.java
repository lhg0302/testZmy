package com.telpo.idcard;

import java.util.HashMap;
import java.util.Iterator;

import com.common.pos.api.util.posutil.PosUtil;
import com.example.idcardchecktest.R;
import com.telpo.idcheck.ReaderUtils;
import com.telpo.idcheck.UsbIdCard;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.telpo.tps550.api.util.ShellUtils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class USBIDCardActivity_1 extends Activity implements OnClickListener {

	Button btn_setPower, btn_setPowerOff, btn_aquireUSBpermission, btn_read_idcard, btn_read_idcard_loop,
			btn_stop_read_loop;
	TextView tv_info;
	ImageView img_head_picture;

	private SoundPool soundPool;
	private static long play_time = 0;

	private boolean isUsingUsb = false;
	private boolean hasReader = false;
	private byte[] image;
	private byte[] fringerprint;
	private String fringerprintData;

	private IdentityInfo info;
	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private UsbManager mUsbManager;
	private UsbDevice idcard_reader;
	private UsbIdCard reader;
	long startTime, finishTime;
	Thread mThread;
	public static boolean findloop = true;
	private GetIDInfoTask mAsyncTask;

	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				tv_info.setText(getString(R.string.idcard_xm) + info.getName() + "\n" + getString(R.string.idcard_xb)
						+ info.getSex() + "\n" + getString(R.string.idcard_mz) + info.getNation() + "\n"
						+ getString(R.string.idcard_csrq) + info.getBorn() + "\n" + getString(R.string.idcard_dz)
						+ info.getAddress() + "\n" + getString(R.string.idcard_sfhm) + info.getNo() + "\n"
						+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n" + getString(R.string.idcard_yxqx)
						+ info.getPeriod() + "\n" + getString(R.string.idcard_zwxx) + fringerprintData + "\n" + "读卡时间："
						+ (finishTime - startTime));
				try {
					Bitmap bmp = UsbIdCard.decodeIdCardImage(image);
					img_head_picture.setImageBitmap(bmp);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				playSound(1);
				// findloop = true;
				break;
			case 1:
				tv_info.setText("读卡失败！");
				// playSound(5);
				break;
			default:
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_idcard);
		initData();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initView();
		if (mThread != null && mThread.isAlive()) {
			btn_read_idcard_loop.setEnabled(false);
			btn_read_idcard.setEnabled(false);
			btn_stop_read_loop.setEnabled(true);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// setPowerOff();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		PosUtil.setIdCardPower(PosUtil.IDCARD_POWER_OFF);// 身份证上电
	}

	public void initView() {
		btn_setPower = (Button) findViewById(R.id.btn_setPower);
		btn_setPowerOff = (Button) findViewById(R.id.btn_setPowerOff);
		btn_aquireUSBpermission = (Button) findViewById(R.id.btn_aquireUSBpermission);
		btn_read_idcard = (Button) findViewById(R.id.btn_read_idcard);
		btn_read_idcard_loop = (Button) findViewById(R.id.btn_read_idcard_loop);
		btn_stop_read_loop = (Button) findViewById(R.id.btn_stop_read_idcard_loop);
		tv_info = (TextView) findViewById(R.id.tv_idcard_info);
		img_head_picture = (ImageView) findViewById(R.id.img_head_picture);
		btn_setPower.setEnabled(true);
		btn_aquireUSBpermission.setEnabled(false);
		btn_read_idcard.setEnabled(false);
		btn_read_idcard_loop.setEnabled(false);
		btn_stop_read_loop.setEnabled(false);
		btn_setPowerOff.setEnabled(false);
	}

	public void initData() {
		mPermissionIntent = PendingIntent.getBroadcast(USBIDCardActivity_1.this, 0, new Intent(ACTION_USB_PERMISSION),
				0);
		soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
		soundPool.load(this, R.raw.read_card, 1);
		soundPool.load(this, R.raw.verify_fail, 2);
		soundPool.load(this, R.raw.verify_success, 3);
		soundPool.load(this, R.raw.please_verify, 4);
		soundPool.load(this, R.raw.read_fail, 5);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_setPower:
			setPowerOn();
			break;
		case R.id.btn_aquireUSBpermission:
			openUsbTACard();
			break;
		case R.id.btn_read_idcard:
			readIdCard();
			// mAsyncTask = new GetIDInfoTask();
			// mAsyncTask.execute();
			break;
		case R.id.btn_read_idcard_loop:
			// Log.e("btn_read_idcard_loop---------->", "循环读取身份证");
			findloop = true;

			mThread = new Thread() {
				@Override
				public void run() {
					while (findloop) {
						Log.e("btn_read_idcard_loop---------->", "循环读取身份证");
						readIdCard();
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
			};
			mThread.start();
			btn_read_idcard_loop.setEnabled(false);
			btn_read_idcard.setEnabled(false);
			btn_stop_read_loop.setEnabled(true);
			break;
		case R.id.btn_stop_read_idcard_loop:
			findloop = false;
			btn_read_idcard_loop.setEnabled(true);
			btn_read_idcard.setEnabled(true);
			btn_stop_read_loop.setEnabled(false);
			break;
		case R.id.btn_setPowerOff:
			setPowerOff();
			break;

		default:
			break;
		}
	}

	/**
	 * 上电
	 */
	public void setPowerOn() {
		PosUtil.setIdCardPower(PosUtil.IDCARD_POWER_ON);// 身份证上电
		ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", true);
		btn_setPower.setEnabled(false);
		btn_aquireUSBpermission.setEnabled(true);
		btn_setPowerOff.setEnabled(true);
	}

	/**
	 * 下电
	 */
	public void setPowerOff() {
		PosUtil.setIdCardPower(PosUtil.IDCARD_POWER_OFF);// 身份证上电
		ShellUtils.execCommand("echo 4 > /sys/class/telpoio/power_status", false);
		btn_setPower.setEnabled(true);
		btn_aquireUSBpermission.setEnabled(false);
		btn_read_idcard.setEnabled(false);
		btn_read_idcard_loop.setEnabled(false);
		btn_stop_read_loop.setEnabled(false);
		btn_setPowerOff.setEnabled(false);
		tv_info.setText("");
		img_head_picture.setImageBitmap(null);
	}

	/**
	 * 获取USB授权并初始化身份证阅读器
	 */
	private void openUsbTACard() {
		isUsingUsb = true;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> tdeviceHashMap = mUsbManager.getDeviceList();
		Iterator<UsbDevice> iterator = tdeviceHashMap.values().iterator();
		hasReader = false;

		while (iterator.hasNext()) {
			UsbDevice usbDevice = iterator.next();
			int pid = usbDevice.getProductId();
			int vid = usbDevice.getVendorId();
			if (pid == 0xc35a && vid == 0x0400) {
				hasReader = true;
				idcard_reader = usbDevice;

				if (mUsbManager.hasPermission(usbDevice)) {
					try {
						reader = new UsbIdCard(mUsbManager, idcard_reader);
						btn_aquireUSBpermission.setEnabled(false);
						btn_read_idcard.setEnabled(true);
						btn_read_idcard_loop.setEnabled(true);
						tv_info.setText("USB授权成功");
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				} else {
					mUsbManager.requestPermission(usbDevice, mPermissionIntent);// ***弹出对话框问是否允许的时�?
					try {
						reader = new UsbIdCard(mUsbManager, usbDevice);
						btn_aquireUSBpermission.setEnabled(false);
						btn_read_idcard.setEnabled(true);
						btn_read_idcard_loop.setEnabled(true);
						tv_info.setText("USB授权成功");
					} catch (TelpoException e) {
						// TODO Auto-generated catch block
						btn_read_idcard.setEnabled(false);
						btn_read_idcard_loop.setEnabled(false);
						tv_info.setText("USB授权失败");
						img_head_picture.setImageBitmap(null);
						e.printStackTrace();
					}
				}
			}
		}
		isUsingUsb = false;
	}

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

	/**
	 * 读取身份证信息
	 */
	public void readIdCard() {
		try {
			startTime = System.currentTimeMillis();
			Log.e("checkIdCard---------->", "checkIdCard");
			reader = new UsbIdCard(mUsbManager, idcard_reader);
			byte[] ret = reader.findIdCard();
			if (ret != null) {
				ret = reader.selectIdCard();
				if (ret != null) {
					info = reader.readIdCard();
				}
			}

			// info = reader.checkIdCard();
			finishTime = System.currentTimeMillis();
			if (info != null) {
				if ("".equals(info.getName())) {
					return;
				}
				image = UsbIdCard.getIdCardImage(info);
				fringerprint = UsbIdCard.getFringerPrint(info);
				fringerprintData = ReaderUtils.get_finger_info(USBIDCardActivity_1.this, fringerprint);
				mHandler.sendEmptyMessage(0);
			} else {
				Log.i("checkIdCard fail---------->", "info = null");
				mHandler.sendEmptyMessage(1);
			}

		} catch (TelpoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mHandler.sendEmptyMessage(1);

		}
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
					fringerprintData = ReaderUtils.get_finger_info(USBIDCardActivity_1.this, fringerprint);
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
				Log.i("info-------->", "" + info.getName());
				Log.i("info-------->", "" + info.getSex());
				Log.i("info-------->", "" + info.getBorn());
				Log.i("info-------->", "" + info.getAddress());
				Log.i("info-------->", "" + info.getPeriod());
				Log.i("success-------->", "success");
				tv_info.setText(getString(R.string.idcard_xm) + info.getName() + "\n" + getString(R.string.idcard_xb)
						+ info.getSex() + "\n" + getString(R.string.idcard_mz) + info.getNation() + "\n"
						+ getString(R.string.idcard_csrq) + info.getBorn() + "\n" + getString(R.string.idcard_dz)
						+ info.getAddress() + "\n" + getString(R.string.idcard_sfhm) + info.getNo() + "\n"
						+ getString(R.string.idcard_qzjg) + info.getApartment() + "\n" + getString(R.string.idcard_yxqx)
						+ info.getPeriod() + "\n" + getString(R.string.idcard_zwxx) + fringerprintData);
				try {
					Bitmap bmp = UsbIdCard.decodeIdCardImage(image);
					img_head_picture.setImageBitmap(bmp);
				} catch (TelpoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				playSound(1);
			} else {
				Log.e("fail-------->", "fail");
				tv_info.setText("读卡失败！");
				playSound(5);
			}
		}
	}

}
