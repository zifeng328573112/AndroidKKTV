package com.fedorvlasov.lazylist2;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

public class Utils {
	/**
	 * 获取星期值
	 */
	public static String getWeekOfDate() {
		String[] weekDays = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" };
		Calendar cal = Calendar.getInstance();
		Date curDate = new Date(System.currentTimeMillis());
		cal.setTime(curDate);
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w < 0)
			w = 0;
		return weekDays[w];
	}
}