package org.stagex.danmaku.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelDefFavAdapter;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.adapter.ChannelListAdapter;
import org.stagex.danmaku.adapter.ChannelLoadAdapter;
import org.stagex.danmaku.adapter.ChannelSourceAdapter;
import org.stagex.danmaku.adapter.CustomExpandableAdapter;
import org.stagex.danmaku.adapter.CustomExpandableAdapterDF;
import org.stagex.danmaku.adapter.ProvinceInfo;
import org.stagex.danmaku.util.ParseUtil;
import org.stagex.danmaku.util.ProgramTask;
import org.stagex.danmaku.util.SourceName;
import org.stagex.danmaku.util.SystemUtility;

import com.hp.hpl.sparta.xpath.PositionEqualsExpr;
import com.nmbb.oplayer.scanner.ChannelListBusiness;
import com.nmbb.oplayer.scanner.DbHelper;
import com.nmbb.oplayer.scanner.POChannelList;
import com.nmbb.oplayer.scanner.POUserDefChannel;
import com.togic.mediacenter.player.AbsMediaPlayer;
import com.togic.mediacenter.player.DefMediaPlayer;
import com.togic.mediacenter.player.VlcMediaPlayer;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

public class PlayerActivity extends Activity implements
		AbsMediaPlayer.OnBufferingUpdateListener,
		AbsMediaPlayer.OnCompletionListener, AbsMediaPlayer.OnErrorListener,
		AbsMediaPlayer.OnInfoListener, AbsMediaPlayer.OnPreparedListener,
		AbsMediaPlayer.OnProgressUpdateListener,
		AbsMediaPlayer.OnVideoSizeChangedListener, OnClickListener,
		OnSeekBarChangeListener {

	static final String LOGTAG = "PlayerActivity";

//	private static final int SURFACE_NONE = 0;
	private static final int SURFACE_FILL = 0;
	private static final int SURFACE_ORIG = 1;
	private static final int SURFACE_4_3 = 2;
	private static final int SURFACE_16_9 = 3;
	private static final int SURFACE_16_10 = 4;
	private static final int SURFACE_MAX = 5;

	private static final int MEDIA_PLAYER_BUFFERING_UPDATE = 0x4001;
	private static final int MEDIA_PLAYER_COMPLETION = 0x4002;
	private static final int MEDIA_PLAYER_ERROR = 0x4003;
	private static final int MEDIA_PLAYER_INFO = 0x4004;
	private static final int MEDIA_PLAYER_PREPARED = 0x4005;
	private static final int MEDIA_PLAYER_PROGRESS_UPDATE = 0x4006;
	private static final int MEDIA_PLAYER_VIDEO_SIZE_CHANGED = 0x4007;

	/* the media player */
	private AbsMediaPlayer mMediaPlayer = null;

	/* */
	private ArrayList<String> mPlayListArray = null;
	private int mPlayListSelected = -1;

	/* GUI evnet handler */
	private Handler mEventHandler;

	/* player misc */
	private ProgressBar mProgressBarPreparing;
	// private TextView mLoadingTxt;
	private TextView mPercentTxt;

	/* player controls */
	private TextView mTitle;
	private TextView mSource;
	private TextView mSysTime;
	private TextView mBattery;
	private TextView mTextViewTime;
	private TextView mCodecMode;
	private SeekBar mSeekBarProgress;
	private TextView mTextViewLength;
	private TextView mSelfdef;
	// 点击阴影部分也不会导致隐藏
	private LinearLayout player_overlay_header;
	private LinearLayout interface_overlay;
	private LinearLayout seekbar_overlay;
	private LinearLayout program_overlay;
	// end
	private ImageButton mImageButtonStar;
	private ImageButton mImageButtonToggleMessage;
	private ImageButton mImageButtonSwitchAudio;
	private ImageButton mImageButtonSwitchSubtitle;
	private ImageButton mImageButtonPrevious;
	private ImageButton mImageButtonTogglePlay;
	private ImageButton mImageButtonNext;
	private ImageButton mImageButtonSwitchAspectRatio;

	private RelativeLayout mLinearLayoutControlBar;

	/* player video */
	private SurfaceView mSurfaceViewDef;
	private SurfaceHolder mSurfaceHolderDef;
	private SurfaceView mSurfaceViewVlc;
	private SurfaceHolder mSurfaceHolderVlc;

	/* misc */
	private boolean mMediaPlayerLoaded = false;
	private boolean mMediaPlayerStarted = false;

	/* misc */
	private int mTime = -1;
	private int mLength = -1;
	private boolean mCanSeek = true;
	private int mAspectRatio = 0; // TODO根据设置里面的值提取

	/* title name */
	private String mTitleName;
	private String mSourceName;

	// private int mAudioTrackIndex = 0;
	// private int mAudioTrackCount = 0;
	// private int mSubtitleTrackIndex = 0;
	// private int mSubtitleTrackCount = 0;

	/* 播放界面选台和切源 */
	private LinearLayout mLinearLayoutSourceList;
	private RelativeLayout mLinearLayoutChannelList;
	private TextView mSortName;
	private TextView mSourcePath;
	private ImageView mSortLeft;
	private ImageView mSortRight;
	private String sortString;
	private Boolean isFavSort = false;
	private ListView source_list;
	private ListView channel_list;
	private TextView list_load;
	// 官方频道数据结构
	private List<POChannelList> channel_infos = null;
	// 自定义收藏频道数据结构
	private List<POUserDefChannel> userdef_infos = null;
	// 自定义频道数据结构
//	private List<ChannelInfo> userload_infos = null;
	private ImageButton mImageButtonList;
	private ImageButton mImageButtonChannel;
	// 2013-10-16 标记第一次打开频道切换
	private Boolean mFirstLoad = true;
	private int mSourceNum = 0;
	private int mSourceIndex = 0;
	// TODO 2013-10-23 从列表界面进入播放界面暂时没有标记index，赋值为-1
	private int mChannelIndex = -1;
	private int mChannelGroup = -1;
	
	private Boolean no_player_help1;
	private Boolean no_player_help2;
	private int no_player_help2_num = 0;
	
	/**
	 * 增加手势控制
	 * 
	 * @{
	 */
	private View mVolumeBrightnessLayout;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private AudioManager mAudioManager;
	/** 最大声音 */
	private int mMaxVolume;
	/** 当前声音 */
	private int mVolume = -1;
	/** 当前亮度 */
	private float mBrightness = -1f;
	/** 当前缩放模式 */
	// private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;
	/** 响应函数是否生效的标志位 */
	private boolean mDoHandleAll = false;
	private boolean mDoHandleClick = false;
	private boolean mDoHandleSeek = false;

	private static final int MSG_CTL_ALL = 0;
	private static final int MSG_CTL_CLICK = 1;
	private static final int MSG_CTL_SEEKBAR = 2;

	private GestureDetector mGestureDetector;
	/* @} */

	/* 记录硬解码与软解码的状态 */
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private boolean isHardDec;
	/* 记录直播电视还是本地媒体状态 */
	private boolean isLiveMedia;

	/* 频道收藏的数据库 */
	private DbHelper<POChannelList> mDbHelper;
	private DbHelper<POUserDefChannel> mSelfDbHelper;
	private Boolean channelStar = false;
	List<POChannelList> channelList = null;

	// TODO 目前只按照节目源的类别来切台
	private String channelSort = null;

	/* 是否是自定义频道 */
	private Boolean isSelfTV = false;
	// 标识是自定义的收藏频道
	private Boolean isSelfFavTV = false;

	// 播放界面的频道分类切换
	private String[] chSorts= {"1.央视", "2.卫视", "3.地方", "4.体育", "5.港澳台", "6.其他", "7.收藏", "8.自定义", "9.自定义收藏"};
	private int chSortNum = chSorts.length;
	// 当前切换到的频道分类
	private int curChSortIndex = 0;
	// 当前正在播放的频道分类
	private int curPlaySortIndex = 0;
	// 当前播放的分类，与当前切换的分类是不同的
	private int curPlaySort = 0;		// 主要是区分是否是自定义, 0是自定义，1是官方频道
	private Boolean isListLoading = false;
	
	// 自定义的toast界面
	private TextView customToastText;
	private View customToastLayout;
	private String ratioView[] = {"全 屏", "原 始", "4：3", "16：9", "16：10"};
	private Toast customToast;
	//================================================
	// 节目预告相关的代码
	private TextView programText;
	private ProgramTask mProgramtask = null;
	private String mPrograPath = null;
	
	// TODO 由于点击屏幕会出现当前节目，所以要有两个变量来计算时间间隔
	private Date mDateLast;
	private Date mDateCur;
	private long mTimeLast;							// 刚打开的时候初始化
	private final long DIFFTIME = 3 * 60;	// 3 分钟
	
	// 是否隐藏播放进度条
	private boolean bShowSeekbar;
	//================================================
	/**
	 * 初始化自定义的Toast界面
	 */
	public void initCustomToast() {
		//获取LayoutInflater对象，该对象能把XML文件转换为与之一直的View对象
        LayoutInflater inflater = getLayoutInflater();
        //根据指定的布局文件创建一个具有层级关系的View对象
        //第二个参数为View对象的根节点，即LinearLayout的ID
        customToastLayout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout_root));
          
        //查找控件
        //注意是在layout中查找
        customToastText = (TextView) customToastLayout.findViewById(R.id.text);
        
        customToast = new Toast(getApplicationContext());
        //设置Toast的位置  
        customToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        customToast.setDuration(Toast.LENGTH_SHORT);
		 
        //让Toast显示为我们自定义的样子  
		customToast.setView(customToastLayout);
	}
	
	/**
	 * 显示自定义Toast界面
	 */
	public void showCustomToast(String value) {
		customToastText.setText(value);

		customToast.show();
	}
	//================================================
	
	/**
	 * 判断使用的解码接口
	 * 
	 * @param obj
	 * @return
	 */
	private static boolean isDefMediaPlayer(Object obj) {
		return obj.getClass().getName()
				.compareTo(DefMediaPlayer.class.getName()) == 0;
	}

	private static boolean isVlcMediaPlayer(Object obj) {
		return obj.getClass().getName()
				.compareTo(VlcMediaPlayer.class.getName()) == 0;
	}

	/**
	 * 播放过程中的事件响应的核心处理方法
	 */
	private void initializeEvents() {
		mEventHandler = new Handler() {
			public void handleMessage(Message msg) {
				// Log.d(LOGTAG, "===> get message [" + msg.what + "]");
				switch (msg.what) {
				case MEDIA_PLAYER_BUFFERING_UPDATE: {
					// FIXBUG bug#0023 这里的判断标志会引起第一次启动时没有
					// 缓冲百分比的提示
					// if (mMediaPlayerLoaded) {
					// Log.d(LOGTAG, "===>load   " + msg.arg1 + "%");
					mPercentTxt.setText("正在缓冲===> " + String.valueOf(msg.arg1)
							+ "%");

					mPercentTxt.setVisibility(msg.arg1 < 100 ? View.VISIBLE
							: View.GONE);
					
					// 显示播放界面的帮助信息
					if (msg.arg1 == 100)
						if (no_player_help1 == false) {
							new AlertDialog.Builder(PlayerActivity.this)
									.setIcon(R.drawable.ic_dialog_alert)
									.setTitle("音量和亮度调节")
									.setMessage(
											"上下滑动右半屏幕可以调节音量\n\n上下滑动左半屏幕可以调节亮度")
									.setPositiveButton("不再提醒",
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog,
														int which) {
													// 不再收藏
													no_player_help1 = true;
													editor.putBoolean("no_player_help1", true);
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
					// ===================

					mProgressBarPreparing
							.setVisibility(msg.arg1 < 100 ? View.VISIBLE
									: View.GONE);

					// mLoadingTxt
					// .setVisibility(msg.arg1 < 100 ? View.VISIBLE
					// : View.GONE);
				}
					break;
				// }
				case MEDIA_PLAYER_COMPLETION: {
					Log.w(LOGTAG, "MEDIA_PLAYER_COMPLETION");
					/* TODO 播放结束后，如何处理 */
					// 使用通知窗口
					// Toast.makeText(getApplicationContext(),"播放结束，请按返回键",
					// Toast.LENGTH_LONG).show();
					// 使用警告窗口 @{
					// FIXME 判断当前是否是直播电视状态，如果是，则此时的结束播放
					// 是由于网络链接中断引起的，立即重新启动@{
					isLiveMedia = sharedPreferences.getBoolean("isLiveMedia",
							true);
					if (isLiveMedia) {
						// modify 2013-08-31
						reConnectSource(mPlayListArray.get(mPlayListSelected));
					} else
						// @{
						new AlertDialog.Builder(PlayerActivity.this)
								.setIcon(R.drawable.ic_about)
								.setTitle("播放结束")
								.setMessage("该视频已经播放结束.")
								.setNegativeButton("知道了",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing - it will close on
												// its
												// own
												// 关闭当前的PlayerActivity，退回listview的界面
												finish();
											}
										}).show();
					// @}
					break;
				}
				/* FIXME 这里的处理有待进一步细化 */
				case MEDIA_PLAYER_ERROR: {
					Log.e(LOGTAG, "MEDIA_PLAYER_ERROR");
					Boolean changeFlag = false;
					//===================================
					/* TODO 2013-08-31
					 * 这是播放界面切源的主体部分
					 */
					if (mSourceIndex < mSourceNum - 1) {
						// 还有别的源可以使用
						mSourceIndex++;
						
						changeFlag = true;
					}
					
					//===================================
					
					/* fall back to VlcMediaPlayer if possible */
					if (isDefMediaPlayer(msg.obj)) {
						// Log.i(LOGTAG,
						// "DefMediaPlayer selectMediaPlayer（VLC）");
						// selectMediaPlayer(
						// mPlayListArray.get(mPlayListSelected), true);
						// break;
						mProgressBarPreparing.setVisibility(View.GONE);
						// FIXME bug#0023
						mPercentTxt.setVisibility(View.GONE);
						// mLoadingTxt.setVisibility(View.GONE);
						
						if (changeFlag) {
							reConnectSource(mPlayListArray.get(mSourceIndex));
							mSourceName = "线路" + Integer.toString(mSourceIndex + 1) + "：" + SourceName.whichName(mPlayListArray.get(mSourceIndex));
							Toast.makeText(PlayerActivity.this, "线路" + mSourceIndex + "已失效，尝试线路" + (mSourceIndex + 1), Toast.LENGTH_LONG).show();
							
							// TODO 2013-10-26 标记当前正在播放的线路
							if (mSourceAdapter != null) {
								mSourceAdapter.mCurrentIndex = mSourceIndex;
								mSourceAdapter.notifyDataSetChanged();
							}
							
						} else
						
						/* TODO 用在硬解解码模式，判断不支持的源 */
						new AlertDialog.Builder(PlayerActivity.this)
								.setIcon(R.drawable.ic_dialog_alert)
//								.setTitle("播放失败【硬解码】")
								.setTitle(mTitleName)
								.setMessage(
										"抱歉，当前频道线路异常，暂停直播！\n\n您可切换至【软解码】模式再次尝试，或稍后重试！\n\n现在切换解码模式吗？")
								.setPositiveButton("切换",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing - it will close on
												// its own
												Intent intent = new Intent();
												// 跳转至设置界面
												intent.setClass(
														PlayerActivity.this,
														SetupActivity.class);
												startActivity(intent);
												finish();
											}
										})
								.setNegativeButton("暂不",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing - it will close on
												// its own
												// 关闭当前的PlayerActivity，退回listview的界面
												// 2013-09-06 暂时去掉
//												finish();
											}
										}).show();
						// @}
						// Log.i(LOGTAG, "get out of alert");
						break;
					} else if (isVlcMediaPlayer(msg.obj)) {
						// Log.i(LOGTAG, "VlcMediaPlayer");
						
						/* destroy media player */
						// TODO 2013-08-31屏蔽这行代码的原因是，目前屏幕切台不关闭当前的surfaceView
						// 否则，会造成新的连接崩溃，类似于当前的直播源断掉了，重新连接的场景
//						mSurfaceViewVlc.setVisibility(View.GONE);
						// end 2013-08-31
						
						// Log.i(LOGTAG, "VlcMediaPlayer update UI");
						mProgressBarPreparing.setVisibility(View.GONE);
						// FIXME bug#0023
						mPercentTxt.setVisibility(View.GONE);
						// mLoadingTxt.setVisibility(View.GONE);
						
						if (changeFlag) {
							reConnectSource(mPlayListArray.get(mSourceIndex));
							mSourceName = "线路" + Integer.toString(mSourceIndex + 1) + "：" + SourceName.whichName(mPlayListArray.get(mSourceIndex));
							Toast.makeText(PlayerActivity.this, "线路" + mSourceIndex + "已失效，尝试线路" + (mSourceIndex + 1), Toast.LENGTH_LONG).show();
							
							// TODO 2013-10-26 标记当前正在播放的线路
							if (mSourceAdapter != null) {
								mSourceAdapter.mCurrentIndex = mSourceIndex;
								mSourceAdapter.notifyDataSetChanged();
							}
						} else
						
						// 弹出播放失败的窗口@{
						new AlertDialog.Builder(PlayerActivity.this)
								.setIcon(R.drawable.ic_dialog_alert)
//								.setTitle("播放失败")
								.setTitle(mTitleName)
								.setMessage(
										"抱歉，当前频道线路异常，暂停直播！\n\n请稍后重试！")
								.setPositiveButton("继续",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing - it will close on
												
											}
										})
								.setNegativeButton("退出",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing - it will close on
												// its own
												// 关闭当前的PlayerActivity，退回listview的界面
												// TODO 2013-08-31 如果是单个源
												finish();
											}
										}).show();
						// @}
						// Log.i(LOGTAG, "get out of alert");
						break;
					}
				}
				case MEDIA_PLAYER_INFO: {
					if (msg.arg1 == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
						mCanSeek = false;
					}
					break;
				}
				case MEDIA_PLAYER_PREPARED: {
					Log.i(LOGTAG, "===> MEDIA_PLAYER_PREPARED");
					// FIXME bug#0023 对于rtmp的视频，不会有该message
					// 因此是个bug，暂时将mMediaPlayerLoaded =
					// true在MEDIA_PLAYER_PROGRESS_UPDATE
					// 中也进行置位操作
					if (isDefMediaPlayer(msg.obj) || isVlcMediaPlayer(msg.obj)) {
						/* update status */
						mMediaPlayerLoaded = true;
					}
					/* update UI */
					if (mMediaPlayerLoaded) {
						mProgressBarPreparing.setVisibility(View.GONE);
						// mLoadingTxt.setVisibility(View.GONE);
						// FIXME bug#0023
						mPercentTxt.setVisibility(View.GONE);
					}
					startMediaPlayer();
					break;
				}
				case MEDIA_PLAYER_PROGRESS_UPDATE: {
					// FIXME bug#0023
					mMediaPlayerLoaded = true;
					//
					if (mMediaPlayer != null) {
						int length = msg.arg2;
						if (length >= 0) {
							mLength = length;
							mTextViewLength.setText(SystemUtility
									.getTimeString(mLength));
							mSeekBarProgress.setMax(mLength);
						}
						int time = msg.arg1;
						if (time >= 0) {
							mTime = time;
							mTextViewTime.setText(SystemUtility
									.getTimeString(mTime));
							mSeekBarProgress.setProgress(mTime);
						}
					}
					break;
				}
				case MEDIA_PLAYER_VIDEO_SIZE_CHANGED: {
					Log.i(LOGTAG, "===> MEDIA_PLAYER_VIDEO_SIZE_CHANGED");
					AbsMediaPlayer player = (AbsMediaPlayer) msg.obj;
					SurfaceView surface = isDefMediaPlayer(player) ? mSurfaceViewDef
							: mSurfaceViewVlc;
					int ar = mAspectRatio;
					// 根据设置，改变播放界面大小和比例
					changeSurfaceSize(player, surface, ar);
					break;
				}
				default:
					break;
				}
			}
		};
	}

	/**
	 * 播放控件初始化：创建surface、获取各子控件的id
	 */
	protected void initializeControls() {
		/* SufaceView used by VLC is a normal surface */
		mSurfaceViewVlc = (SurfaceView) findViewById(R.id.player_surface_vlc);
		mSurfaceHolderVlc = mSurfaceViewVlc.getHolder();
		mSurfaceHolderVlc.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		mSurfaceHolderVlc.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
//				Log.i(LOGTAG, "===> surfaceCreated");
				createMediaPlayer(false, mPlayListArray.get(mPlayListSelected),
						mSurfaceHolderVlc);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
//				Log.i(LOGTAG, "===> surfaceChanged");
				mMediaPlayer.setDisplay(holder);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
//				Log.i(LOGTAG, "===> surfaceDestroyed");
				destroyMediaPlayer(false);
			}

		});
		/* SurfaceView used by MediaPlayer is a PUSH_BUFFERS surface */
		mSurfaceViewDef = (SurfaceView) findViewById(R.id.player_surface_def);
		mSurfaceHolderDef = mSurfaceViewDef.getHolder();
		mSurfaceHolderDef.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHolderDef.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
