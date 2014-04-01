package org.stagex.danmaku.adapter;

import java.util.List;

import org.keke.player.R;

import com.fedorvlasov.lazylist.ImageLoader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ProvinceAdapter extends BaseAdapter {
	private static final String LOGTAG = "ProvinceAdapter";
	private List<ProvinceInfo> infos;
	private Context mContext;

	// 自定义的img加载类，提升加载性能，防止OOM
	public ImageLoader imageLoader;

	private LayoutInflater mLayoutInflater;

	public ProvinceAdapter(Context context, List<ProvinceInfo> infos) {
		this.infos = infos;
		this.mContext = context;

		mLayoutInflater = LayoutInflater.from(context);
		imageLoader = new ImageLoader(context);
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

			convertView = mLayoutInflater.inflate(R.layout.province_list_item,
					null);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView) convertView
					.findViewById(R.id.province_icon);
			viewHolder.text2 = (TextView) convertView
					.findViewById(R.id.province_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.text2.setText(infos.get(position).getProvinceName());

		// TODO 新方法，防止OOM
		imageLoader.DisplayImage(infos.get(position).getIcon(), null,
				viewHolder.imageView);

		return convertView;
	}

	private class ViewHolder {
		ImageView imageView;
		TextView text2;
	}
}
