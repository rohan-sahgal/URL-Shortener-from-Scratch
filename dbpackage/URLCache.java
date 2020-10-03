// import java.util.ArrayList;
// import java.util.HashMap;
import java.util.*;
import java.io.*;
// import org.apache.commons.collections.MapIterator;
// import org.apache.commons.collections.map.LRUMap;
 
 
public class URLCache {
 
    private long timeToLive;
    private HashMap<String, CacheObject> cacheMap;
 
    protected class CacheObject {
        public long lastAccessed = System.currentTimeMillis();
        public String value;
 
        protected CacheObject(String value) {
            this.value = value;
        }
    }
 
    public URLCache(long TimeToLive, final long TimerInterval, int maxItems) {
        this.timeToLive = TimeToLive * 1000;
 
        cacheMap = new HashMap<String, CacheObject>(maxItems);
 
        if (timeToLive > 0 && TimerInterval > 0) {
 
            Thread t = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(TimerInterval * 1000);
                        } catch (InterruptedException ex) {
                        }
                        cleanup();
                    }
                }
            });
 
            t.setDaemon(true);
            t.start();
        }
    }
 
    public void put(String shortURL, String longURL) {
        synchronized (cacheMap) {
            cacheMap.put(shortURL, new CacheObject(longURL));
        }
    }
 
    @SuppressWarnings("unchecked")
    public String get(String shortURL) {
        synchronized (cacheMap) {
            CacheObject c = (CacheObject) cacheMap.get(shortURL);
 
            if (c == null)
                return null;
            else {
                c.lastAccessed = System.currentTimeMillis();
                return c.value;
            }
        }
    }
 
    public void remove(String shortURL) {
        synchronized (cacheMap) {
            cacheMap.remove(shortURL);
        }
    }
 
    public int size() {
        synchronized (cacheMap) {
            return cacheMap.size();
        }
    }
 
    @SuppressWarnings("unchecked")
    public void cleanup() {
 
        long now = System.currentTimeMillis();
        ArrayList<String> deleteKey = null;
 
        synchronized (cacheMap) {
            // MapIterator itr = cacheMap.mapIterator();
            Iterator hashMapIterator = cacheMap.entrySet().iterator();
            // Iterator hashMapIterator = cacheMap.keySet().iterator();

            deleteKey = new ArrayList<String>((cacheMap.size() / 2) + 1);
            String key = null;
            CacheObject c = null;

            while (hashMapIterator.hasNext()) {
                
                // key = (String) hashMapIterator.next();
                Map.Entry mapElement = (Map.Entry)hashMapIterator.next();
                c = (CacheObject) mapElement.getValue();
                key = (String) mapElement.getKey();
 
                if (c != null && (now > (timeToLive + c.lastAccessed))) {
                    deleteKey.add(key);
                }
            }
        }
 
        for (String key : deleteKey) {
            synchronized (cacheMap) {
                cacheMap.remove(key);
            }
 
            Thread.yield();
        }
    }
}