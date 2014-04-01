package org.stagex.danmaku.activity;

import org.keke.player.R;
import org.stagex.danmaku.util.SourceName;
import org.stagex.danmaku.util.SystemUtility;

import br.com.dina.ui.widget.UITableView;
import br.com.dina.ui.widget.UITableView.ClickListener;
import cn.waps.AppConnect;
import cn.waps.UpdatePointsNotifier;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SetupActivity extends Activity implements UpdatePointsNotifier {
	/** Called when the activity is first created. */
	private static final String LOGTAG = "SetupActivity";

	/* 顶部标题栏的控件 */
	// private ImageView button_home;
	private TextView button_back;

	/* 记录硬解码与软解码的状态 */
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private boolean isHardDec;
	private boolean isSeekbarShow;
	private boolean noAd;

	// private TextView pointsTextView;
	private String displayPointsText;
	private String currencyName = "积分";
	final Handler mHandler = new Handler();

	UITableView tableView1;
	UITableView tableView2;
	UITableView tableView3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup2);

		/* 顶部标题栏的控件 */
		button_back = (TextView) findViewById(R.id.back_btn);

		// =====================================================
		tableView1 = (UITableView) findViewById(R.id.tableView1);
		createList1();
		Log.d(LOGTAG, "total items: " + tableView1.getCount());
		tableView1.commit();

		tableView2 = (UITableView) findViewById(R.id.tableView2);
		createList2();
		Log.d(LOGTAG, "total items: " + tableView2.getCount());
		tableView2.commit();

		tableView3 = (UITableView) findViewById(R.id.tableView3);
		createList3();
		Log.d(LOGTAG, "total items: " + tableView3.getCount());
		tableView3.commit();
		// =====================================================

		/* 判断解码器状态 */
		sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
		editor = sharedPreferences.edit();
		isHardDec = sharedPreferences.getBoolean("isHardDec", false);
		if (isHardDec) {
			// int resource =
			// SystemUtility.getDrawableId("mini_operate_selected");
			// button_codec.setImageResource(resource);
			Log.d(LOGTAG, "检测到为硬解码模式");
		} else {
			// int resource = SystemUtility
			// .getDrawableId("mini_operate_unselected");
			// button_codec.setImageResource(resource);
			Log.d(LOGTAG, "检测到为软解码模式");
		}

		// ========================================================
		// 2013-08-06
		// 由于新版1.3.1之后加入了积分要求在线配置的功能，所以可能会有调节功能
		// 如某段时间搞活动，要求的积分较少，过一段时间，可能要上调
		// 主要根据收益情况调整（这部分工作放到HomeActivity中去做）
		// ========================================================

		/* 检测是否需要显示广告 */
		// sharedPreferences = getSharedPreferences("keke_player",
		// MODE_PRIVATE);
		// editor = sharedPreferences.edit();
		noAd = sharedPreferences.getBoolean("noAd", false);
		if (noAd) {
			// int resource =
			// SystemUtility.getDrawableId("mini_operate_selected");
			// button_ad.setImageResource(resource);
			Log.d(LOGTAG, "检测到无广告模式");
		} else {
			// int resource = SystemUtility
			// .getDrawableId("mini_operate_unselected");
			// button_ad.setImageResource(resource);
			Log.d(LOGTAG, "检测到有广告模式");
		}

		/* 设置监听 */
		setListensers();
	}

	// Listen for button clicks
	private void setListensers() {
		button_back.setOnClickListener(goListener);
	}

	// 按键监听
	private Button.OnClickListener goListener = new Button.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back_btn:
				// 回到上一个界面(Activity)
				finish();
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};

	/**
	 * 程序关于界面
	 */
	private void startAboutMedia() {
		Intent intent = new Intent(SetupActivity.this, MessageActivity.class);
		intent.putExtra("msgPath", "about.html");
		intent.putExtra("msgName", "关于");
		startActivity(intent);
	}

	/**
	 * 程序帮助界面
	 */
	private void startHelpMedia() {
		Intent intent = new Intent(SetupActivity.this, MessageActivity.class);
		intent.putExtra("msgPath", "help.html");
		intent.putExtra("msgName", "使用帮助");
		startActivity(intent);
	}

	@Override
	protected void onResume() {
		// 从服务器端获取当前用户的虚拟货币.
		// 返回结果在回调函数getUpdatePoints(...)中处理
		AppConnect.getInstance(this).getPoints(this);
		super.onResume();
	}

	// 创建一个线程
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			// if (pointsTextView != null) {
			// pointsTextView.setText("(" + "当前" + displayPointsText + ")");
			// }
		}
	};

	/**
	 * AppConnect.getPoints()方法的实现，必须实现
	 * 
	 * @param currencyName
	 *            虚拟货币名称.
	 * @param pointTotal
	 *            虚拟货币余额.
	 */
	public void getUpdatePoints(String currencyName, int pointTotal) {
		this.currencyName = currencyName;
		displayPointsText = currencyName + ": " + pointTotal;
		// 保存积分值
		editor.putInt("pointTotal", pointTotal);
		editor.commit();
		mHandler.post(mUpdateResults);
	}

	/**
	 * AppConnect.getPoints() 方法的实现，必须实现
	 * 
	 * @param error
	 *            请求失败的错误信息
	 */
	public void getUpdatePointsFailed(String error) {
		displayPointsText = error;
		mHandler.post(mUpdateResults);
	}

	// ===========================================================================
	/**
	 * 采用圆角布局的ListView
	 */
	private void createList1() {
		CustomClickListener1 listener = new CustomClickListener1();
		tableView1.setClickListener(listener);

		tableView1.addBasicItem(R.drawable.ic_about, "关于(v1.4.2.4)", "软件信息介绍");
		tableView1.addBasicItem(R.drawable.ic_about, "帮助", "软件帮助信息");
		tableView1
				.addBasicItem(R.drawable.ic_star, "QQ群", "可可电视交流群火爆招募中");
		tableView1.addBasicItem(R.drawable.ic_feedback, "信息反馈", "反馈您的建议和意见");
		tableView1.addBasicItem(R.drawable.ic_check_update, "检查更新", "检查软件最新版本");
	}

	/**
	 * 设置监听事件
	 * 
	 * @author jgf
	 * 
	 */
	private class CustomClickListener1 implements ClickListener {
		@Override
		public void onClick(int index) {

			switch (index) {
			case 0:
				startAboutMedia();
				break;
			case 1:
				startHelpMedia();
				break;
			case 2:
				// 显示QQ交流群信息
				new AlertDialog.Builder(SetupActivity.this)
						.setIcon(R.drawable.ic_about)
						.setTitle("可可电视交流群")
						.setMessage(
								"1群：336809417（500即将爆满）\n2群：278046078（新开）\n3群：115735045（新开）\n\n欢迎加入交流群，可以分享更多的自定义地址，反馈您的建议和意见\n\n可可电视官网论坛：\nhttp://www.kklive8.com\n欢迎您的光临")
						.setNegativeButton("关闭",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
				break;
			case 3:
				AppConnect.getInstance(SetupActivity.this).showFeedback();
				break;
			case 4:
				AppConnect.getInstance(SetupActivity.this).checkUpdate(
						SetupActivity.this);
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}

		}
	}

	/**
	 * 采用圆角布局的ListView
	 */
	private void createList2() {
		CustomClickListener2 listener = new CustomClickListener2();
		tableView2.setClickListener(listener);

		tableView2.addBasicItem(R.drawable.ic_home, "帮助可可", "可可的发展需要您的支持");
		tableView2.addBasicItem(R.drawable.ic_app, "应用商店", "当前热门软件和游戏");
		tableView2.addBasicItem(R.drawable.ic_star, "亲，给个好评呗", "到小米、安卓等市场给个好评");
		tableView2.addBasicItem(R.drawable.ic_tuangou, "大众团购", "大众点评团购入口");
	}

	/**
	 * 设置监听事件
	 * 
	 * @author jgf
	 * 
	 */
	private class CustomClickListener2 implements ClickListener {
		@Override
		public void onClick(int index) {

			switch (index) {
			case 0:
				// 改为支持可可的控件入口
				Intent intent = new Intent(SetupActivity.this, SupportKK.class);
				startActivity(intent);
				break;
			case 1:
				AppConnect.getInstance(SetupActivity.this).showOffers(
						SetupActivity.this);
				break;
			case 2:
//				new AlertDialog.Builder(SetupActivity.this)
//				.setIcon(R.drawable.ic_about)
//				.setTitle("亲，给个好评")
//				.setMessage("请帮助可可到您所在的应用市场给个好评，让更多的用户知道可可电视，谢谢您的支持！")
//				.setNegativeButton("关闭",
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//									int which) {
//							}
//						}).show();
				
				String str = "market://details?id=org.keke.player";
			    Intent localIntent = new Intent("android.intent.action.VIEW");
			    localIntent.setData(Uri.parse(str));
			    startActivity(localIntent);

				break;
			case 3:
				AppConnect.getInstance(SetupActivity.this).showTuanOffers(
						SetupActivity.this);
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	}

	/**
	 * 采用圆角布局的ListView
	 */
	private void createList3() {
		CustomClickListener3 listener = new CustomClickListener3();
		tableView3.setClickListener(listener);

		tableView3.addBasicItem(R.drawable.ic_decode, "解码模式", "选择软解码或者硬解码");
		// TODO 2013-09-06 暂时去掉秒杀广告的控件
		// tableView3.addBasicItem(R.drawable.ic_noad, "秒杀广告", "达到积分要求可以去除广告");
		tableView3.addBasicItem(R.drawable.ic_star, "画面比例", "选择视频播放界面的默认比列");
		tableView3.addBasicItem(R.drawable.ic_star, "播放进度条", "选择是否显示播放进度条");
	}

	/**
	 * 设置监听事件
	 * 
	 * @author jgf
	 * 
	 */
	private class CustomClickListener3 implements ClickListener {
		@Override
		public void onClick(int index) {

			switch (index) {
			case 0:

//				isHardDec = sharedPreferences.getBoolean("isHardDec", false);
//				if (isHardDec) {
//
//					new AlertDialog.Builder(SetupActivity.this)
//							.setIcon(R.drawable.ic_dialog_alert)
//							.setTitle("当前为硬解")
//							.setMessage("确定切换？")
//							.setPositiveButton("确定",
//									new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(
//												DialogInterface dialog,
//												int which) {
//											editor.putBoolean("isHardDec",
//													false);
//											editor.commit();
//											Log.d(LOGTAG, "设置为软解码模式");
//										}
//
//									})
//							.setNegativeButton("取消",
//									new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(
//												DialogInterface dialog,
//												int which) {
//											dialog.cancel();
//										}
//									}).show();
//
//				} else {
//
//					new AlertDialog.Builder(SetupActivity.this)
//							.setIcon(R.drawable.ic_dialog_alert)
//							.setTitle("当前为软解")
//							.setMessage("确定切换？")
//							.setPositiveButton("确定",
//									new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(
//												DialogInterface dialog,
//												int which) {
//											editor.putBoolean("isHardDec", true);
//											editor.commit();
//											Log.d(LOGTAG, "设置为硬解码模式");
//										}
//									})
//							.setNegativeButton("取消",
//									new DialogInterface.OnClickListener() {
//										@Override
//										public void onClick(
//												DialogInterface dialog,
//												int which) {
//											dialog.cancel();
//										}
//									}).show();
//				}
				// 显示对话框
				currentDialog = DECODE_DIALOG;
				showDialog(DECODE_DIALOG);
				
				break;
			// case 1:
			//
			// noAd = sharedPreferences.getBoolean("noAd", false);
			// if (noAd) {
			// new AlertDialog.Builder(SetupActivity.this)
			// .setIcon(R.drawable.ic_dialog_alert)
			// .setTitle("温馨提示")
			// .setMessage(
			// "广告已被秒杀\n" + "（当前积分" + sharedPreferences.getInt("pointTotal", 0)
			// + "）")
			// .setPositiveButton("打开广告",
			// new DialogInterface.OnClickListener() {
			// public void onClick(
			// DialogInterface dialog,
			// int whichButton) {
			// editor.putBoolean("noAd", false);
			// editor.commit();
			// Log.d(LOGTAG, "设置为有广告模式");
			// }
			// }).show();
			// } else {
			// // 在线获取需要的积分参数，以便随时可以控制积分值
			// String noAdPoint = AppConnect.getInstance(
			// SetupActivity.this).getConfig("noAdPoint", "88888");
			// if (noAdPoint.equals("88888")) {
			// // 如果因为首次运行网络原因，获取到的是88888，说明需要提醒用户联网操作
			// new AlertDialog.Builder(SetupActivity.this)
			// .setIcon(R.drawable.ic_dialog_alert)
			// .setTitle("温馨提示")
			// .setMessage(
			// "亲，该操作需要联网操作哦！\n同时，该操作需要打开网络后重新启动一次！")
			// .setPositiveButton("知道了",
			// new DialogInterface.OnClickListener() {
			// public void onClick(
			// DialogInterface dialog,
			// int whichButton) {
			// }
			// }).show();
			// break;
			// }
			// if (sharedPreferences.getInt("pointTotal", 0) < Integer
			// .parseInt(noAdPoint)) {
			// // 改为从万普的在线参数里获取这个积分值
			// new AlertDialog.Builder(SetupActivity.this)
			// .setIcon(R.drawable.ic_dialog_alert)
			// .setTitle("温馨提示")
			// .setMessage(
			// "您的积分不足"
			// + noAdPoint
			// + "分，暂时无法去除广告！\n您可以打开应用推荐赚取相应的积分，感谢您的支持！")
			// .setPositiveButton("赚积分",
			// new DialogInterface.OnClickListener() {
			// @Override
			// public void onClick(
			// DialogInterface dialog,
			// int which) {
			// AppConnect
			// .getInstance(
			// SetupActivity.this)
			// .showOffers(
			// SetupActivity.this);
			// }
			// })
			// .setNegativeButton("取消",
			// new DialogInterface.OnClickListener() {
			// @Override
			// public void onClick(
			// DialogInterface dialog,
			// int which) {
			// dialog.cancel();
			// }
			// }).show();
			//
			// } else {
			// new AlertDialog.Builder(SetupActivity.this)
			// .setIcon(R.drawable.ic_dialog_alert)
			// .setTitle("温馨提示")
			// .setMessage(
			// "您可以秒杀广告了\n" + "（当前积分" + sharedPreferences.getInt("pointTotal",
			// 0) + "）")
			// .setPositiveButton("去除广告",
			// new DialogInterface.OnClickListener() {
			// public void onClick(
			// DialogInterface dialog,
			// int whichButton) {
			// editor.putBoolean("noAd", true);
			// editor.commit();
			// Log.d(LOGTAG, "设置为无广告模式");
			// }
			// }).show();
			// }
			// }
			// break;
			case 1:
				// 显示对话框
				currentDialog = RATION_DIALOG;
				showDialog(RATION_DIALOG);

				break;
			case 2:
				// 显示对话框
				currentDialog = SEEKBAR_DIALOG;
				showDialog(SEEKBAR_DIALOG);

				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	}

	// ===========================================================================

	private final static int RATION_DIALOG = 1;
	private final static int DECODE_DIALOG = 2;
	private final static int SEEKBAR_DIALOG = 3;
	private int currentDialog = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case RATION_DIALOG:
			Builder builder = new AlertDialog.Builder(this);
			// builder.setIcon(R.drawable.basketball);
			builder.setTitle("默认画面比列");
			final ChoiceOnClickListener choiceListener = new ChoiceOnClickListener();

			int viewRation = sharedPreferences.getInt("viewTaion", 0);

			builder.setSingleChoiceItems(R.array.ratio, viewRation,
					choiceListener);

			dialog = builder.create();
			break;
		case DECODE_DIALOG:
			Builder builderDecode = new AlertDialog.Builder(this);
			// builder.setIcon(R.drawable.basketball);
			builderDecode.setTitle("默认解码模式");
			final ChoiceOnClickListener choiceListenerDecode = new ChoiceOnClickListener();

			isHardDec = sharedPreferences.getBoolean("isHardDec", false);

			builderDecode.setSingleChoiceItems(R.array.decode, isHardDec ? 1 : 0,
					choiceListenerDecode);

			dialog = builderDecode.create();
			break;
		case SEEKBAR_DIALOG:
			Builder builderSeekbar = new AlertDialog.Builder(this);
			// builder.setIcon(R.drawable.basketball);
			builderSeekbar.setTitle("是否要显示播放进度条");
			final ChoiceOnClickListener choiceListenerSeekbar = new ChoiceOnClickListener();

			isSeekbarShow = sharedPreferences.getBoolean("isSeekbarShow", false);

			builderSeekbar.setSingleChoiceItems(R.array.seekbar, isSeekbarShow ? 1 : 0,
					choiceListenerSeekbar);

			dialog = builderSeekbar.create();
			break;
		}
		return dialog;
	}

	private class ChoiceOnClickListener implements
			DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialogInterface, int which) {
			switch (currentDialog) {
			case RATION_DIALOG:
				editor.putInt("viewTaion", which);
				editor.commit();
				break;
			case DECODE_DIALOG:
				editor.putBoolean("isHardDec", (which == 1));
				editor.commit();
				break;
			case SEEKBAR_DIALOG:
				editor.putBoolean("isSeekbarShow", (which == 1));
				editor.commit();
				break;
		}
		}
	}
}