//				Log.i(LOGTAG, "===> surfaceCreated");
				createMediaPlayer(true, mPlayListArray.get(mPlayListSelected),
						mSurfaceHolderDef);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
//				Log.i(LOGTAG, "===> surfaceChanged");
				mMediaPlayer.setDisplay(holder);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
//				Log.i(LOGTAG, "===> surfaceDestroyed");
				destroyMediaPlayer(true);
			}

		});

		// TODO 2013-08-01
		mSelfdef = (TextView) findViewById(R.id.selfdef_tv);

		// overlay header
		mTitle = (TextView) findViewById(R.id.player_overlay_title);
		mSource = (TextView) findViewById(R.id.player_overlay_name);
		mSysTime = (TextView) findViewById(R.id.player_overlay_systime);
		mBattery = (TextView) findViewById(R.id.player_overlay_battery);
		mCodecMode = (TextView) findViewById(R.id.player_codec_mode);

		// seekbar和两端的岂止时间
		mTextViewTime = (TextView) findViewById(R.id.player_text_position);
		mSeekBarProgress = (SeekBar) findViewById(R.id.player_seekbar_progress);
		mSeekBarProgress.setOnSeekBarChangeListener(this);
		mTextViewLength = (TextView) findViewById(R.id.player_text_length);

		// 点击阴影部分也不会隐藏（并不是正在的监听响应，只是注册个监听）
		player_overlay_header = (LinearLayout) findViewById(R.id.player_overlay_header);
		player_overlay_header.setOnClickListener(this);
		interface_overlay = (LinearLayout) findViewById(R.id.interface_overlay);
		interface_overlay.setOnClickListener(this);
		seekbar_overlay = (LinearLayout) findViewById(R.id.seekbar_overlay);
		seekbar_overlay.setOnClickListener(this);
		program_overlay = (LinearLayout) findViewById(R.id.program_overlay);
		program_overlay.setOnClickListener(this);
		
		// 播放控件
		mImageButtonStar = (ImageButton) findViewById(R.id.player_button_star);
		mImageButtonStar.setOnClickListener(this);
		mImageButtonToggleMessage = (ImageButton) findViewById(R.id.player_button_toggle_message);
		mImageButtonToggleMessage.setOnClickListener(this);
		mImageButtonSwitchAudio = (ImageButton) findViewById(R.id.player_button_switch_audio);
		mImageButtonSwitchAudio.setOnClickListener(this);
		mImageButtonSwitchSubtitle = (ImageButton) findViewById(R.id.player_button_switch_subtitle);
		mImageButtonSwitchSubtitle.setOnClickListener(this);
		mImageButtonPrevious = (ImageButton) findViewById(R.id.player_button_previous);
		mImageButtonPrevious.setOnClickListener(this);
		mImageButtonTogglePlay = (ImageButton) findViewById(R.id.player_button_toggle_play);
		mImageButtonTogglePlay.setOnClickListener(this);
		mImageButtonNext = (ImageButton) findViewById(R.id.player_button_next);
		mImageButtonNext.setOnClickListener(this);
		mImageButtonSwitchAspectRatio = (ImageButton) findViewById(R.id.player_button_switch_aspect_ratio);
		mImageButtonSwitchAspectRatio.setOnClickListener(this);

		mLinearLayoutControlBar = (RelativeLayout) findViewById(R.id.player_control_bar);

		// 缓冲进度圈
		mProgressBarPreparing = (ProgressBar) findViewById(R.id.player_prepairing);
		// 缓冲提示语言
		// mLoadingTxt = (TextView) findViewById(R.id.player_loading);
		// 缓冲比例
		mPercentTxt = (TextView) findViewById(R.id.buffer_percent);

		// =====================================================
		// 播放界面切源以及选台功能
		mLinearLayoutSourceList= (LinearLayout) findViewById(R.id.player_sourcelist);
		source_list = (ListView) findViewById(R.id.source_list);
		mSourcePath = (TextView) findViewById(R.id.source_path);
		mSourcePath.setOnClickListener(this);
		// 防止滑动黑屏
		source_list.setCacheColorHint(Color.TRANSPARENT);
		mImageButtonList = (ImageButton) findViewById(R.id.player_button_list);
		mImageButtonList.setOnClickListener(this);
		
		mLinearLayoutChannelList= (RelativeLayout) findViewById(R.id.player_channellist);
		mSortName = (TextView) findViewById(R.id.sort_name);
		mSortName.setOnClickListener(this);
		channel_list = (ListView) findViewById(R.id.channel_list);
		list_load = (TextView) findViewById(R.id.load_name);
		// 左右滑动，替换之前的mSortName的右滑动
		mSortLeft = (ImageView) findViewById(R.id.sort_left);
		mSortLeft.setOnClickListener(this);
		mSortRight = (ImageView) findViewById(R.id.sort_right);
		mSortRight.setOnClickListener(this);
		// 防止滑动黑屏
		channel_list.setCacheColorHint(Color.TRANSPARENT);
		mImageButtonChannel = (ImageButton) findViewById(R.id.player_button_channel);
		mImageButtonChannel.setOnClickListener(this);
		// =====================================================

		// =====================================================
		epdListView = (ExpandableListView) findViewById(R.id.sort_list);
		// =====================================================
		// 初始化手势
		initGesture();

		// 初始化电量监测
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		// filter.addAction(VLCApplication.SLEEP_INTENT);
		registerReceiver(mReceiver, filter);
		
		// 2013-10-14 节目预告相关
		programText = (TextView)findViewById(R.id.program_txt);
	}

	/**
	 * 获取播放需要的视频数据
	 */
	protected void initializeData() {
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_VIEW)) {
			String one = intent.getDataString();
			mPlayListSelected = 0;
			mPlayListArray = new ArrayList<String>();
			mPlayListArray.add(one);
		} else {
			mPlayListSelected = intent.getIntExtra("selected", 0);
			mSourceIndex = 0;
			mPlayListArray = intent.getStringArrayListExtra("playlist");
			channelStar = intent.getBooleanExtra("channelStar", false);
			// Log.d(LOGTAG, "===>>>" + mTitleName);
			mTitleName = intent.getStringExtra("title");
			sortString = intent.getStringExtra("sortString");
			// 如果是官方收藏频道，数据库查找的方式不同
			isFavSort = intent.getBooleanExtra("favSort", false);
			mSourceName = intent.getStringExtra("source");
			// 如果是自定义（收藏）频道，数据库查找的方式不同
			isSelfTV = intent.getBooleanExtra("isSelfTV", false);
			isSelfFavTV = intent.getBooleanExtra("isSelfFavTV", false);
			// 需要按照频道的分类来传入相应的值，0表示不支持切台
			channelSort = intent.getStringExtra("channelSort");
			
			// 确认当前切换分类数组的序号
			// FIXME 2013-09-28 需要自定义频道传入相应的序列号

			if (channelSort == null)
				channelSort = "8";		//主要针对输入的网址行为，跳转到自定义频道
			curChSortIndex = Integer.parseInt(channelSort) - 1;
			// 点击列表进来之后，当前切换分类值和当前播放分类值是相等的
			curPlaySortIndex = curChSortIndex;
			
			if (isSelfTV || isSelfFavTV ) {
				curPlaySort = 0;
			} else {
				curPlaySort = 1;
			}
			
		}
		if (mPlayListArray == null || mPlayListArray.size() == 0) {
			Log.e(LOGTAG, "initializeData(): empty");
			finish();
			return;
		}
		mSourceNum = mPlayListArray.size();
		
		// TODO 2013-10-20 解析出首次进入播放界面频道的节目预告
		mPrograPath = intent.getStringExtra("prograPath");
		if (mPrograPath != null) {
			mProgramtask = new ProgramTask(programText);
	        mProgramtask.execute(mPrograPath);
		}
		
		// 获取首次的时间
		mTimeLast = new Date().getTime();
		
	}

	/**
	 * 重新设置播放器，控制各控件界面显示与否
	 */
	protected void resetMediaPlayer() {
		int resource = -1;
		/* initial status */
		mMediaPlayerLoaded = false;
		mTime = -1;
		mLength = -1;
		mCanSeek = true;
//		mAspectRatio = 0; // 直接全屏 2013-10-11 保持之前设置的比例
		/* */
		mImageButtonToggleMessage.setVisibility(View.GONE);
		mImageButtonSwitchAudio.setVisibility(View.GONE);
		mImageButtonSwitchSubtitle.setVisibility(View.GONE);
		// TODO 2013-08-31 暂时不使用该功能
//		mImageButtonPrevious
//				.setVisibility((mPlayListArray.size() == 1) ? View.GONE
//						: View.VISIBLE);
		mImageButtonPrevious.setVisibility(View.GONE);
		// end

		// 判断是否以收藏
		if (channelStar) {
			resource = SystemUtility.getDrawableId("ic_fav_pressed");
			mImageButtonStar.setBackgroundResource(resource);
		} else {
			resource = SystemUtility.getDrawableId("ic_fav");
			mImageButtonStar.setBackgroundResource(resource);
		}

		// TODO 2013-08-01 自定义频道暂时不支持在播放界面收藏
		// TODO 2013-09-28 增加了播放界面频道切换功能之后，需要
		// 修改该逻辑，根据专门的标志位切换图标
//		if (isSelfTV || isSelfFavTV) {
		if (curPlaySort == 0) {
			mImageButtonStar.setVisibility(View.GONE);
			mSelfdef.setVisibility(View.VISIBLE);
		} else {
			mImageButtonStar.setVisibility(View.VISIBLE);
			mSelfdef.setVisibility(View.GONE);
		}

		mImageButtonTogglePlay.setVisibility(View.VISIBLE);
		resource = SystemUtility.getDrawableId("btn_play_1");
		mImageButtonTogglePlay.setBackgroundResource(resource);
		// TODO 2013-08-31 暂时不使用该功能
//		mImageButtonNext.setVisibility((mPlayListArray.size() == 1) ? View.GONE
//				: View.VISIBLE);
		mImageButtonNext.setVisibility(View.GONE);
		// end
		mImageButtonSwitchAspectRatio.setVisibility(View.VISIBLE);
		// TODO 2013-10-06 暂时去掉这部分代码，改为信息提醒
//		resource = SystemUtility.getDrawableId("btn_aspect_ratio_0");
//		mImageButtonSwitchAspectRatio.setBackgroundResource(resource);
		/* */
		mLinearLayoutControlBar.setVisibility(View.GONE);
	}

	/**
	 * TODO 选择播放器：软解（VLC）或者硬解（MP），后续可以通过设置选项让用户来选择
	 * 
	 * @param uri
	 * @param forceVlc
	 */
	protected void selectMediaPlayer(String uri, boolean forceVlc) {
		/* TODO: do this through configuration */
		boolean useDefault = true;
		// int indexOfDot = uri.lastIndexOf('.');
		// if (indexOfDot != -1) {
		// String extension = uri.substring(indexOfDot).toLowerCase();
		// /* used for mms network radio */
		// boolean mms_radio_flag = uri.contains("mms://");
		// boolean http_live_flag = uri.contains("http://");
		// if (extension.compareTo(".flv") == 0
		// || extension.compareTo(".hlv") == 0
		// || extension.compareTo(".m3u8") == 0
		// || extension.compareTo(".mkv") == 0
		// || extension.compareTo(".rm") == 0
		// || extension.compareTo(".rmvb") == 0
		// || extension.compareTo(".ts") == 0
		// || mms_radio_flag
		// || http_live_flag) {
		// useDefault = false;
		// }
		// }

		if (forceVlc) {
			useDefault = false;
		}
		mSurfaceViewDef.setVisibility(useDefault ? View.VISIBLE : View.GONE);
		mSurfaceViewVlc.setVisibility(useDefault ? View.GONE : View.VISIBLE);
	}

	/**
	 * 创建MP
	 * 
	 * @param useDefault
	 * @param uri
	 * @param holder
	 */
	protected void createMediaPlayer(boolean useDefault, String uri,
			SurfaceHolder holder) {
		Log.d(LOGTAG, "createMediaPlayer() " + uri);
		/* */
		resetMediaPlayer();
		/* */
		mMediaPlayer = AbsMediaPlayer.getMediaPlayer(useDefault);
		mMediaPlayer.setOnBufferingUpdateListener(this);
		mMediaPlayer.setOnCompletionListener(this);
		mMediaPlayer.setOnErrorListener(this);
		mMediaPlayer.setOnInfoListener(this);
		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.setOnProgressUpdateListener(this);
		mMediaPlayer.setOnVideoSizeChangedListener(this);
		mMediaPlayer.reset();
		mMediaPlayer.setDisplay(holder);
		if (mMediaPlayer.setDataSource(uri) == false) {
			/* 隐藏缓冲圈 */
			mProgressBarPreparing.setVisibility(View.GONE);
			// FIXME bug#0023
			mPercentTxt.setVisibility(View.GONE);
			// mLoadingTxt.setVisibility(View.GONE);
			/* TODO 用在硬解解码模式，判断不支持的源 */
			new AlertDialog.Builder(PlayerActivity.this)
					.setIcon(R.drawable.ic_dialog_alert)
					.setTitle("播放失败【硬解码】")
					.setMessage("很遗憾，您的硬件解码器无法播放该视频\n请切换至【软解码】再次尝试\n现在切换解码模式吗？")
					.setPositiveButton("是",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing - it will close on
									// its own
									Intent intent = new Intent();
									// 跳转至设置界面
									intent.setClass(PlayerActivity.this,
											SetupActivity.class);
									startActivity(intent);
									finish();
								}
							})
					.setNegativeButton("否",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing - it will close on its own
									// 关闭当前的PlayerActivity，退回listview的界面
									finish();
								}
							}).show();
		}
		mMediaPlayer.prepareAsync();
	}

	/**
	 * 销毁MP
	 * 
	 * @param isDefault
	 */
	protected void destroyMediaPlayer(boolean isDefault) {
		// FIXME 2013-07-02
		if (mMediaPlayer != null) {
			boolean testDefault = isDefMediaPlayer(mMediaPlayer);
			// add by juguofeng 2013-06-23
			mMediaPlayerStarted = false;
			// end add
			if (isDefault == testDefault) {
				Log.i(LOGTAG, "destroyMediaPlayer");
				mMediaPlayer.setDisplay(null);
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
		}
	}

	/**
	 * 启动播放器
	 */
	protected void startMediaPlayer() {
		// FIXME bug#0023 rtmp的视频可能不走这里，但是如何开始播放的呢？
//		 Log.i(LOGTAG, "startMediaPlayer() ");
		if (mMediaPlayerStarted || !mMediaPlayerLoaded) {
			// Log.i(LOGTAG,
			// "(mMediaPlayerStarted || !mMediaPlayerLoaded) return");
			return;
		}
		if (mMediaPlayer != null) {
			 Log.i(LOGTAG, "===> mMediaPlayer.start()");
			mMediaPlayer.start();
			mMediaPlayerStarted = true;
		}
	}

	/**
	 * TODO 处理surface的界面比例
	 * 
	 * @param player
	 * @param surface
	 * @param ar
	 */
	protected void changeSurfaceSize(AbsMediaPlayer player,
			SurfaceView surface, int ar) {
		int videoWidth = player.getVideoWidth();
		int videoHeight = player.getVideoHeight();
		if (videoWidth <= 0 || videoHeight <= 0) {
			return;
		}
		SurfaceHolder holder = surface.getHolder();
		holder.setFixedSize(videoWidth, videoHeight);
		int displayWidth = getWindowManager().getDefaultDisplay().getWidth();
		int displayHeight = getWindowManager().getDefaultDisplay().getHeight();
		int targetWidth = -1;
		int targetHeight = -1;
		switch (ar) {
//		case SURFACE_NONE: {
//			targetWidth = videoWidth;
//			targetHeight = videoHeight;
//			break;
//		}
		case SURFACE_FILL: {
			break;
		}
		case SURFACE_ORIG: {
			displayWidth = videoWidth;
			displayHeight = videoHeight;
			break;
		}
		case SURFACE_4_3: {
			targetWidth = 4;
			targetHeight = 3;
			break;
		}
		case SURFACE_16_9: {
			targetWidth = 16;
			targetHeight = 9;
			break;
		}
		case SURFACE_16_10: {
			targetWidth = 16;
			targetHeight = 10;
			break;
		}
		default:
			break;
		}
		if (targetWidth > 0 && targetHeight > 0) {
			double ard = (double) displayWidth / (double) displayHeight;
			double art = (double) targetWidth / (double) targetHeight;
			if (ard > art) {
				displayWidth = displayHeight * targetWidth / targetHeight;
			} else {
				displayHeight = displayWidth * targetHeight / targetWidth;
			}
		}
		LayoutParams lp = surface.getLayoutParams();
		lp.width = displayWidth;
		lp.height = displayHeight;
		surface.setLayoutParams(lp);
		surface.invalidate();
	}

	/**
	 * 入口方法
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 播放事件初始化
		initializeEvents();
		initializeEvents2();
		initCustomToast();
		// 加载布局
		setContentView(R.layout.player);
		// 播放控件初始化
		initializeControls();
		// 缓冲环显示
		mProgressBarPreparing.setVisibility(View.VISIBLE);
		// 缓冲提示语
		// mLoadingTxt.setVisibility(View.VISIBLE);
		// 数据初始化
		initializeData();
		String uri = mPlayListArray.get(mPlayListSelected);

		/* 频道收藏的数据库 */
		mDbHelper = new DbHelper<POChannelList>();
		mSelfDbHelper = new DbHelper<POUserDefChannel>();

		// 选择播放器
		sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
		editor = sharedPreferences.edit();
		
		// 2013-10-24 是否显示播放进度条
		bShowSeekbar = sharedPreferences.getBoolean("isSeekbarShow", false);
		if (bShowSeekbar)
			seekbar_overlay.setVisibility(View.VISIBLE);
		
		// 获取设置的画面默认比例
		mAspectRatio = sharedPreferences.getInt("viewTaion", 0);
		
		/* 判断解码器状态 */
		isHardDec = sharedPreferences.getBoolean("isHardDec", false);
		if (isHardDec) {
			// 选择系统硬解码
			selectMediaPlayer(uri, false);
			// 应用运行时，保持屏幕高亮，不锁屏
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			// 强制选择VLC播放器
			selectMediaPlayer(uri, true);
		}
		
		no_player_help1 = sharedPreferences.getBoolean("no_player_help1", false);
		no_player_help2 = sharedPreferences.getBoolean("no_player_help2", false);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 注销电量检测事件
		unregisterReceiver(mReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mMediaPlayer != null) {
			mMediaPlayer.pause();
		}
	}

	/**
	 * 对上述控件的子控件的操作响应
	 */
	@Override
	public void onClick(View v) {
		// FIXME 由于rtmp的视频没有 MEDIA_PLAYER_PREPARED message
		// 所以点击不会出现播放控件界面
		// TODO 2013-09-06 增加屏幕切台功能之后，不再限制控件的点击行为
//		if (!mMediaPlayerLoaded)
//			return;

		// 如果有click事件，也阻止控件隐藏
		mDoHandleAll = false;
		mDoHandleClick = true;
		mDoHandleSeek = false;
		endCTLGesture(MSG_CTL_CLICK);

		int id = v.getId();
		switch (id) {
		case R.id.player_button_star: {
			// TODO 2013-09-28 加上了播放界面的分类切台之后，收藏的
			// 逻辑需要修改
			if (curPlaySort == 0) {
			// TODO 决定是否收藏该频道
//			if (isSelfTV || isSelfFavTV) {
				// 用户自定义的频道
				// TODO 2013-08-01 暂时不支持自定义的频道在播放界面收藏
				// updateSelfFavDatabase(mTitleName);
			} else {
				// 官方频道
				updateFavDatabase(mTitleName);
			}
			break;
		}
		case R.id.player_button_switch_audio: {
			// TODO 暂不做处理
			break;
		}
		case R.id.player_button_switch_subtitle: {
			// TODO 暂不做处理
			break;
		}
		case R.id.player_button_previous: {
			// TODO 暂不做处理
			break;
		}
		case R.id.player_button_toggle_play: {
			boolean playing = false;
			if (mMediaPlayer != null)
				playing = mMediaPlayer.isPlaying();
			if (playing) {
				if (mMediaPlayer != null)
					mMediaPlayer.pause();
			} else {
				if (mMediaPlayer != null)
					mMediaPlayer.start();
			}
			String name = String.format("btn_play_%d", !playing ? 1 : 0);
			int resouce = SystemUtility.getDrawableId(name);
			mImageButtonTogglePlay.setBackgroundResource(resouce);
			break;
		}
		case R.id.player_button_next: {
			// TODO 暂不做处理
			break;
		}
		case R.id.player_button_switch_aspect_ratio: {
			mAspectRatio = (mAspectRatio + 1) % SURFACE_MAX;
			if (mMediaPlayer != null)
				changeSurfaceSize(mMediaPlayer,
						isDefMediaPlayer(mMediaPlayer) ? mSurfaceViewDef
								: mSurfaceViewVlc, mAspectRatio);
			// TODO 2013-10-06 暂时去掉这部分代码，改为信息提醒
//			String name = String.format("btn_aspect_ratio_%d", mAspectRatio);
//			int resource = SystemUtility.getDrawableId(name);
//			mImageButtonSwitchAspectRatio.setBackgroundResource(resource);
			
			showCustomToast(ratioView[mAspectRatio]);
			break;
		}
		case R.id.player_button_list: {
			// TODO 增加播放界面切源和切台
			mLinearLayoutSourceList.setVisibility(View.VISIBLE);
			// 同时隐藏播放的控件
			mLinearLayoutControlBar.setVisibility(View.GONE);

			createList(mPlayListArray);
			Log.d(LOGTAG, "total items: " + source_list.getCount());

			break;
		}
		case R.id.player_button_channel: {
			if (mFirstLoad) {
				mFirstLoad = false;
	//			if (isFavSort || isSelfTV ||isSelfFavTV )
	//				mSortName.setText(sortString);
	//			else
					mSortName.setText(chSorts[curChSortIndex]);
				// TODO 增加播放界面切源和切台
				mLinearLayoutChannelList.setVisibility(View.VISIBLE);
				// 同时隐藏播放的控件
				mLinearLayoutControlBar.setVisibility(View.GONE);
	
				// 先显示加载提示语
				channel_list.setVisibility(View.GONE);
				epdListView.setVisibility(View.GONE);
				list_load.setVisibility(View.VISIBLE);
				
				Log.d(LOGTAG, "=============");
				
				// 暂时先清除之前的数据
				if (userdef_infos != null) {
					userdef_infos.clear();
					userdef_infos = null;
				}
				
				// TODO 清除之前的数据（自定义的分类）
				if (groupArray == null)
					groupArray = new ArrayList<String>();
				else
					groupArray.clear();
				if (childArray == null)
					childArray = new ArrayList<List<ChannelInfo>>();
				else
					childArray.clear();
				
				// TODO 清除之前的数据（官方地方台的分类）
				// FIXME 为提高之后的加载速度，就暂时不清除数据了
				if (groupArrayDF == null)
					groupArrayDF = new ArrayList<ProvinceInfo>();
	//			else
	//				groupArrayDF.clear();
				if (childArrayDF == null)
					childArrayDF = new ArrayList<List<POChannelList>>();
	//			else
	//				childArrayDF.clear();
				
				// 清除之前的数据
				if (channel_infos != null) {
					channel_infos.clear();
					channel_infos = null;
				}
				
				// TODO 采用线程的方式加载切换的分类节目
				startRefreshList();
			} else {
				// TODO 增加播放界面切源和切台
				mLinearLayoutChannelList.setVisibility(View.VISIBLE);
				// 同时隐藏播放的控件
				mLinearLayoutControlBar.setVisibility(View.GONE);
			}
			break;
		}
		case R.id.sort_name: {
			// 提醒用户点击左右两边的箭头
			Toast.makeText(PlayerActivity.this, "请点击两边的左右箭头切换分类", Toast.LENGTH_SHORT).show();
			break;
		}
		case R.id.sort_right: {
			// 控制切换的速度
			if (isListLoading)
				Toast.makeText(PlayerActivity.this, "请等待加载完毕",
	                    Toast.LENGTH_SHORT).show();
			else {
				curChSortIndex++;
				if ((curChSortIndex + 1) > chSortNum) {
					curChSortIndex = 0;
				}
				sortNameChange();
			}
			break;
		}
		case R.id.sort_left: {
			// 控制切换的速度
			if (isListLoading)
				Toast.makeText(PlayerActivity.this, "请等待加载完毕",
	                    Toast.LENGTH_SHORT).show();
			else {
				curChSortIndex--;
				if (curChSortIndex < 0) {
					curChSortIndex = chSortNum - 1;
				}
				sortNameChange();
			}
			break;
		}
		default:
			break;
		}
	}

	/**
	 * 播放界面的分类切换的公共代码
	 */
	private void sortNameChange() {
		isListLoading = true;
		
		// 先显示加载提示语
		channel_list.setVisibility(View.GONE);
		epdListView.setVisibility(View.GONE);
		list_load.setVisibility(View.VISIBLE);
		
		Log.d(LOGTAG, "=============");
		
		// 根据curChSortIndex大小，判断后面三个分类
		if (curChSortIndex == 6) {
			isFavSort = true;
			isSelfTV = false;
			isSelfFavTV = false;
		} else if  (curChSortIndex == 7) {
			isSelfTV = true;
			isFavSort = false;
			isSelfFavTV = false;
		} else if (curChSortIndex == 8) {
			isSelfFavTV = true;
			isFavSort = false;
			isSelfTV = false;
		} else {
			isFavSort = false;
			isSelfTV = false;
			isSelfFavTV = false;
		}
		
		// 暂时先清除之前的数据
		if (userdef_infos != null) {
			userdef_infos.clear();
			userdef_infos = null;
		}
//		if (userload_infos != null) {
//			userload_infos.clear();
//			userload_infos = null;
//		}
		// 清除之前的数据
		if (groupArray == null)
			groupArray = new ArrayList<String>();
		else
			groupArray.clear();
		if (childArray == null)
			childArray = new ArrayList<List<ChannelInfo>>();
		else
			childArray.clear();
		
		// TODO 清除之前的数据（官方地方台的分类）
		// FIXME 为提高之后的加载速度，就暂时不清除数据了
		if (groupArrayDF == null)
			groupArrayDF = new ArrayList<ProvinceInfo>();
//		else
//			groupArrayDF.clear();
		if (childArrayDF == null)
			childArrayDF = new ArrayList<List<POChannelList>>();
//		else
//			childArrayDF.clear();
		
		// 清除之前的数据
		if (channel_infos != null) {
			channel_infos.clear();
			channel_infos = null;
		}
		
		// TODO 采用线程的方式加载切换的分类节目
		startRefreshList();

		mSortName.setText(chSorts[curChSortIndex]);
	}
	
	/**
	 * seekbar的响应方法
	 * 
	 * @{
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		/* not used */
		// Log.v(LOGTAG, "-----Progress-----");
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		/* not used */
		// Log.v(LOGTAG, "-----start seek---------");
		mDoHandleAll = false;
		mDoHandleClick = false;
		mDoHandleSeek = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (!mMediaPlayerLoaded)
			return;
		int id = seekBar.getId();
		switch (id) {
		case R.id.player_seekbar_progress: {
			if (mCanSeek && mLength > 0) {
				int position = seekBar.getProgress();
				if (mMediaPlayer != null)
					mMediaPlayer.seekTo(position);
				// Log.v(LOGTAG, "-------seek end--------");
				/* seek结束了，可做控件隐藏的相应处理 */
				endCTLGesture(MSG_CTL_SEEKBAR);
			}
			break;
		}
		default:
			break;
		}
	}

	/** @} */

	/**
	 * 以下：接收事件，做中间处理，再调用handleMessage方法处理之
	 * 
	 * @{
	 */
	@Override
	public void onBufferingUpdate(AbsMediaPlayer mp, int percent) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_BUFFERING_UPDATE;
		msg.arg1 = percent;
		mEventHandler.sendMessage(msg);
	}

	@Override
	public void onCompletion(AbsMediaPlayer mp) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_COMPLETION;
		mEventHandler.sendMessage(msg);
	}

	@Override
	public boolean onError(AbsMediaPlayer mp, int what, int extra) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_ERROR;
		msg.arg1 = what;
		msg.arg2 = extra;
		mEventHandler.sendMessage(msg);
		return true;
	}

	@Override
	public boolean onInfo(AbsMediaPlayer mp, int what, int extra) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_INFO;
		msg.arg1 = what;
		msg.arg2 = extra;
		mEventHandler.sendMessage(msg);
		return true;
	}

	@Override
	public void onPrepared(AbsMediaPlayer mp) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_PREPARED;
		mEventHandler.sendMessage(msg);
	}

	@Override
	public void onProgressUpdate(AbsMediaPlayer mp, int time, int length) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_PROGRESS_UPDATE;
		msg.arg1 = time;
		msg.arg2 = length;
		mEventHandler.sendMessage(msg);
	}

	@Override
	public void onVideoSizeChangedListener(AbsMediaPlayer mp, int width,
			int height) {
		Message msg = new Message();
		msg.obj = mp;
		msg.what = MEDIA_PLAYER_VIDEO_SIZE_CHANGED;
		msg.arg1 = width;
		msg.arg2 = height;
		mEventHandler.sendMessage(msg);
	}

	/** @} */

	/**
	 * 初始化手势控制
	 */
	protected void initGesture() {
		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		mGestureDetector = new GestureDetector(this, new MyGestureListener());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* 首先处理touch事件（因为废弃了onTouch事件了） */
		// TODO 2013-09-06 增加屏幕切台功能之后，不再限制控件的点击行为
//		if (!mMediaPlayerLoaded) {
//			return true;
//		}

		// TODO 2013-10-25 计算时间间隔，以便更新节目预告
		if (mPrograPath != null) {
			if ((new Date().getTime() - mTimeLast) / 1000 > DIFFTIME) {
				if (mProgramtask != null && mProgramtask.getStatus() != AsyncTask.Status.FINISHED) {
                	// TODO 可能不是很严谨
                	mProgramtask.cancel(true);
                }
	                mProgramtask = new ProgramTask(programText);
					mProgramtask.execute(mPrograPath);
					// 记录这一次获取节目预告的时间
					mTimeLast = new Date().getTime();
			}
		}
		
		// ==================================
		// 显示播放界面的帮助信息
			if (no_player_help2 == false) {
				no_player_help2_num++;
				if (no_player_help2_num <= 1)
					new AlertDialog.Builder(PlayerActivity.this)
							.setIcon(R.drawable.ic_dialog_alert)
							.setTitle("频道和节目源切换")
							.setMessage(
									"点击右下角按钮可以切换频道\n\n点击左下角按钮可以切换节目源")
							.setPositiveButton("不再提醒",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,
												int which) {
											// 不再收藏
											no_player_help2 = true;
											editor.putBoolean("no_player_help2", true);
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
		// ==================================
		
		// 2013-08-31 隐藏源切换和切台的控件
		mLinearLayoutSourceList.setVisibility(View.GONE);
		mLinearLayoutChannelList.setVisibility(View.GONE);
		
		// TODO 更新当前时间信息
		mSysTime.setText(DateFormat.format("kk:mm", System.currentTimeMillis()));
		mTitle.setText(mTitleName);
		mSource.setText(mSourceName);
		mCodecMode.setText(isHardDec ? "[硬解码]" : "[软解码]");

		// 仅在触摸按下时，响应触摸事件
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int visibility = mLinearLayoutControlBar.getVisibility();
			// 加上判断之后可以在连续触摸的时候，到达其延时后仍可隐藏
			if (visibility != View.VISIBLE) {
				mLinearLayoutControlBar.setVisibility(View.VISIBLE);
				// 延时一段时间后隐藏
				mDoHandleAll = true;
				mDoHandleClick = false;
				mDoHandleSeek = false;
				endCTLGesture(MSG_CTL_ALL);
			} else {
				mDoHandleAll = false;
				mDoHandleClick = false;
				mDoHandleSeek = false;
				mLinearLayoutControlBar.setVisibility(View.GONE);
			}
		}

		// 处理音量和亮度调节手势事件
		if (mGestureDetector.onTouchEvent(event))
			return true;

		// 处理手势结束
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			endALGesture(); // 结束音量和亮度调节手势
			break;
		}

		return super.onTouchEvent(event);
	}

	/** 结束音量和亮度调节手势 */
	private void endALGesture() {
		mVolume = -1;
		mBrightness = -1f;

		// 隐藏
		mDismissALHandler.removeMessages(0);
		mDismissALHandler.sendEmptyMessageDelayed(0, 500);
	}

	/** 结束控制接口触摸 */
	private void endCTLGesture(int msg) {
		// 隐藏
		mDismissCTLHandler.removeMessages(msg);
		mDismissCTLHandler.sendEmptyMessageDelayed(msg, 5000);
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		/** TODO 双击（改变分辨率） */
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// if (mLayout == VideoView.VIDEO_LAYOUT_ZOOM)
			// mLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
			// else
			// mLayout++;
			// if (mVideoView != null)
			// mVideoView.setVideoLayout(mLayout, 0);
			return true;
		}

		/** 滑动 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// 异常处理
			if (e1 == null || e2 == null) {
				Log.e(LOGTAG, "get MotionEvent value null");
				return true;
			}

			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();

			if (mOldX > windowWidth * 4.0 / 5)// 右边滑动
				onVolumeSlide((mOldY - y) / windowHeight);
			else if (mOldX < windowWidth / 5.0)// 左边滑动
				onBrightnessSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	/** 定时隐藏音量和亮度图标 */
	private Handler mDismissALHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mVolumeBrightnessLayout.setVisibility(View.GONE);
		}
	};

	/** 定时隐藏播放控件 */
	private Handler mDismissCTLHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Log.v(LOGTAG, "-----msg.what------" + msg.what);
			switch (msg.what) {
			case MSG_CTL_ALL:
				if (mDoHandleAll) {
					mLinearLayoutControlBar.setVisibility(View.GONE);
					mDoHandleAll = false;
				}
				break;
			case MSG_CTL_CLICK:
				if (mDoHandleClick) {
					mLinearLayoutControlBar.setVisibility(View.GONE);
					mDoHandleClick = false;
				}
				break;
			case MSG_CTL_SEEKBAR:
				if (mDoHandleSeek) {
					mLinearLayoutControlBar.setVisibility(View.GONE);
					mDoHandleSeek = false;
				}
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 滑动改变声音大小
	 * 
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;

			// 显示
			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		// 变更进度条
		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width
				* index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	/**
	 * 滑动改变亮度
	 * 
	 * @param percent
	 */
	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;

			// 显示
			mOperationBg.setImageResource(R.drawable.video_brightness_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}
		WindowManager.LayoutParams lpa = getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		getWindow().setAttributes(lpa);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
		mOperationPercent.setLayoutParams(lp);
	}

	// 电池电量检测
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
				int batteryLevel = intent.getIntExtra("level", 0);
				// Log.v(LOGTAG, "---->get batteryLevel = " + batteryLevel);
				if (batteryLevel >= 50)
					mBattery.setTextColor(Color.GREEN);
				else if (batteryLevel >= 30)
					mBattery.setTextColor(Color.YELLOW);
				else
					mBattery.setTextColor(Color.RED);
				mBattery.setText(String.format("%d%%", batteryLevel));
			}
			// else if (action.equalsIgnoreCase(VLCApplication.SLEEP_INTENT)) {
			// finish();
			// }
		}
	};

	/**
	 * 菜单、返回键响应
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exitBy2Click(); // 调用双击退出函数
		}
		return false;
	}

	/**
	 * 双击退出函数
	 */
	private static Boolean isExit = false;

	private void exitBy2Click() {
		Timer tExit = null;
		if (isExit == false) {
			isExit = true; // 准备退出
			Toast.makeText(this, "再按一次退出播放", Toast.LENGTH_SHORT).show();
			tExit = new Timer();
			TimerTask mTimerTask = new TimerTask() {
				@Override
				public void run() {
					isExit = false; // 取消退出
				}
			};
			tExit.schedule(mTimerTask, 3000); // 如果3秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务
		} else {
			finish();
		}
	}

	/**
	 * 收藏后更新某一条数据信息
	 * 
	 */
	private void updateFavDatabase(String name) {
		int resource = -1;

		List<POChannelList> channelList = mDbHelper.queryForEq(
				POChannelList.class, "name", name);
		for (POChannelList channel : channelList) {
			if (channel.save) {
				channel.save = false;
				resource = SystemUtility.getDrawableId("ic_fav");
				mImageButtonStar.setBackgroundResource(resource);

				Toast.makeText(getApplicationContext(), "取消收藏",
						Toast.LENGTH_SHORT).show();
			} else {
				channel.save = true;
				resource = SystemUtility.getDrawableId("ic_fav_pressed");
				mImageButtonStar.setBackgroundResource(resource);

				Toast.makeText(getApplicationContext(), "添加收藏",
						Toast.LENGTH_SHORT).show();
			}
			// update
			Log.i(LOGTAG, "==============>" + channel.name + "###"
					+ channel.poId + "###" + channel.save);

			mDbHelper.update(channel);
		}
	}

	// /**
	// * 自定义收藏后更新某一条数据信息
	// *
	// */
	// private void updateSelfFavDatabase(String name) {
	// int resource = -1;
	//
	// List<POUserDefChannel> channelList = mSelfDbHelper.queryForEq(
	// POUserDefChannel.class, "name", name);
	// for (POUserDefChannel channel : channelList) {
	// if (channel.save) {
	// channel.save = false;
	// resource = SystemUtility.getDrawableId("ic_fav");
	// mImageButtonStar.setBackgroundResource(resource);
	//
	// Toast.makeText(getApplicationContext(), "取消收藏",
	// Toast.LENGTH_SHORT).show();
	// } else {
	// channel.save = true;
	// resource = SystemUtility.getDrawableId("ic_fav_pressed");
	// mImageButtonStar.setBackgroundResource(resource);
	//
	// Toast.makeText(getApplicationContext(), "添加收藏",
	// Toast.LENGTH_SHORT).show();
	// }
	// // update
	// Log.i(LOGTAG, "==============>" + channel.name + "###"
	// + channel.poId + "###" + channel.save);
	//
	// mSelfDbHelper.update(channel);
	// }
	// }

	// =========================================================

	private ChannelSourceAdapter mSourceAdapter = null;
	/**
	 * 2013-08-31 增加播放界面切源功能
	 */
	private void createList(ArrayList<String> infos) {
		// FIXME 2013-10-26 为标记当前线路，将起改为全局的
//		ChannelSourceAdapter adapter = new ChannelSourceAdapter(this, mPlayListArray);
		mSourceAdapter = new ChannelSourceAdapter(this, mPlayListArray);
		
		// TODO 2013-10-26 标记当前正在播放的线路
		mSourceAdapter.mCurrentIndex = mSourceIndex;
		source_list.setAdapter(mSourceAdapter);
		// 突出显示当前频道
		if (mChannelIndex >= 0)
			source_list.setSelection(mSourceIndex);
		
		source_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				mSourceIndex = arg2;
				
				// TODO Auto-generated method stub
				String url = (String) source_list
						.getItemAtPosition(arg2);
				mSourceName = "线路" + Integer.toString(arg2 + 1) + "：" + SourceName.whichName(url);
				reSetSourceData(url, mSourceName);
//				Log.i(LOGTAG, "===>>>" + mSourceName);
				
				// TODO 2013-10-26 标记当前正在播放的线路
				mSourceAdapter.mCurrentIndex = arg2;
				mSourceAdapter.notifyDataSetChanged();
				
				// 2013-08-31 隐藏源切换和切台的控件
				// 2013-10-10 不再隐藏
//				mLinearLayoutSourceList.setVisibility(View.GONE);
			}
		});

		source_list.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
			}
		});
	}
	
	/**
	 * 自定义频道的数据，切台
	 * @param sort
	 */
	private void createUserloadChannelList() {
//		
//		ChannelLoadAdapter mSourceAdapter = new ChannelLoadAdapter(this, userload_infos, true);
//		channel_list.setAdapter(mSourceAdapter);
//		// 突出显示当前频道
//		channel_list.setSelection(mChannelIndex);
//		// TODO 用一个全局的变量来记录当前是哪一个频道
//		
//		// 设置监听事件
//		channel_list.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1,
//					int arg2, long arg3) {
//				
//				curPlaySort = 0;	// 当前播放的是自定义频道
//				
//				mChannelIndex = arg2;
//				
//				// TODO Auto-generated method stub
//				ChannelInfo info = (ChannelInfo) channel_list
//						.getItemAtPosition(arg2);
//
//				mTitleName = info.getName();
//				reSetUserdefChannelData(info);
//			}
//		});
//		// 增加长按频道收藏功能
//		channel_list.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//			@Override
//			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//					int arg2, long arg3) {
//				ChannelInfo info = (ChannelInfo) channel_list
//						.getItemAtPosition(arg2);
//				// 转换为数据库数据结构
//				POUserDefChannel POinfo = new POUserDefChannel(info, true);
//				showFavMsg(arg1, POinfo);
//				return true;
//			}
//		});
		// 实现自定义节目的分类
        
	    final CustomExpandableAdapter adapter = new CustomExpandableAdapter(this, groupArray, childArray, true);
        
		// TODO 2013-10-23 标记当前正在播放的频道
		if (curChSortIndex == curPlaySortIndex) {
			adapter.mCurGroup = mChannelGroup;
			adapter.mCurChild = mChannelIndex;
			epdListView.setAdapter(adapter);
			// 突出显示当前分类
			if (mChannelGroup >= 0) {
//			epdListView.setSelectedChild(mChannelGroup, mChannelIndex, true);
				epdListView.setSelection(mChannelGroup);
				epdListView.expandGroup(mChannelGroup);
			}
		} else {
			adapter.mCurGroup = -1;
			adapter.mCurChild = -1;
			epdListView.setAdapter(adapter);
		}
	    
        //设置item点击的监听器
	    epdListView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {

				curPlaySort = 0;	// 当前播放的是自定义频道
				
				mChannelIndex = childPosition;
				mChannelGroup = groupPosition;
				
				// 点击播放之后，当前切换的频道分类必然就是当前播放的频道分类
				curPlaySortIndex = curChSortIndex;
				
                ChannelInfo info = (ChannelInfo)adapter.getChild(groupPosition, childPosition);
                
                mTitleName = info.getName();
                reSetUserdefChannelData(info);
                
				// TODO 2013-10-23 标记当前正在播放的频道
				adapter.mCurGroup = groupPosition;
				adapter.mCurChild = childPosition;
				adapter.notifyDataSetChanged();
                
                // 2013-10-15 显示节目预告
                // 置空节目预告控件
                programText.setText("");
                
                mPrograPath = null;
                mTimeLast = new Date().getTime();
                
                return false;
            }
        });
	    
		// 2013-11-05 长按收藏自定义频道
	    epdListView.setOnItemLongClickListener(new OnItemLongClickListener() {
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
	}
	
	/**
	 * 自定义收藏频道的数据，切台
	 * @param sort
	 */
	private void createUserdefChannelList() {
		
		final ChannelDefFavAdapter adapter = new ChannelDefFavAdapter(this, userdef_infos, true);

		// TODO 2013-10-23 标记当前正在播放的频道
		if (curChSortIndex == curPlaySortIndex) {
			adapter.mCurrentIndex = mChannelIndex;
			channel_list.setAdapter(adapter);
			// 突出显示当前频道
			if (mChannelIndex >= 0)
				channel_list.setSelection(mChannelIndex);
		} else {
			adapter.mCurrentIndex = -1;
			channel_list.setAdapter(adapter);
		}
		
		channel_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				curPlaySort = 0;	// 当前播放的是自定义频道
				
				mChannelIndex = arg2;
				
				// 点击播放之后，当前切换的频道分类必然就是当前播放的频道分类
				curPlaySortIndex = curChSortIndex;
				
				// TODO Auto-generated method stub
				POUserDefChannel info = (POUserDefChannel) channel_list
						.getItemAtPosition(arg2);
				
				mTitleName = info.name;
				reSetUserdefFavChannelData(info);
//				Log.i(LOGTAG, "===>>>" + mSourceName);

				// TODO 2013-10-26 标记当前正在播放的频道
				adapter.mCurrentIndex = arg2;
				adapter.notifyDataSetChanged();
				
				// 2013-08-31 隐藏源切换和切台的控件
				// 为防止重复搜索数据库分类，此处暂时不隐藏
//				mLinearLayoutChannelList.setVisibility(View.GONE);
				// 2013-10-15 自定义节目没有节目预告
				programText.setText("");
				
				mPrograPath = null;
				mTimeLast = new Date().getTime();
			}
		});

		source_list.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
			}
		});
	}
	
	/**
	 * 官方频道的地方台分类数据，切台
	 * @param sort
	 */
	private void createChannelListDF() {
        
	    final CustomExpandableAdapterDF adapter = new CustomExpandableAdapterDF(this, groupArrayDF, childArrayDF);
	    
		// TODO 2013-10-23 标记当前正在播放的频道
		if (curChSortIndex == curPlaySortIndex) {
			adapter.mCurGroup = mChannelGroup;
			adapter.mCurChild = mChannelIndex;
			epdListView.setAdapter(adapter);
			// 突出显示当前分类
			if (mChannelGroup >= 0) {
//				epdListView.setSelectedChild(mChannelGroup, mChannelIndex, true);
				epdListView.setSelection(mChannelGroup);
				epdListView.expandGroup(mChannelGroup);
			}
		} else {
			adapter.mCurGroup = -1;
			adapter.mCurChild = -1;
			epdListView.setAdapter(adapter);
		}

        //设置item点击的监听器
	    epdListView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {

				curPlaySort = 1;	// 当前播放的是官方频道
				
				mChannelIndex = childPosition;
				mChannelGroup = groupPosition;
            	
				// 点击播放之后，当前切换的频道分类必然就是当前播放的频道分类
				curPlaySortIndex = curChSortIndex;
				
				POChannelList info = (POChannelList)adapter.getChild(groupPosition, childPosition);
                
                mTitleName = info.name;
                reSetChannelData(info);
                
				// TODO 2013-10-23 标记当前正在播放的频道
				adapter.mCurGroup = groupPosition;
				adapter.mCurChild = childPosition;
				adapter.notifyDataSetChanged();
                
                // 2013-10-15 显示节目预告
                if (mProgramtask != null && mProgramtask.getStatus() != AsyncTask.Status.FINISHED) {
                	// TODO 可能不是很严谨
                	mProgramtask.cancel(true);
                }
                if (info.program_path != null) {
	                mProgramtask = new ProgramTask(programText);
					mProgramtask.execute(info.program_path);
                } else {
					programText.setText("");
				}
				mPrograPath = info.program_path;
				mTimeLast = new Date().getTime();
                
                return false;
            }
        });
	}
	
	/**
	 * 官方(收藏)频道的数据，切台
	 * @param sort
	 */
	private void createChannelList() {
		
		final ChannelListAdapter adapter = new ChannelListAdapter(this, channel_infos);
		
		// TODO 2013-10-23 标记当前正在播放的频道
		if (curChSortIndex == curPlaySortIndex) {
			adapter.mCurrentIndex = mChannelIndex;
			channel_list.setAdapter(adapter);
			// 突出显示当前频道
			if (mChannelIndex >= 0)
				channel_list.setSelection(mChannelIndex);
		} else {
			adapter.mCurrentIndex = -1;
			channel_list.setAdapter(adapter);
		}
		
		channel_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				curPlaySort = 1;	// 当前播放的是官方频道
				
				mChannelIndex = arg2;
				
				// 点击播放之后，当前切换的频道分类必然就是当前播放的频道分类
				curPlaySortIndex = curChSortIndex;
				
				// TODO Auto-generated method stub
				POChannelList info = (POChannelList) channel_list
						.getItemAtPosition(arg2);
				
				mTitleName = info.name;
				reSetChannelData(info);
				Log.i(LOGTAG, "===>>>" + mSourceName);

				// TODO 2013-10-22 标记当前正在播放的频道
				adapter.mCurrentIndex = arg2;
				adapter.notifyDataSetChanged();
				
				// 2013-10-15 显示节目预告
				if (mProgramtask != null && mProgramtask.getStatus() != AsyncTask.Status.FINISHED) {
                	// TODO 可能不是很严谨
                	mProgramtask.cancel(true);
                }
				if (info.program_path != null) {
					mProgramtask = new ProgramTask(programText);
					mProgramtask.execute(info.program_path);
				} else {
					programText.setText("");
				}
				
				mPrograPath = info.program_path;
				mTimeLast = new Date().getTime();
				
				// 2013-08-31 隐藏源切换和切台的控件
				// 为防止重复搜索数据库分类，此处暂时不隐藏
//				mLinearLayoutChannelList.setVisibility(View.GONE);
			}
		});

		source_list.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
			}
		});
	}
	
	/**
	 * 重新获取播放需要的视频数据
	 */
	private void reSetSourceData(String url, String name) {
		reConnectSource(url);
	}

	/**
	 * 重新连接候选的地址源
	 */
	private void reConnectSource(String url) {
//		isLiveMedia = sharedPreferences.getBoolean("isLiveMedia", true);
//		if (isLiveMedia) {
			// 缓冲环显示
			mProgressBarPreparing.setVisibility(View.VISIBLE);
			// 缓冲提示语
			// mLoadingTxt.setVisibility(View.VISIBLE);
			Log.d(LOGTAG, "reconnect the Media Server in LiveTV mode");
			if (sharedPreferences.getBoolean("isHardDec", false)) {
				// 硬解码重新连接媒体服务器
				destroyMediaPlayer(true);
				selectMediaPlayer(url, false);
				createMediaPlayer(true, url, mSurfaceHolderDef);
//				mMediaPlayer.setDisplay(mSurfaceHolderDef);
			} else {
				// 软解码重新连接媒体服务器
				destroyMediaPlayer(false);
				selectMediaPlayer(url, true);
				createMediaPlayer(false, url, mSurfaceHolderVlc);
//				mMediaPlayer.setDisplay(mSurfaceHolderVlc);
			}
		}
