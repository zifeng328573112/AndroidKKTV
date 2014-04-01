package org.stagex.danmaku.adapter;

import java.util.List;

import org.keke.player.R;
import org.stagex.danmaku.activity.TvProgramActivity;

import com.fedorvlasov.lazylist.ImageLoader;
import com.fedorvlasov.lazylist2.ProgramLoader;
import com.nmbb.oplayer.scanner.POChannelList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("ResourceAsColor")
public class ChannelAdapter extends BaseAdapter {
	private List<POChannelList> infos;
	private Context mContext;

	private LayoutInflater mLayoutInflater;

	// 自定义的img加载类，提升加载性能，防止OOM
	public ImageLoader imageLoader;
	public ProgramLoader programLoader;

	public int mCurrentIndex = -1;
	
	public ChannelAdapter(Context context, List<POChannelList> infos) {
		this.infos = infos;
		this.mContext = context;

		mLayoutInflater = LayoutInflater.from(context);
		imageLoader = new ImageLoader(context);
		programLoader = new ProgramLoader(context);

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
		// TODO Auto-generated method stub
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.channel_list_item,
					null);

			viewHolder = new ViewHolder();
			viewHolder.text = (TextView) convertView
					.findViewById(R.id.channel_name);
			viewHolder.pgmtext = (TextView) convertView
					.findViewById(R.id.program_name);
			viewHolder.imageView = (ImageView) convertView
					.findViewById(R.id.channel_icon);
			viewHolder.hotView = (ImageView) convertView
					.findViewById(R.id.hot_icon);
			viewHolder.newView = (ImageView) convertView
					.findViewById(R.id.new_icon);
			viewHolder.favView = (ImageView) convertView
					.findViewById(R.id.fav_icon);

			// TODO 节目预告
			viewHolder.programView = (LinearLayout) convertView
					.findViewById(R.id.program_icon);

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		
		// FIXME 2013-10-26 由于infos的内存会被清除，所以这里加以保护
		if (position < infos.size()) {
			viewHolder.programView.setTag(position);
			viewHolder.programView
					.setOnClickListener(new ImageView.OnClickListener() {
						public void onClick(View v) {
							// TODO Auto-generated method stub
							// Toast.makeText(
							// mContext,
							// "您单击了[" + infos.get((Integer) v.getTag()).getName()
							// + "]", Toast.LENGTH_SHORT).show();
							Intent intent = new Intent(mContext,
									TvProgramActivity.class);
							String channel_name = infos.get((Integer) v.getTag()).name;
							String program_path = infos.get((Integer) v.getTag()).program_path;
							if (program_path == null) {
								new AlertDialog.Builder(mContext)
										.setIcon(R.drawable.ic_dialog_alert)
										.setTitle("抱歉")
										.setMessage("暂时没有该电视台的节目预告！")
										.setPositiveButton(
												"知道了",
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
														// do nothing - it will
														// close on
														// its own
													}
												}).show();
							} else {
								intent.putExtra("ProgramPath", program_path);
								intent.putExtra("ChannelName", channel_name);
								mContext.startActivity(intent);
							}
						}
					});
	
			// 是否显示已收藏图标
			if (infos.get(position).save) {
				viewHolder.favView.setVisibility(View.VISIBLE);
			} else {
				// FIXME 2013-10-20 采用了ViewHolder之后，必须强制设置下默认值
				viewHolder.favView.setVisibility(View.GONE);
			}
	
			viewHolder.text.setText(infos.get(position).name);
			// 标记当前正在播放的频道
			if (mCurrentIndex == position)
				viewHolder.text.setTextColor(R.color.yellow);
			
			// 判断是否是热门频道，暂时使用HOT字样
			if (infos.get(position).mode.equalsIgnoreCase("HOT"))
				viewHolder.hotView.setVisibility(View.VISIBLE);
			else 
				viewHolder.hotView.setVisibility(View.GONE);
			// 判断是否是新频道，暂时用NEW字样
			if (infos.get(position).mode.equalsIgnoreCase("NEW"))
				viewHolder.newView.setVisibility(View.VISIBLE);
			else
				viewHolder.newView.setVisibility(View.GONE);
	
			// FIXME 添加对togic的部分图标的url无hostname的支持
			String iconUrl = infos.get(position).icon_url;
			if (iconUrl.startsWith("/upload")) {
				// add "http://tv.togic.com"
				StringBuffer urlBuf = new StringBuffer();
				urlBuf.append("http://tv.togic.com");
				urlBuf.append(iconUrl);
				// TODO 新方法，防止OOM
				imageLoader.DisplayImage(urlBuf.toString(), null,
						viewHolder.imageView);
			} else {
				// TODO 新方法，防止OOM
				imageLoader.DisplayImage(iconUrl, null, viewHolder.imageView);
			}
	
			// TODO 2013-10-17 实现节目预告功能
			if (infos.get(position).program_path != null) {
				programLoader.DisplayText(infos.get(position).program_path, null,
						viewHolder.pgmtext);
			} else {
				viewHolder.pgmtext.setText("");
			}
		} else {
//			Log.w("arrayOut", "===========arrayOUT===========");
		}
		
		return convertView;
	}

	private class ViewHolder {
		TextView text;
		TextView pgmtext;
		ImageView imageView;
		ImageView hotView;
		ImageView newView;
		ImageView favView;

		// TODO 节目预告
		LinearLayout programView;
	}
}
