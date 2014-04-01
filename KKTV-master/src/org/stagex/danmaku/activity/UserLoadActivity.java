package org.stagex.danmaku.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.adapter.CustomExpandableAdapter;
import org.stagex.danmaku.util.AppWall;
import org.stagex.danmaku.util.BackupData;
import org.stagex.danmaku.util.ParseUtil;
import org.stagex.danmaku.util.SourceName;

import com.nmbb.oplayer.scanner.DbHelper;
import com.nmbb.oplayer.scanner.POUserDefChannel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ExpandableListView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint("NewApi")
public class UserLoadActivity extends Activity {
	/** Called when the activity is first created. */
	private static final String LOGTAG = "UserLoadActivity";

	/* 顶部标题栏的控件 */
	private TextView button_back;
	private ImageView button_search;
	private ImageView button_edit;
	// private ImageView button_defFav;
	/* ListView */
//	private ListView mTvList;

	private WebView mWebView;

	/* 频道收藏的数据库 */
	private DbHelper<POUserDefChannel> mDbHelper;

	private SharedPreferences sharedPreferences;
	private Editor editor;

	// ===============================
    private List<String> groupArray;
    private List<List<ChannelInfo>> childArray;
    CustomExpandableAdapter adapter;
    
    ExpandableListView listView;  
 // ===============================
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_load);

		/* 顶部标题栏的控件 */
		button_back = (TextView) findViewById(R.id.back_btn);
		button_search = (ImageView) findViewById(R.id.help_btn);
		button_edit = (ImageView) findViewById(R.id.edit_btn);
		// button_defFav = (ImageView) findViewById(R.id.fav_btn);

		mWebView = (WebView) findViewById(R.id.wv);

		sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
		editor = sharedPreferences.edit();

		/* 设置监听 */
		setListensers();

//		mTvList = (ListView) findViewById(R.id.tv_list);
		// 防止滑动黑屏
//		mTvList.setCacheColorHint(Color.TRANSPARENT);

        listView = (ExpandableListView) findViewById(R.id.sort_list);
		
		/* 频道收藏的数据库 */
		mDbHelper = new DbHelper<POUserDefChannel>();

		String path = Environment.getExternalStorageDirectory().getPath()
				+ "/kekePlayer/tvlist.txt";
		File listFile = new File(path);
		if (listFile.exists()) {

			// ===============================================================
			if (sharedPreferences.getBoolean("no_SelfFav_help", false) == false) {
				new AlertDialog.Builder(UserLoadActivity.this)
						.setIcon(R.drawable.ic_dialog_alert)
						.setTitle("温馨提示")
						.setMessage("长按频道名称可以实现收藏或取消收藏")
						.setPositiveButton("不再提醒",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// 不再收藏
										editor.putBoolean("no_SelfFav_help",
												true);
										editor.commit();
									}
								})
						.setNegativeButton("知道了",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								}).show();
			}
			// ===============================================================
			
			// FIXME just for test
//			mTvList.setVisibility(View.VISIBLE);
			mWebView.setVisibility(View.GONE);
			// 解析本地的自定义列表
//			infos = ParseUtil.parseDef(path);
			