//	}
	
	/**
	 * 切换频道，重新设置相关数据
	 */
	private void reSetChannelData(POChannelList info) {
		// 先清除原有的
		mPlayListArray.clear();
		// 再加载新的数据
		mPlayListArray = info.getAllUrl();
		mSourceIndex = 0;
		mSourceNum = mPlayListArray.size();
		mPlayListSelected = 0;
		channelStar = info.save;
		
		String url = mPlayListArray.get(mPlayListSelected);
		mSourceName = "线路" + Integer.toString(1) + "：" + SourceName.whichName(url);
		reConnectSource(url);
	}
	
	/**
	 * 自定义收藏切换频道，重新设置相关数据
	 */
	private void reSetUserdefFavChannelData(POUserDefChannel info) {
		// 先清除原有的
		mPlayListArray.clear();
		// 再加载新的数据
		mPlayListArray = info.getAllUrl();
		mSourceIndex = 0;
		mSourceNum = mPlayListArray.size();
		mPlayListSelected = 0;
		
		String url = mPlayListArray.get(mPlayListSelected);
		mSourceName = "线路" + Integer.toString(1) + "：" + SourceName.whichName(url);
		reConnectSource(url);
	}
	
	/**
	 * 自定义切换频道，重新设置相关数据
	 */
	private void reSetUserdefChannelData(ChannelInfo info) {
		// 先清除原有的
		mPlayListArray.clear();
		// 再加载新的数据
		mPlayListArray = info.getAllUrl();
		mSourceIndex = 0;
		mSourceNum = mPlayListArray.size();
		mPlayListSelected = 0;
		
		String url = mPlayListArray.get(mPlayListSelected);
		mSourceName = "线路" + Integer.toString(1) + "：" + SourceName.whichName(url);
		reConnectSource(url);
	}
	
	/**
	 * 提示是否收藏为个性频道
	 */
	private void showFavMsg(View view, POUserDefChannel info) {

		final POUserDefChannel saveInfo = info;

			new AlertDialog.Builder(PlayerActivity.this)
					.setIcon(R.drawable.ic_dialog_alert)
					.setTitle("温馨提示")
					.setMessage("确定收藏该自定义频道吗？")
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO 增加加入数据库操作
							saveInfo.date = DateFormat.format("MM月dd日",
									System.currentTimeMillis()).toString();
							mSelfDbHelper.create(saveInfo);
						}
					})
					.setNegativeButton("取消", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).show();
		}
		
	//======================================================
	/**
	 * 2013-09-28 在线程中操作数据库，更安全
	 * 查询数据库的频道分类数据
	 */
	private void startRefreshList() {
		// 发送开始刷新的消息
//		onRefreshStart();

		Log.d(LOGTAG, "===> start refresh playlist");

		// 这里创建一个脱离UI主线程的线程负责网络下载
		new Thread() {
			public void run() {
				// 判断是自定义频道、收藏频道、官方频道等
				if (isSelfTV) {
					// 自定义加载频道
//					if (userload_infos == null) {
//						// 解析本地的自定义列表
//						String path = Environment.getExternalStorageDirectory().getPath()
//								+ "/kekePlayer/tvlist.txt";
//						userload_infos = ParseUtil.parseDef(path);
//					}
			        String path = Environment.getExternalStorageDirectory().getPath()
							+ "/kekePlayer/tvlist.txt";
					File listFile = new File(path);
					if (listFile.exists()) {
						parseDef(path);
					}
				} else if (isSelfFavTV) {
					// 如果是自定义频道，数据结构变了
					if (userdef_infos == null) {
						userdef_infos = ChannelListBusiness.getAllDefFavChannels();
					}
				} else {
					if (isFavSort)		// FIXME 2013-09-28 如果当前定位在收藏界面，取消了收藏频道，那么需要重新加载
						channel_infos = ChannelListBusiness.getAllFavChannels();
					// TODO 清除数据（是否可以只查询一次）
					else if (channel_infos == null) {
						// 根据JSON里面的types来区分直播频道分类
						
						// TODO 2013-09-29
						// 进一步区分官方的地方频道，也要进行分类
						if (curChSortIndex == 2) {
							// 先解析出所有的地方台的分类名称
							// FIXME 为提高之后的加载速度，就暂时不清除数据了
							if (isDFhasLoad == false) {
								groupArrayDF = ParseUtil.getProvinceNames(PlayerActivity.this);
								// 循环查找出所有的地方台分类
								parseDF();
							}
						} else {
						// end
							channel_infos = ChannelListBusiness.getAllSearchChannels("types", String.valueOf(curChSortIndex + 1));
						}
					}
				}
				onRefreshEnd();
			}
		}.start();
	}
		
	private Handler mEventHandler2;
	private static final int TV_LIST_REFRESH_START = 0x0001;
	private static final int TV_LIST_REFRESH_END = 0x0002;

	/**
	 * 地址刷新过程中的事件响应的核心处理方法
	 */
	private void initializeEvents2() {
		mEventHandler2 = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case TV_LIST_REFRESH_START:
					break;
				case TV_LIST_REFRESH_END:
					// 先显示加载提示语
					channel_list.setVisibility(View.VISIBLE);
					list_load.setVisibility(View.GONE);
					
					// 判断是自定义频道、收藏频道、官方频道等
					if (isSelfTV) {
						// 自定义加载频道
					    epdListView.setVisibility(View.VISIBLE);
					    channel_list.setVisibility(View.GONE);
					    
						createUserloadChannelList();
					} else if (isSelfFavTV) {
						/* 获取所有的自定义收藏频道 */
						createUserdefChannelList();
					} else {
						// TODO 2013-09-29 需要区分官方频道的地方台分类
						if (curChSortIndex == 2) {
						    epdListView.setVisibility(View.VISIBLE);
						    channel_list.setVisibility(View.GONE);
							createChannelListDF();
						} else {
							createChannelList();
						}
					}
					
					isListLoading = false;
					
					break;
				default:
					break;
				}
			}
		};
	}
	
	/**
	 * 以下：接收事件，做中间处理，再调用handleMessage方法处理之
	 * 
	 * @{
	 */
	private void onRefreshStart() {
		Message msg = new Message();
		msg.what = TV_LIST_REFRESH_START;
		mEventHandler2.sendMessage(msg);
	}

	private void onRefreshEnd() {
		Message msg = new Message();
		msg.what = TV_LIST_REFRESH_END;
		mEventHandler2.sendMessage(msg);
	}
	//======================================================
	
	// 重写自定义列表的屏幕切台，便于分类
	// ===============================
    private List<String> groupArray = null;
    private List<List<ChannelInfo>> childArray = null;
    
    ExpandableListView epdListView;  
	
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
 							
