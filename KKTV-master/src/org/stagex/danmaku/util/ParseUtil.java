package org.stagex.danmaku.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.adapter.ProvinceInfo;

import com.nmbb.oplayer.scanner.ChannelListBusiness;
import com.nmbb.oplayer.scanner.POUserDefChannel;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class ParseUtil {
	// 解析json数据表
	public static List<ChannelInfo> parse(Context context, Boolean pathFlag) {
		List<ChannelInfo> list = new ArrayList<ChannelInfo>();

		int length = 0;
		byte[] byteBuffer = null;

		String destPath = Environment.getExternalStorageDirectory().getPath()
				+ "/kekePlayer/";
		String zipPath = Environment.getExternalStorageDirectory().getPath()
				+ "/kekePlayer/.channel_list.zip";
		String srcPath = Environment.getExternalStorageDirectory().getPath()
				+ "/kekePlayer/channel_list_cn.list.api2";

		File zipFile = new File(zipPath);

		/*
		 * FIXME 为了便于测试直播地址，特地在这里加一行代码，强制 使用本地asset的下的地址，测试通过之后，再上传服务器
		 */
		// pathFlag = false;
		/* end */

		try {
			// pathFlag为true，表示采用更新后的地址
			// pathFlag为false，表示采用assert目录下默认地址
			if (pathFlag) {
				// FIXME 2013-11-03 解压缩文件
				Log.i("ParseUtil", "==========> from network");
			} else {
				Log.i("ParseUtil", "==========> from localAsset");
				copyBigDataToSD(context, zipPath);
			}
			// 解码加密的zip文件
			try {
				unzip(zipFile, destPath, "3421170@keke");
			} catch (ZipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// ========================
			InputStream fos = new FileInputStream(srcPath);
			length = fos.available();
			byteBuffer = new byte[length];
			fos.read(byteBuffer);
			fos.close();
			// 删除临时文件，为保密
			File tmp = new File(Environment.getExternalStorageDirectory().getPath()
					+ "/kekePlayer/.channel_list.zip");
			if (tmp.exists())
				tmp.delete();
			tmp = new File(Environment.getExternalStorageDirectory().getPath()
					+ "/kekePlayer/channel_list_cn.list.api2");
			if (tmp.exists())
				tmp.delete();
			// =========================
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// =========================

		// 解析JSON文件
		if (byteBuffer != null) {
			try {
				JSONArray arr = new JSONArray(new String(byteBuffer, 0, length));

				int nums = arr.length();

				for (int i = 0; i < nums; i++) {
					JSONObject obj = arr.getJSONObject(i);
					int id = obj.getInt("channel_id");
					String name = obj.getString("channel_name");
					String icon_url = obj.getString("icon_url");
					String province_name = null;
					if (obj.has("province"))
						province_name = obj.getString("province");
					String mode = obj.getString("mode");
					String url = obj.getString("url");
					JSONArray secArr = obj.getJSONArray("second_url");
					int size = secArr.length();
					String[] second_url = null;
					if (size > 0) {
						second_url = new String[size];
						for (int j = 0; j < size; j++) {
							second_url[j] = (String) secArr.get(j);
						}
					}
					String types = obj.getString("types");
					String path = null;
					if (obj.has("path"))
						path = obj.getString("path");
					ChannelInfo info = new ChannelInfo(id, name, icon_url,
							province_name, mode, url, second_url, types, path);
					list.add(info);
				}

				Log.d("ParseUtil", "tvlist nums = " + nums);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list;
	}

	// =============================================================
	// 解析本地自定义的列表
	// TODO 2013-09-24
	// 为了加强自定义的分类功能，采用数据库暂存所有的自定义
	// 的数据，每次加载时，重新入数据库？或者查看自定义文件
	// 是否作出了修改，若修改了，则清除数据库数据，重新装载
	public static List<ChannelInfo> parseDef(String tvList) {
		List<ChannelInfo> list = new ArrayList<ChannelInfo>();
		int nums = 0;
		String code = "GBK";
		String privName = null;
		String sortName = null;
		String privSort = null;
		String tvName = null;
		String first_url = null;
		ArrayList<String> list_url = new ArrayList<String>();

		// ArrayList<String> sortNames = new ArrayList<String>();

		// FIXBUG 2013-07-28
		Boolean dropLast = true;

		try {
			// 探测txt文件的编码格式
			code = codeString(tvList);
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
						sortName = "other";
						tvName = scName;
					} else {
						tvName = pair2[1].trim();
						sortName = pair2[0].trim();
						// TODO 暂时按照顺序来比较，提取分类
						if (privSort == null) {
							// 第一次出现，直接存储，后续以文件存储？
							privSort = sortName;
							// sortNames.add(privSort + '\n');
						} else {
							// 将新的分类存储起来
							if (sortName.equals(privSort) == false) {
								privSort = sortName;
								// sortNames.add(privSort + '\n');
							}
						}
					}
					// end

					// TODO 合并相同节目名称的源
					if (tvName.equals(privName))
						list_url.add(url);
					else {
						if (privName != null) {
							// 保存节目源
							String[] second_url = new String[list_url.size()];
							list_url.toArray(second_url);
							ChannelInfo info = new ChannelInfo(0, privName,
									null, sortName, null, first_url,
									second_url, null, null);
							list.add(info);
						}
						list_url.clear();
						first_url = url;
						privName = tvName;
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

		// DEBUG
		// Log.d("UserDefSort", "all userdef sortName = " +
		// sortNames.toString());
		// write to file
		// saveFile(sortNames);

		return list;
	}

	// /*
	// * 暂存自定义分类名称
	// */
	// private static void saveFile(ArrayList<String> sortNames) {
	// File backupFile = new File(Environment.getExternalStorageDirectory(),
	// "/kekePlayer/.selfDefineSort.txt");
	// try {
	// FileOutputStream fos = new FileOutputStream(backupFile);
	// OutputStreamWriter ow = new OutputStreamWriter(fos, "GBK");
	// BufferedWriter bw = new BufferedWriter(ow);
	// try {
	// for (String sort : sortNames) {
	// bw.append(sort);
	// }
	// bw.flush();
	// } finally {
	// bw.close();
	// ow.close();
	// fos.close();
	// Log.d("sortName", "===>backup selfdefine sort name success");
	// }
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	// =============================================================
	/*
	 * 读取自定义分类名称
	 */
	// public static List<String> readFile(String sortPath) {
	// int nums = 0;
	// String code = "GBK";
	//
	// List<String> sortNames = new ArrayList<String>();
	//
	// try {
	// // 探测txt文件的编码格式
	// code = codeString(sortPath);
	// } catch (Exception e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	//
	// try {
	// InputStream is = new FileInputStream(sortPath);
	// InputStreamReader ir = new InputStreamReader(is, code);
	// BufferedReader br = new BufferedReader(ir);
	// try {
	// while (true) {
	// String line = br.readLine();
	// if (line == null) {
	// break;
	// }
	// nums++;
	// sortNames.add(line);
	// }
	// } finally {
	// br.close();
	// ir.close();
	// is.close();
	// }
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// Log.d("ParseUtil", "user define sort nums = " + nums);
	//
	// return sortNames;
	// }

	// =============================================================

	/**
	 * 判断文件的编码格式
	 * 
	 * @param fileName
	 *            :file
	 * @return 文件编码格式
	 * @throws Exception
	 */
	public static String codeString(String fileName) throws Exception {
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(
				fileName));
		int p = (bin.read() << 8) + bin.read();
		String code = null;

		switch (p) {
		case 0xefbb:
			code = "UTF-8";
			break;
		case 0xfffe:
			code = "Unicode";
			break;
		case 0xfeff:
			code = "UTF-16BE";
			break;
		default:
			code = "GBK";
		}

		bin.close();

		Log.d("Parseutil", "find text code ===>" + code);

		return code;
	}

	/**
	 * 获取省份名单分类
	 * 
	 * @return
	 */
	public static List<ProvinceInfo> getProvinceNames(Context context) {
		List<ProvinceInfo> list = new ArrayList<ProvinceInfo>();
		StringBuffer stringBuffer = new StringBuffer();
		int len = -1;
		int all = 0;

		try {
			char[] readBuffer = new char[1024];

			InputStream fip = context.getAssets().open("province.json");
			InputStreamReader ir = new InputStreamReader(fip);
			BufferedReader br = new BufferedReader(ir);

			while ((len = br.read(readBuffer)) != -1) {
				all += len;
				stringBuffer.append(new String(readBuffer, 0, len));
			}
			fip.close();
			ir.close();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.d("ParseUtil",
				"all = " + all + " buff len = " + stringBuffer.length());

		try {
			JSONArray arr = new JSONArray(stringBuffer.toString());

			int nums = arr.length();

			for (int i = 0; i < nums; i++) {
				JSONObject obj = arr.getJSONObject(i);
				String name = obj.getString("name");
				String icon = obj.getString("icon");
				ProvinceInfo info = new ProvinceInfo(name, icon);
				list.add(info);
			}

			Log.d("ParseUtil", "province nums = " + nums);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return list;
	}

	// 解析本地自定义的列表
	public static List<String> parseName(String path) {
		List<String> list = new ArrayList<String>();
		int nums = 0;
		String code = "GBK";

		try {
			// 探测txt文件的编码格式
			code = codeString(path);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			InputStream is = new FileInputStream(path);
			InputStreamReader ir = new InputStreamReader(is, code);
			BufferedReader br = new BufferedReader(ir);
			try {
				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					list.add(line);
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

		Log.d("ParseUtil", "user fav  tvlist nums = " + nums);

		return list;
	}

	// ====================

	private static void copyBigDataToSD(Context mContext, String strOutFileName)
			throws IOException {
		InputStream myInput;
		OutputStream myOutput = new FileOutputStream(strOutFileName);
		myInput = mContext.getAssets().open("channel_list.zip");
		byte[] buffer = new byte[1024];
		int length = myInput.read(buffer);
		while (length > 0) {
			myOutput.write(buffer, 0, length);
			length = myInput.read(buffer);
		}

		myOutput.flush();
		myInput.close();
		myOutput.close();
	}

	// FIXME 2013-11-03 解压缩zip文件
	public static void unzip(File zipFile, String dest, String passwd)
			throws ZipException {
		ZipFile zFile = new ZipFile(zipFile); // 首先创建ZipFile指向磁盘上的.zip文件
		zFile.setFileNameCharset("GBK"); // 设置文件名编码，在GBK系统中需要设置
		if (!zFile.isValidZipFile()) { // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
			throw new ZipException("压缩文件不合法,可能被损坏.");
		}
		File destDir = new File(dest); // 解压目录
		if (destDir.isDirectory() && !destDir.exists()) {
			destDir.mkdir();
		}
		if (zFile.isEncrypted()) {
			zFile.setPassword(passwd.toCharArray()); // 设置密码
		}
		zFile.extractAll(dest); // 将文件抽出到解压目录(解压)
	}
	// ====================
}