//			mSourceAdapter = new ChannelLoadAdapter(this, infos, false);
//			mTvList.setAdapter(mSourceAdapter);
//			// 设置监听事件
//			mTvList.setOnItemClickListener(new OnItemClickListener() {
//				@Override
//				public void onItemClick(AdapterView<?> arg0, View arg1,
//						int arg2, long arg3) {
//					// TODO Auto-generated method stub
//					ChannelInfo info = (ChannelInfo) mTvList
//							.getItemAtPosition(arg2);
//
//					// FIXME 2013-07-31 这里的收藏就不放入是否收藏的按钮了
//					// startLiveMedia(info.getAllUrl(), info.getName(), false);
//					startLiveMedia(info.getAllUrl(), info.getName(), false,
//							"自定义频道");
//				}
//			});
//			// 增加长按频道收藏功能
//			mTvList.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//				@Override
//				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//						int arg2, long arg3) {
//					ChannelInfo info = (ChannelInfo) mTvList
//							.getItemAtPosition(arg2);
//					// 转换为数据库数据结构
//					POUserDefChannel POinfo = new POUserDefChannel(info, true);
//					showFavMsg(arg1, POinfo);
//					return true;
//				}
//			});
			
			// 实现自定义节目的分类

			groupArray = new ArrayList<String>();  
	        childArray = new ArrayList<List<ChannelInfo>>();  
	        
			parseDef(path);
	        
	        adapter = new CustomExpandableAdapter(this, groupArray, childArray, false);
	        listView.setAdapter(adapter);
	        listView.setVisibility(View.VISIBLE);
	        
	        //设置item点击的监听器
	        listView.setOnChildClickListener(new OnChildClickListener() {

	            @Override
	            public boolean onChildClick(ExpandableListView parent, View v,
	                    int groupPosition, int childPosition, long id) {

	                ChannelInfo info = (ChannelInfo)adapter.getChild(groupPosition, childPosition);
	                
	                startLiveMedia(info.getAllUrl(), info.getName(), false,
							"自定义频道");
	                return false;
	            }
	        });
	        
			// 长按收藏自定义频道
	        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View view,
						int pos, long id) {
					int groupPos = (Integer)view.getTag(R.id.channel_name); //参数值是在setTag时使用的对应资源id号
					int childPos = (Integer)view.getTag(R.id.channel_index);
					if(childPos == -1){//长按的是父项
					    //根据groupPos判断你长按的是哪个父项，做相应处理（弹框等）
					} else {
					    //根据groupPos及childPos判断你长按的是哪个父项下的哪个子项，然后做相应处理。	
						ChannelInfo info = (ChannelInfo)adapter.getChild(groupPos, childPos);
						// 转换为数据库数据结构
						POUserDefChannel POinfo = new POUserDefChannel(info, true);
						showFavMsg(arg0, POinfo);
					}
					return false;
				}
	        });
			
		} else {
			// 如果不存在，则显示帮助文档
			readHtmlFormAssets();
		}
	}

	// Listen for button clicks
	private void setListensers() {
		button_back.setOnClickListener(goListener);
		button_search.setOnClickListener(goListener);
		button_edit.setOnClickListener(goListener);
		// button_defFav.setOnClickListener(goListener);
	}

	/**
	 * 启动播放器界面
	 * 
	 * @param liveUrl
	 * @param name
	 * @param pos
	 */
	private void startLiveMedia(ArrayList<String> liveUrls, String name,
			Boolean channel_star, String sortName) {
		Intent intent = new Intent(UserLoadActivity.this, PlayerActivity.class);
		intent.putExtra("selected", 0);
		intent.putExtra("playlist", liveUrls);
		intent.putExtra("title", name);
		intent.putExtra("channelStar", channel_star);
		intent.putExtra("sortString", sortName);
		intent.putExtra("isSelfTV", true);
		// FIXME 2013-09-28 增加了播放界面的分类切台，需要分类序号
		intent.putExtra("channelSort", "8");
		intent.putExtra(
				"source",
				"线路" + Integer.toString(1) + "："
						+ SourceName.whichName(liveUrls.get(0)));

		startActivity(intent);
	}

	// // 打开网络媒体
	// private void startLiveMedia(ArrayList<String> all_url, String name,
	// Boolean isStar) {
	// // 如果该节目只有一个候选源地址，那么直接进入播放界面
	// if (all_url.size() == 1) {
	// Intent intent = new Intent(UserLoadActivity.this,
	// PlayerActivity.class);
	// ArrayList<String> playlist = new ArrayList<String>();
	// playlist.add(all_url.get(0));
	// intent.putExtra("selected", 0);
	// intent.putExtra("playlist", playlist);
	// intent.putExtra("title", name);
	// intent.putExtra("channelStar", isStar);
	// intent.putExtra("isSelfTV", true);
	// startActivity(intent);
	// } else {
	// // 否则进入候选源界面
	// Intent intent = new Intent(UserLoadActivity.this,
	// ChannelSourceActivity.class);
	// intent.putExtra("all_url", all_url);
	// intent.putExtra("channel_name", name);
	// intent.putExtra("channelStar", isStar);
	// intent.putExtra("isSelfTV", true);
	// startActivity(intent);
	// }
	// }

	// 按键监听
	private Button.OnClickListener goListener = new Button.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back_btn:
				// 回到上一个界面(Activity)

				// TODO 暂时在每次返回的时候，都进行备份数据库
				// new
				// BackupData(UserLoadActivity.this).execute("backupDatabase");
				new BackupData().execute("backupDatabase");

				finish();
				break;
			case R.id.help_btn:
				showHelp();
				break;
			case R.id.edit_btn:
				// 用户自己输入网址
				Intent intent = new Intent();
				intent.setClass(UserLoadActivity.this, UserDefActivity.class);
				startActivity(intent);
				break;
			// case R.id.fav_btn:
			// // TODO 暂时在每次打开自定义收藏的时候，都进行备份数据库
			// new BackupData(UserLoadActivity.this).execute("backupDatabase");
			// // 打开自定义的收藏频道
			// Intent intent_defFav = new Intent();
			// intent_defFav.setClass(UserLoadActivity.this,
			// UserDefFavActivity.class);
			// startActivity(intent_defFav);
			// break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};

	// 显示帮助对话框
	private void showHelp() {
		readHtmlFormAssets();
	}

	// 利用webview来显示帮助的文本信息
	private void readHtmlFormAssets() {
//		mTvList.setVisibility(View.GONE);
		listView.setVisibility(View.GONE);
		mWebView.setVisibility(View.VISIBLE);
		WebSettings webSettings = mWebView.getSettings();

		webSettings.setLoadWithOverviewMode(true);
		// WebView双击变大，再双击后变小，当手动放大后，双击可以恢复到原始大小
		// webSettings.setUseWideViewPort(true);
		// 设置WebView可触摸放大缩小：
		// webSettings.setBuiltInZoomControls(true);
		// WebView 背景透明效果
		mWebView.setBackgroundColor(Color.TRANSPARENT);
		mWebView.loadUrl("file:///android_asset/html/tvList_help.html");
	}

	/**
	 * 提示是否收藏为个性频道
	 */
	private void showFavMsg(View view, POUserDefChannel info) {

		final POUserDefChannel saveInfo = info;

		// // TODO 需要判断是否已经收藏过，先按名称，再判断地址
		// List<POUserDefChannel> exitInfo =
		// mDbHelper.queryForEq(POUserDefChannel.class, "name", saveInfo.name);
		// int size = exitInfo.size();
		// if (size == 1) {
		// ArrayList<String> loadUrl = info.getAllUrl();
		// ArrayList<String> exitUrl = exitInfo.get(0).getAllUrl();
		// int size1 = loadUrl.size();
		// int size2 = exitUrl.size();
		// for (int m = 0; m < size1; m++) {
		// for (int n = 0; n < size1; n++) {
		// // TODO 判断地址是否相同，不同则合并之
		// }
		// }
		//
		// } else {
		new AlertDialog.Builder(UserLoadActivity.this)
				.setIcon(R.drawable.ic_dialog_alert)
				.setTitle("温馨提示")
				.setMessage("确定收藏该自定义频道吗？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO 增加加入数据库操作
						saveInfo.date = DateFormat.format("MM月dd日",
								System.currentTimeMillis()).toString();
						mDbHelper.create(saveInfo);

						// // TODO 暂时在每次打开自定义收藏的时候，都进行备份数据库
						// new
						// BackupData(UserLoadActivity.this).execute("backupDatabase");

					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).show();
	}

	// }

	/**
	 * 在主界面按下返回键，提示用户是否退出应用
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 按下键盘上返回按钮
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// TODO 暂时在每次返回的时候，都进行备份数据库
			// new BackupData(UserLoadActivity.this).execute("backupDatabase");
			new BackupData().execute("backupDatabase");

			finish();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
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
		menu.add(Menu.NONE, SUPPORT_ID, Menu.NONE, "帮助可可").setIcon(R.drawable.ic_tuangou_pressed);
		menu.add(Menu.NONE, SETUP_ID, Menu.NONE, "设置").setIcon(R.drawable.ic_setup2);
		menu.add(Menu.NONE, APP_ID, Menu.NONE, "热门应用").setIcon(R.drawable.ic_star);

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) { // 获取Id
		case SUPPORT_ID:
			Intent intent1 = new Intent(UserLoadActivity.this, SupportKK.class);
			startActivity(intent1);
			break;
		case SETUP_ID:
			Intent intent2 = new Intent(UserLoadActivity.this,
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
	
	// 解析本地自定义的列表
		// TODO 2013-09-26
		// 为了加强自定义的分类功能，采用数据库暂存所有的自定义
		// 的数据，每次加载时，重新入数据库？或者查看自定义文件
		// 是否作出了修改，若修改了，则清除数据库数据，重新装载
		private void parseDef(String tvList) {
			List<ChannelInfo> list = new ArrayList<ChannelInfo>();
			
			int nums = 0;
			int groupIndex = -1;
			
			String code = "GBK";
			String privName = null;
			String sortName = null;
			String privSort = null;
			String tvName = null;
			String first_url = null;
			List<String> list_url = new ArrayList<String>();

			// FIXBUG 2013-07-28
			Boolean dropLast = true;

			Boolean canSort = false;
			
			try {
				// 探测txt文件的编码格式
				code = ParseUtil.codeString(tvList);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				InputStream is = new FileInputStream(tvList);
				InputStreamReader ir = new InputStreamReader(is, code);
				BufferedReader br = new BufferedReader(ir);
				try {
					while (true) {
						String line = br.readLine();
						if (line == null) {
							// FIXBUG 2013-07-28
							if (dropLast != true) {
								// 最后一组节目源
								String[] second_url = new String[list_url.size()];
								list_url.toArray(second_url);
								ChannelInfo info = new ChannelInfo(0, privName,
										null, null, null, first_url, second_url,
										null, null);
								list.add(info);
							}
							
							childArray.add(groupIndex, list);
							
//							Log.d("sort", "===>last size = " + list.size());
							
							break;
						}

						// FIXBUG 2013-07-28
						dropLast = false;

						// 如果不符合要求（节目名和节目地址以英文逗号隔开）直接忽略该行
						// FIXME bug#0019
						String[] pair = line.split(",");
						int strLen = pair.length;
						if (strLen < 2) {
							// FIXBUG 2013-07-28
							dropLast = true;
							continue;
						}
						nums++;
						String scName = pair[0].trim();
						String url = null;
						if (strLen == 2) {
							url = pair[1].trim();
						} else {
							StringBuffer urlBuf = new StringBuffer();
							for (int i = 1; i < strLen; i++) {
								if (i >= 2)
									urlBuf.append(",");
								urlBuf.append(pair[i].trim());
							}
							url = urlBuf.toString();
						}
						
						// 2013-09-24 提取出自定义的分类名称
						String[] pair2 = scName.split("\\|");
						if (pair2.length != 2) {
							// 如果没有分类名称，则统一为"其他"
							sortName = "未分类";
							tvName = scName;
							// TODO 2013-09-27
							// 需要对没有分类的自定义作出区别？？？
							//end
						} else {
							tvName = pair2[1].trim();
							sortName = pair2[0].trim();
						}
						// TODO 暂时按照顺序来比较，提取分类
						if (privSort == null) {
							// 第一次出现，直接存储，后续以文件存储？
							privSort = sortName;
							groupIndex++;
							groupArray.add(privSort);
							
							list.clear();
						} else {
							// 将新的分类存储起来
							if (sortName.equals(privSort) == false) {
//								Log.i("sort", "===> sort name $$$ " + privSort);
								privSort = sortName;
								
//								Log.i("sort", "===> sort name = " + privSort);
								
								groupArray.add(privSort);

								canSort = true;

							}
						}
						// end
						
						// TODO 合并相同节目名称的源
						if (tvName.equals(privName) && !canSort)
							list_url.add(url);
						else {
							if (privName != null) {
								// 保存节目源
								String[] second_url = new String[list_url.size()];
								list_url.toArray(second_url);
								ChannelInfo info = new ChannelInfo(0, privName,
										null, sortName, null, first_url, second_url,
										null, null);
								list.add(info);
							}
							
							list_url.clear();
							first_url = url;
							privName = tvName;
						}
						// end
						
						// 保存某一分类数据
						if (canSort) {
							childArray.add(groupIndex, list);
							
							groupIndex++;
							
//							Log.d("sort", "===>sort size = " + list.size());
							
							list = new ArrayList<ChannelInfo>();
							canSort = false;
						}
						// end
						
					}
				} finally {
					br.close();
					ir.close();
					is.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.d("ParseUtil", "user define tvlist nums = " + nums);
		}
}
