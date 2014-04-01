package com.fedorvlasov.lazylist2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.stagex.danmaku.adapter.ProgramInfo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileCache {

	private File cacheDir;

	public FileCache(Context context) {
		// Find the dir to save cached images
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED))
			cacheDir = new File(
					android.os.Environment.getExternalStorageDirectory(),
					"kekeplayer/LiveTV_guide");
		else
			cacheDir = context.getCacheDir();
		if (!cacheDir.exists())
			cacheDir.mkdirs();
	}

	public File getFile(String url) {
		// I identify images by hashcode. Not a perfect solution, good for the
		// demo.
		String filename = String.valueOf(url.hashCode());
		File f = new File(cacheDir, filename);
		return f;
	}

	public void clear() {
		File[] files = cacheDir.listFiles();
		for (File f : files)
			f.delete();
	}

	/*
	 * TODO 暂存各个频道的自定义数据
	 */
	public void saveFile(ArrayList<ProgramInfo> tvGuide, File programFile) {
		try {
			FileOutputStream fos = new FileOutputStream(programFile);
			OutputStreamWriter ow = new OutputStreamWriter(fos);
			BufferedWriter bw = new BufferedWriter(ow);
			try {
				for (ProgramInfo program : tvGuide) {
						bw.append(program.getTime() + "," + program.getProgram() + "\n");
				}
				bw.flush();
			} finally {
				bw.close();
				ow.close();
				fos.close();
//				Log.d("program", "===>backup selfdefine sort name success");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}