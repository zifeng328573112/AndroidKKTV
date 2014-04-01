package org.stagex.danmaku.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.adapter.ProgramAdapter;
import org.stagex.danmaku.adapter.ProgramInfo;

import com.nmbb.oplayer.scanner.ChannelListBusiness;
import com.nmbb.oplayer.scanner.POUserDefChannel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ProgramTask extends AsyncTask<String, Void, String> {
//	private static final String COMMAND_BACKUP = "backupDatabase";
//	public static final String COMMAND_RESTORE = "restoreDatabase";

	private int listPosition = 0;
	private TextView text;
	
	public ProgramTask(TextView text) {
		this.text = text;
	}
	
	@SuppressLint("SimpleDateFormat")
	@Override
	protected String doInBackground(String... params) {
		
		String programPath = params[0];
		
//		Log.d("===", "get into ====================>");
		
		/* ====================================================== */
		/* 用webview方式显示节目预告 */
		// readHtmlFormAssets();
		/* ====================================================== */
		/* TODO 以listView文本方式显示节目预告 */
		Document doc = null;
		try {
			doc = Jsoup.connect(
					"http://www.tvmao.com/ext/show_tv.jsp?p=" + programPath)
					.get();

			Elements links = doc.select("li"); // 带有href属性的a元素

			ArrayList<ProgramInfo> infos = new ArrayList<ProgramInfo>();

			Date fromDate = new Date();
			SimpleDateFormat simple1 = new SimpleDateFormat("kk:mm");

			// 当前时间
			String timeStr = DateFormat.format("kk:mm",
					System.currentTimeMillis()).toString();
			try {
				fromDate = simple1.parse(timeStr);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return null;
			}

			long curTime = fromDate.getTime();
			Boolean findFlag = false;

			for (Element link : links) {
				String[] pair = link.text().split(" ");
				if (pair.length < 2)
					continue;
				String time = pair[0].trim();
				String program = pair[1].trim();

				if (!findFlag) {
					listPosition++;
					try {
						fromDate = simple1.parse(time);
						/* 找到第一个比当前时间大的节目，而正在播放的实际是前一个节目 */
						if (fromDate.getTime() >= curTime) {
							findFlag = true;
						}
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				ProgramInfo info = new ProgramInfo(time, program, false);
				infos.add(info);
			}

			// 在listView中突出显示当前的播放节目
			if (!findFlag) {
				// FIXME bug#0022 有些节目预告有内容，但是不是真正的节目单，此时的失败是因为没有节目单
				if (infos.size() == 0) {
					return null;
				} else {
					// FIXME bug#0022 此处的没找到是因为有节目预告，但是处于24：00分左右的临界情况
					/* 如果没有大于当前时间值的节目，说明当日的最后一个节目就是当前播放的节目 */
					return "正在播出：" + infos.get(infos.size() - 1).getProgram();
				}
			} else if (listPosition == 1) {
				/* 如果第一个节目的时间指就大于当前时间，实际是前一天的最后一个节目，在新的一天什么都不显示 */
			} else {
				/* 其他正常情况，如果找到一个大于当前时间值的节目，置前一个节目为正在播放节目 */
				return "正在播出：" + infos.get(listPosition - 2).getProgram();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* ====================================================== */
		
		return null;
	}
	
    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(String result) {
//    	Log.d("===", "onPostExecute ====================>");
    	text.setText(result);
    }

    @Override
    protected void onPreExecute() {
        // 任务启动，可以在这里显示一个对话框，这里简单处理
    	text.setText("正在播出：");
    }
//
//    @Override
//    protected void onProgressUpdate(Integer... values) {
//        // 更新进度
//          System.out.println(""+values[0]);
//          message.setText(""+values[0]);
//          pdialog.setProgress(values[0]);
//    }
}
