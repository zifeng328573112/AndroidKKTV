package com.fedorvlasov.lazylist2;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.stagex.danmaku.adapter.ProgramInfo;

import android.graphics.Bitmap;

public class MemoryCache {
    private HashMap<String, SoftReference<ArrayList<ProgramInfo>>> cache=new HashMap<String, SoftReference<ArrayList<ProgramInfo>>>();
    
    public ArrayList<ProgramInfo> get(String id){
        if(!cache.containsKey(id))
            return null;
        SoftReference<ArrayList<ProgramInfo>> ref=cache.get(id);
        return ref.get();
    }
    
    public void put(String id, ArrayList<ProgramInfo> infos){
        cache.put(id, new SoftReference<ArrayList<ProgramInfo>>(infos));
    }

    public void clear() {
        cache.clear();
    }
}