// 							Log.d("sort", "===>last size = " + list.size());
 							
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
// 								Log.i("sort", "===> sort name $$$ " + privSort);
 								privSort = sortName;
 								
// 								Log.i("sort", "===> sort name = " + privSort);
 								
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
 							
// 							Log.d("sort", "===>sort size = " + list.size());
 							
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
 // ===============================
 		
 	// 重写自定义列表的屏幕切台，便于分类
	// ===============================
    private List<ProvinceInfo> groupArrayDF = null;
    private List<List<POChannelList>> childArrayDF = null;
    private Boolean isDFhasLoad = false;
    
    //ExpandableListView epdListView;  
	
 	// 解析官方频道数据库中的地方台分类的列表
	// TODO 2013-09-29
	// 为了加强地方频道的分类功能，采用数据库暂存所有的自定义
	// 的数据，每次加载时，重新入数据库？或者查看自定义文件
	// 是否作出了修改，若修改了，则清除数据库数据，重新装载
    // #########
    // TODO 2013-10-16
    // 修改查找策略，提升地方台的加载速度
	private void parseDF() {
		
		// ============================================================
		// 方法一：效率较低
//		int sortNum = groupArrayDF.size();
//		int index = 0;
//		String sortName = null;
//		for (index = 0; index < sortNum; index++) {
//			sortName = groupArrayDF.get(index).getProvinceName();
//			List<POChannelList> listChannel = new ArrayList<POChannelList>();
//			
//			listChannel = ChannelListBusiness.getAllSearchChannels(
//					"province_name", sortName);
//			
//			childArrayDF.add(index, listChannel);
//			
//		}
		// ============================================================
		// 方法二：效率相对高一些
		List<POChannelList> listChannel = ChannelListBusiness.getAllOfficialChannels();
		ChannelInfoCache mChannelInfoCache = new ChannelInfoCache();
		
		for (ProvinceInfo info : groupArrayDF) {
			mChannelInfoCache.put(info.getProvinceName(), new ArrayList<POChannelList>());
		}
		
		List<POChannelList> tmpList;
		
		for (POChannelList info : listChannel) {
			tmpList = mChannelInfoCache.get(info.province_name);
			if (tmpList != null)
				tmpList.add(info);
		}
		
		for (ProvinceInfo info : groupArrayDF) {
			childArrayDF.add(mChannelInfoCache.get(info.getProvinceName()));
		}
		// ============================================================
		
		isDFhasLoad = true;
	}
 // ===============================
	public class ChannelInfoCache {
	    private HashMap<String, List<POChannelList>> cache=new HashMap<String, List<POChannelList>>();
	    
	    public List<POChannelList> get(String id){
	        if(!cache.containsKey(id))
	            return null;
	        return cache.get(id);
	    }
	    
	    public void put(String id, List<POChannelList> infos){
	        cache.put(id, infos);
	    }

	    public void clear() {
	        cache.clear();
	    }
	}
}
