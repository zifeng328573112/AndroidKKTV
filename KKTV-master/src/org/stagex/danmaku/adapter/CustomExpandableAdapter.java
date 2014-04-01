package org.stagex.danmaku.adapter;

import java.util.List;

import org.keke.player.R;
import org.stagex.danmaku.util.DensityUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

@SuppressLint("ResourceAsColor")
public class CustomExpandableAdapter extends BaseExpandableListAdapter {

	private List<String> groupArray;
	private List<List<ChannelInfo>> childArray;
	private LayoutInflater mLayoutInflater;
	private Activity activity;
	private Boolean fromPlaying;
	
	public int mCurGroup = -1;
	public int mCurChild = -1;

	public CustomExpandableAdapter(Activity activity, List<String> groupArray,
			List<List<ChannelInfo>> childArray, Boolean fromPlaying) {
		this.activity = activity;
		this.groupArray = groupArray;
		this.childArray = childArray;
		this.fromPlaying = fromPlaying;
		mLayoutInflater = LayoutInflater.from(activity);
	}

	@Override
	public int getGroupCount() {
		if (groupArray != null) {
			return groupArray.size();
		} else {
			return 0;
		}
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return childArray.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groupArray.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return childArray.get(groupPosition).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			if (fromPlaying)
				convertView = mLayoutInflater.inflate(R.layout.custom_expanditem2,
						null);
			else
				convertView = mLayoutInflater.inflate(R.layout.custom_expanditem,
					null);
			viewHolder = new ViewHolder();
			viewHolder.name = (TextView) convertView
					.findViewById(R.id.expand_name);
			viewHolder.name.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			viewHolder.name.setPadding(DensityUtil.dip2px(activity, 40), 0, 0, 0);
			if (fromPlaying == false)
				viewHolder.name.getPaint().setFakeBoldText(true);
			convertView.setTag(viewHolder);
			
			// 2013-11-05 为自定义收藏设置tag
			convertView.setTag(R.id.channel_name, groupPosition);
			convertView.setTag(R.id.channel_index, -1);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		// FIXME 2013-10-26 由于会清除childArray和groupArray的内存，故要加以保护
		if (groupPosition < groupArray.size()) {
			viewHolder.name.setText(groupArray.get(groupPosition));
		} else {
//			Log.w("arrayOut", "=========arraryOut============");
		}
		
		if (fromPlaying) {
			// 标记当前正在播放的频道
			if (mCurGroup == groupPosition) {
				// FIXME 2013-10-22 好像只能用Color.YELLOW，否则颜色不对
				viewHolder.name.setTextColor(Color.YELLOW);
			} else {
				viewHolder.name.setTextColor(Color.WHITE);
			}
		}
		
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		ViewHolder2 viewHolder;
		if (convertView == null) {
			if (fromPlaying)
				convertView = mLayoutInflater.inflate(R.layout.channel_list_item5,
						null);
			else
				convertView = mLayoutInflater.inflate(R.layout.channel_list_item4,
						null);
			viewHolder = new ViewHolder2();
			viewHolder.textName = (TextView) convertView
					.findViewById(R.id.channel_name);
			viewHolder.textIndex = (TextView) convertView
					.findViewById(R.id.channel_index);
//			viewHolder.name.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//			viewHolder.name.setPadding(DensityUtil.dip2px(activity, 40), 0, 0, 0);
			convertView.setTag(viewHolder);
			
			// 2013-11-05 为自定义收藏设置tag
			convertView.setTag(R.id.channel_name, groupPosition);
			convertView.setTag(R.id.channel_index, childPosition);
		} else {
			viewHolder = (ViewHolder2) convertView.getTag();
		}
		
		// 由于会清除childArray和groupArray的内存，故要加以保护
		if (groupPosition < groupArray.size() && childPosition < childArray.get(groupPosition).size()) {
			viewHolder.textName.setText(childArray.get(groupPosition)
					.get(childPosition).getName());
			viewHolder.textIndex.setText(Integer.toString(childPosition + 1) + ".");
		} else {
//			Log.w("arrayOut", "=========arraryOut============");
		}
		
		if (fromPlaying) {
			// 标记当前正在播放的频道
			if (mCurGroup == groupPosition && mCurChild == childPosition) {
				// FIXME 2013-10-22 好像只能用Color.YELLOW，否则颜色不对
				viewHolder.textIndex.setTextColor(Color.YELLOW);
				viewHolder.textName.setTextColor(Color.YELLOW);
			} else {
				viewHolder.textIndex.setTextColor(Color.WHITE);
				viewHolder.textName.setTextColor(Color.WHITE);
			}
		}
		
//		Log.d("test", "===> child name ==>"
//				+ childArray.get(groupPosition).get(childPosition).getName());

		return convertView;
	}

	private class ViewHolder {
		TextView name;
	}

	private class ViewHolder2 {
		TextView textName;
		TextView textIndex;
	}
	
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}