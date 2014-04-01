package org.stagex.danmaku.activity;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.util.AppWall;

import com.nmbb.oplayer.scanner.DbHelper;
import com.nmbb.oplayer.scanner.POUserDefChannel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class UserDefActivity extends Activity {
	/** Called when the activity is first created. */
	private static final String LOGTAG = "UserDefActivity";
	private EditText mTextUri = null;
	private EditText mTextName = null;
	private Button mButtonPlay = null;

	/* 顶部标题栏的控件 */
	private TextView button_back;
	/* 文本编辑框 */
	private Button button_clear;
	private Button button_save;

	/* 频道收藏的数据库 */
	private DbHelper<POUserDefChannel> mDbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_def);

		/* 顶部标题栏的控件 */
		button_back = (TextView) findViewById(R.id.back_btn);
		/* 文本编辑框 */
		button_clear = (Button) findViewById(R.id.clear_play);
		mTextUri = (EditText) findViewById(R.id.test_uri);
		mTextName = (EditText) findViewById(R.id.test_name);
		mButtonPlay = (Button) findViewById(R.id.test_play);
		button_save = (Button) findViewById(R.id.save_play);

		/* 频道收藏的数据库 */
		mDbHelper = new DbHelper<POUserDefChannel>();

		/* 设置监听 */
		setListensers();
	}

	// Listen for button clicks
	private void setListensers() {
		button_back.setOnClickListener(goListener);
		button_clear.setOnClickListener(goListener);
		mButtonPlay.setOnClickListener(goListener);
		button_save.setOnClickListener(goListener);
	}

	/**
	 * 检查URL的合法性
	 * 
	 * @param str
	 * @return ftp的user@ IP形式的URL- 199.194.52.184 允许IP和DOMAIN（域名） 域名- www. 二级域名
	 *         first level domain- .com or .museum 端口- :80 a slash isn't
	 *         required if there is no file name
	 */
	private static boolean isURL(String str) {
		// 转换为小写
		str = str.toLowerCase();

		String regex = "^((https|http|ftp|rtsp|rtmp|mmsh|mms|mmst)?://)"
				+ "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?"
				+ "(([0-9]{1,3}\\.){3}[0-9]{1,3}" + "|"
				+ "([0-9a-z_!~*'()-]+\\.)*"
				+ "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." + "[a-z]{2,6})"
				+ "(:[0-9]{1,4})?" + "((/?)|"
				+ "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";

		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(str);

		return matcher.matches();
	}

	// 串流地址
	private String uri;

	// 打开网络媒体
	private void startMedia() {
		uri = mTextUri.getText().toString();
		if (uri.length() > 0) {
			// 判断url的合法性
			if (isURL(uri)) {
				Intent intent = new Intent(UserDefActivity.this,
						PlayerActivity.class);
				ArrayList<String> playlist = new ArrayList<String>();
				playlist.add(uri);
				intent.putExtra("selected", 0);
				intent.putExtra("playlist", playlist);
				intent.putExtra("title", uri);
				// 加上自定义标识
				intent.putExtra("isSelfTV", true);
				startActivity(intent);
			} else {
				new AlertDialog.Builder(UserDefActivity.this)
						.setIcon(R.drawable.ic_dialog_alert)
						.setTitle("警告")
						.setMessage("请检查URL的合法性：\n" + uri)
						.setPositiveButton("继续",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// do nothing - it will close on its own
										Intent intent = new Intent(
												UserDefActivity.this,
												PlayerActivity.class);
										ArrayList<String> playlist = new ArrayList<String>();
										playlist.add(uri);
										intent.putExtra("selected", 0);
										intent.putExtra("playlist", playlist);
										intent.putExtra("title", uri);
										// 加上自定义标识
										intent.putExtra("isSelfTV", true);
										startActivity(intent);
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// do nothing - it will close on its own
									}
								}).show();
			}
		}
	}

	// 按键监听
	private Button.OnClickListener goListener = new Button.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back_btn:
				// 回到上一个界面(Activity)
				finish();
				break;
			case R.id.test_play:
				startMedia();
				break;
			case R.id.save_play:
				// 将该地址加入自定义收藏频道
				if ((mTextName.getText().toString().length() > 0)
						&& (mTextUri.getText().toString().length() > 0)) {

					ChannelInfo info = new ChannelInfo(0, mTextName.getText()
							.toString(), null, null, null, mTextUri.getText()
							.toString(), null, null, null);
					POUserDefChannel POinfo = new POUserDefChannel(info, true);
					// TODO 增加加入数据库操作
					POinfo.date = DateFormat.format("MM月dd日",
							System.currentTimeMillis()).toString();
					mDbHelper.create(POinfo);

					// toast
					Toast.makeText(UserDefActivity.this, "已添加到自定义收藏频道",
							Toast.LENGTH_LONG).show();
				} else {
					new AlertDialog.Builder(UserDefActivity.this)
							.setIcon(R.drawable.ic_dialog_alert)
							.setTitle("温馨提示")
							.setMessage("先为该频道起个名字吧？")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											// nothing
										}
									}).show();
				}
				break;
			case R.id.clear_play:
				// 清除文本框内容
				mTextUri.setText("");
				mTextName.setText("");
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};

	// =================================================
	// 加上menu
	private static final int SUPPORT_ID = Menu.FIRST + 1;
	private static final int SETUP_ID = Menu.FIRST + 2;
	private static final int APP_ID = Menu.FIRST + 3;

	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * 第一个参数是groupId，如果不需要可以设置为Menu.NONE
		 * 第二个参数就是item的ID，我们可以通过menu.findItem(id)来获取具体的item
		 * 第三个参数是item的顺序，一般可采用Menu.NONE，具体看本文最后MenuInflater的部分
		 * 第四个参数是显示的内容，可以是String，或者是引用Strings.xml的ID
		 */
		menu.add(Menu.NONE, SUPPORT_ID, Menu.NONE, "帮助可可").setIcon(R.drawable.ic_tuangou_pressed);
		menu.add(Menu.NONE, SETUP_ID, Menu.NONE, "设置").setIcon(R.drawable.ic_setup2);
		menu.add(Menu.NONE, APP_ID, Menu.NONE, "热门应用").setIcon(R.drawable.ic_star);

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) { // 获取Id
		case SUPPORT_ID:
			Intent intent1 = new Intent(UserDefActivity.this, SupportKK.class);
			startActivity(intent1);
			break;
		case SETUP_ID:
			Intent intent2 = new Intent(UserDefActivity.this,
					SetupActivity.class);
			startActivity(intent2);
			break;
		case APP_ID:
			// 获取全部自定义广告数据
			Intent appWallIntent = new Intent(this, AppWall.class);
			this.startActivity(appWallIntent);
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	// =================================================
}
