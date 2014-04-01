package org.stagex.danmaku.activity;

import org.keke.player.R;
import org.stagex.danmaku.util.AppWall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

@SuppressLint("NewApi")
public class MessageActivity extends Activity {
	/** Called when the activity is first created. */
	private static final String LOGTAG = "MessageActivity";

	/* 顶部标题栏的控件 */
	private TextView button_back;
	/* 需要显示的文本信息 */
	private WebView mWebView;
	private String mMsgPath;
	private String mMsgName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message);

		/* 顶部标题栏的控件 */
		button_back = (TextView) findViewById(R.id.back_btn);
		mWebView = (WebView) findViewById(R.id.wv);
		/* 设置监听 */
		setListensers();

		Intent intent = getIntent();
		mMsgPath = intent.getStringExtra("msgPath");
		mMsgName = intent.getStringExtra("msgName");
		
		button_back.setText(mMsgName);
		readHtmlFormAssets();
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

	// 利用webview来显示帮助的文本信息
	private void readHtmlFormAssets() {
		WebSettings webSettings = mWebView.getSettings();

		webSettings.setLoadWithOverviewMode(true);
		// WebView双击变大，再双击后变小，当手动放大后，双击可以恢复到原始大小
		// webSettings.setUseWideViewPort(true);
		// 设置WebView可触摸放大缩小：
		// webSettings.setBuiltInZoomControls(true);
		// WebView 背景透明效果
		mWebView.setBackgroundColor(Color.TRANSPARENT);
		mWebView.loadUrl("file:///android_asset/html/" + mMsgPath);
	}
	
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
		menu.add(Menu.NONE, SUPPORT_ID, Menu.NONE, "帮助可可");
		menu.add(Menu.NONE, SETUP_ID, Menu.NONE, "设置");
		menu.add(Menu.NONE, APP_ID, Menu.NONE, "热门应用");

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) { // 获取Id
		case SUPPORT_ID:
			Intent intent1 = new Intent(MessageActivity.this, SupportKK.class);
			startActivity(intent1);
			break;
		case SETUP_ID:
			Intent intent2 = new Intent(MessageActivity.this, SetupActivity.class);
			startActivity(intent2);
			break;
		case APP_ID:	
			//获取全部自定义广告数据
			Intent appWallIntent = new Intent(this, AppWall.class);
			this.startActivity(appWallIntent);
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	// =================================================
}
