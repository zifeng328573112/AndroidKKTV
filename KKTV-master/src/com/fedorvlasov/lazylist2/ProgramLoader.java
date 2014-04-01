package com.fedorvlasov.lazylist2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stagex.danmaku.adapter.ProgramInfo;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

public class ProgramLoader {

	private Context mContext;

	MemoryCache memoryCache = new MemoryCache();
	FileCache fileCache;
	private Map<TextView, String> textViews = Collections
			.synchronizedMap(new WeakHashMap<TextView, String>());

	public ProgramLoader(Context context) {
		// Make the background thead low priority. This way it will not affect
		// the UI performance
		programLoaderThread.setPriority(Thread.NORM_PRIORITY - 2);
		mContext = context;
		fileCache = new FileCache(context);
	}

	public void DisplayText(String url, Activity activity, TextView textView) {
		textViews.put(textView, url);
		// String program = memoryCache.get(url);
		// TODO 2013-10-18 为了区分每日的节目预告，url末尾加上日期
		// 该url作为Hash Memory的键值
		ArrayList<ProgramInfo> programInfo = memoryCache.get(url
				+ Utils.getWeekOfDate());
		if (programInfo != null) {
			// Log.d("===", "find exist============\n");
			String program = getCurrentProgram(programInfo);
			textView.setText(program);
		} else {
			queueProgram(url, activity, textView);
			textView.setText("......");
		}
	}

	private void queueProgram(String url, Activity activity, TextView textView) {
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		programsQueue.Clean(textView);
		ProgramToLoad p = new ProgramToLoad(url, textView);
		synchronized (programsQueue.programsToLoad) {
			programsQueue.programsToLoad.push(p);
			programsQueue.programsToLoad.notifyAll();
		}

		// start thread if it's not started yet
		if (programLoaderThread.getState() == Thread.State.NEW)
			programLoaderThread.start();
	}

    /** 
     * 读取修改时间的方法2 
     */  
    public static String getModifiedTime(File f){  
        Calendar cal = Calendar.getInstance();  
        long time = f.lastModified();  
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");         
        cal.setTimeInMillis(time);    
        
        return formatter.format(cal.getTime());     
        //输出：修改时间[2]    2009-08-17 10:32:38  
    }  
	
	
	private ArrayList<ProgramInfo> getProgram(String programPath) {

		// ================================================
		// TODO 2013-10-26 为了便于“删除”过旧的节目预告，更换策略，
		// URL+DATE缓存的内存，URL作为文件名保存文件。
		// 当请求一个节目预告时，首先根据URL+DATE判断内存中有没有，
		// 如果有，直接解析；如果没有，首先尝试在本地缓存的文件寻找，
		// 如果没有，则直接根据URL从网络获取；如果有的话，则比较当前
		// 日期与已有文件的修改日期是否一致，一致的话则解析文件，不一
		// 致的话，也会请求URL获取节目预告，并覆盖已有的文件
		// ================================================
		
//		File f = fileCache.getFile(programPath + Utils.getWeekOfDate());
		File f = fileCache.getFile(programPath);

		// 如果文件存在的话
		if (f.exists()) {
			// 判断文件的修改时间，如果与当前日期一致，说明已缓存
			// 不相等的话，就覆盖（FIXME 注意，今日最后一个节目到次日
			// 凌晨的第一个节目之间的bug，会后续修复）
			if (getModifiedTime(f).contains(DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()))) {
//				Log.d("", "============find=============");
				// from SD cache
				ArrayList<ProgramInfo> fileInfos = decodeFile(f);
				if (fileInfos != null)
					return fileInfos;
			}
		}
		/* ====================================================== */

		/* TODO 以listView文本方式显示节目预告 */
		Document doc = null;
		try {
			doc = Jsoup.connect(
					"http://www.tvmao.com/ext/show_tv.jsp?p=" + programPath)
					.get();

			Elements links = doc.select("li"); // 带有href属性的a元素

			ArrayList<ProgramInfo> infos = new ArrayList<ProgramInfo>();

			for (Element link : links) {
				String[] pair = link.text().split(" ");
				if (pair.length < 2)
					continue;
				infos.add(new ProgramInfo(pair[0].trim(), pair[1].trim(), false));
			}

			// TODO 2013-10-22 缓存到本地
			fileCache.saveFile(infos, f);

			return infos;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* ====================================================== */
		return null;
	}

	// decodes image and scales it to reduce memory consumption
	private ArrayList<ProgramInfo> decodeFile(File f) {

		ArrayList<ProgramInfo> infos = new ArrayList<ProgramInfo>();

		try {
			InputStream is = new FileInputStream(f);
			InputStreamReader ir = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(ir);
			try {
				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}

					// 如果不符合要求（节目名和节目地址以英文逗号隔开）直接忽略该行
					String[] pair = line.split(",");
					if (pair.length < 2)
						continue;
					infos.add(new ProgramInfo(pair[0].trim(), pair[1].trim(),
							false));
				}
			} finally {
				br.close();
				ir.close();
				is.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return null;
		}

