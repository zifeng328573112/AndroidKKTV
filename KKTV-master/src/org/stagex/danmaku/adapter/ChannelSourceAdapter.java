package org.stagex.danmaku.adapter;

import java.util.ArrayList;

import org.keke.player.R;
import org.stagex.danmaku.util.SourceName;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChannelSourceAdapter extends BaseAdapter {
	private static final String LOGTAG = "ChannelSourceAdapter";
	private ArrayList<String> infos;
	private Context mContext;

	private LayoutInflater mLayoutInflater;
	public int mCurrentIndex = 0;
	
	public ChannelSourceAdapter(Context context, ArrayList<String> infos) {
		this.infos = infos;
		this.mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return infos.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return infos.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Log.d(LOGTAG, infos.get(position));
		// TODO Auto-generated method stub
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(
					R.layout.channel_source_list_item, null);
			viewHolder = new ViewHolder();
			viewHolder.text1 = (TextView) convertView
					.findViewById(R.id.channel_name);
			viewHolder.text2 = (TextView) convertView
					.findViewById(R.id.source_name);
			viewHolder.hdView = (ImageView) convertView
					.findViewById(R.id.hd_icon);
			viewHolder.hotView = (ImageView) convertView
					.findViewById(R.id.hot_icon);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		// TODO 在界面中显示相关的节目源信息，而不是其URL的地址
		// 可以增加一个帮助界面，来介绍诸如：LETV、搜狐视频、CNTV、QQ视频等节目源
		// 以表明一般哪个节目源地址比较流畅
		/* 截取url的hostname，提高甄别的效率 */
		String urlName = SourceName.whichName(infos.get(position));

		// 是否是高清
		if (SourceName.mHd)
			viewHolder.hdView.setVisibility(View.VISIBLE);
		else
			viewHolder.hdView.setVisibility(View.GONE);
		// 是否是热门链接
		if (SourceName.mHot)
			viewHolder.hotView.setVisibility(View.VISIBLE);
		else
			viewHolder.hotView.setVisibility(View.GONE);
		
		viewHolder.text1.setText(Integer.toString(position + 1));
		viewHolder.text2.setText(urlName);
		
		// 标记当前正在播放的频道
		if (mCurrentIndex == position) {
			// FIXME 2013-10-22 好像只能用Color.YELLOW，否则颜色不对
			viewHolder.text1.setTextColor(Color.YELLOW);
			viewHolder.text2.setTextColor(Color.YELLOW);
		} else {
			viewHolder.text1.setTextColor(Color.WHITE);
			viewHolder.text2.setTextColor(Color.WHITE);
		}

		// Log.d(LOGTAG, "地址" + Integer.toString(position));

		return convertView;
	}

	private class ViewHolder {
		TextView text1;
		TextView text2;
		ImageView hdView;
		ImageView hotView;
	}
}
