package org.stagex.danmaku.adapter;

import java.util.ArrayList;

import org.keke.player.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ProgramAdapter extends BaseAdapter {
	private static final String LOGTAG = "ProgramAdapter";
	private ArrayList<ProgramInfo> infos;
	private Context mContext;

	private LayoutInflater mLayoutInflater;

	public ProgramAdapter(Context context, ArrayList<ProgramInfo> infos) {
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

			convertView = mLayoutInflater.inflate(R.layout.program_list_item,
					null);
			viewHolder = new ViewHolder();
			viewHolder.text1 = (TextView) convertView.findViewById(R.id.time);
			viewHolder.text2 = (TextView) convertView
					.findViewById(R.id.program);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.text1.setText(infos.get(position).getTime());
		viewHolder.text2.setText(infos.get(position).getProgram());

		if (infos.get(position).getCurProgram()) {
			viewHolder.text1.setTextColor(mContext.getResources().getColor(
					R.color.green));
			viewHolder.text2.setTextColor(mContext.getResources().getColor(
					R.color.green));
		} else {
			viewHolder.text1.setTextColor(mContext.getResources().getColor(
					R.color.kkblack));
			viewHolder.text2.setTextColor(mContext.getResources().getColor(
					R.color.kkblack));
		}

		return convertView;
	}

	private class ViewHolder {
		TextView text1;
		TextView text2;
	}
}