		// end
		return infos;
	}

	/**
	 * 从ArrayList<ProgramInfo>中解析出当前时刻的节目
	 * 
	 * @return
	 */
	private String getCurrentProgram(ArrayList<ProgramInfo> infos) {
		Boolean findFlag = false;
		int listPosition = 0;

		Date fromDate = new Date();
		SimpleDateFormat simple1 = new SimpleDateFormat("kk:mm");

		// 当前时间
		String timeStr = DateFormat.format("kk:mm", System.currentTimeMillis())
				.toString();
		try {
			fromDate = simple1.parse(timeStr);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		long curTime = fromDate.getTime();

		for (ProgramInfo info : infos) {

			if (!findFlag) {
				listPosition++;
				try {
					fromDate = simple1.parse(info.getTime());
					/* 找到第一个比当前时间大的节目，而正在播放的实际是前一个节目 */
					if (fromDate.getTime() >= curTime) {
						findFlag = true;
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// 在listView中突出显示当前的播放节目
		if (!findFlag) {
			// FIXME bug#0022 有些节目预告有内容，但是不是真正的节目单，此时的失败是因为没有节目单
			if (infos.size() == 0) {
				return null;
			} else {
				// FIXME bug#0022 此处的没找到是因为有节目预告，但是处于24：00分左右的临界情况
				/* 如果没有大于当前时间值的节目，说明当日的最后一个节目就是当前播放的节目 */
				return infos.get(infos.size() - 1).getProgram();
			}
		} else if (listPosition == 1) {
			/* 如果第一个节目的时间指就大于当前时间，实际是前一天的最后一个节目，在新的一天什么都不显示 */
			return null;
		} else {
			/* 其他正常情况，如果找到一个大于当前时间值的节目，置前一个节目为正在播放节目 */
			return infos.get(listPosition - 2).getProgram();
		}
	}

	// Task for the queue
	private class ProgramToLoad {
		public String url;
		public TextView textView;

		public ProgramToLoad(String u, TextView i) {
			url = u;
			textView = i;
		}
	}

	ProgramsQueue programsQueue = new ProgramsQueue();

	public void stopThread() {
		programLoaderThread.interrupt();
	}

	// stores list of photos to download
	class ProgramsQueue {
		private Stack<ProgramToLoad> programsToLoad = new Stack<ProgramToLoad>();

		// removes all instances of this ImageView
		public void Clean(TextView textView) {
			for (int j = 0; j < programsToLoad.size();) {
				if (programsToLoad.get(j).textView == textView)
					programsToLoad.remove(j);
				else
					++j;
			}
		}
	}

	class ProgramsLoader extends Thread {
		public void run() {
			try {
				while (true) {
					// thread waits until there are any images to load in the
					// queue
					if (programsQueue.programsToLoad.size() == 0)
						synchronized (programsQueue.programsToLoad) {
							programsQueue.programsToLoad.wait();
						}
					if (programsQueue.programsToLoad.size() != 0) {
						ProgramToLoad programToLoad;
						synchronized (programsQueue.programsToLoad) {
							programToLoad = programsQueue.programsToLoad.pop();
						}
						ArrayList<ProgramInfo> programInfo = getProgram(programToLoad.url);
						// memoryCache.put(programToLoad.url, string);
						// TODO 2013-10-18 为了区分每日的节目预告，url末尾加上日期
						// 该url作为Hash Memory的键值
						memoryCache.put(
								programToLoad.url + Utils.getWeekOfDate(),
								programInfo);
						String tag = textViews.get(programToLoad.textView);
						if (tag != null && tag.equals(programToLoad.url)
								&& programInfo != null) {
							ProgramDisplayer bd = new ProgramDisplayer(
									getCurrentProgram(programInfo),
									programToLoad.textView);
							Activity a = (Activity) programToLoad.textView
									.getContext();
							a.runOnUiThread(bd);
						}
					}
					if (Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
				// allow thread to exit
			}
		}
	}

	ProgramsLoader programLoaderThread = new ProgramsLoader();

	// Used to display bitmap in the UI thread
	class ProgramDisplayer implements Runnable {
		String string;
		TextView textView;

		public ProgramDisplayer(String b, TextView i) {
			string = b;
			textView = i;
		}

		public void run() {
			if (string != null)
				textView.setText(string);
			else
				textView.setText("");
		}
	}

	public void clearCache() {
		memoryCache.clear();
	}

}
