package com.voxivoid.recipelab;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.lang.reflect.Method;

/** Read-only Avindex cursor + native image decode. Called on ONE dedicated worker only. */
final class SonyPhotoSource {
    private final Context context;
    private final Class<?> optionsClass;
    private final Method decode;
    private Cursor cursor;
    private int position=-1;
    private String path;
    SonyPhotoSource(Context context) throws Exception {
        this.context=context.getApplicationContext();
        optionsClass=Class.forName("com.sony.scalar.graphics.OptimizedImageFactory$Options");
        decode=Class.forName("com.sony.scalar.graphics.OptimizedImageFactory").getMethod("decodeImage",String.class,optionsClass);
        Class.forName("com.sony.scalar.provider.AvindexStore$Images$Media").getMethod("getContentUri",String.class);
    }
    private void query() throws Exception {
        if(cursor!=null)return;
        String[] ids=(String[])Class.forName("com.sony.scalar.provider.AvindexStore").getMethod("getExternalMediaIds").invoke(null);
        if(ids==null || ids.length==0)throw new IllegalStateException("NO_CARD");
        Uri uri=(Uri)Class.forName("com.sony.scalar.provider.AvindexStore$Images$Media").getMethod("getContentUri",String.class).invoke(null,ids[0]);
        cursor=context.getContentResolver().query(uri,new String[]{"_id","_data"},null,null,"content_created_utc_date_time DESC");
        if(cursor==null)throw new IllegalStateException("INDEX_UNAVAILABLE");
        cursor.getColumnIndexOrThrow("_data");
    }
    String select(int direction,ReviewLogic.Tickets tickets,int ticket) throws Exception {
        query();
        int count=cursor.getCount();
        if(count==0)return null;
        if(direction==0 && path!=null)return path;
        int found=ReviewLogic.find(count,position,direction,new ReviewLogic.Rows(){public String path(int at){
            if(!cursor.moveToPosition(at))return null;
            return cursor.getString(cursor.getColumnIndexOrThrow("_data"));
        }},tickets,ticket);
        if(found<0)return null;
        position=found;path=cursor.getString(cursor.getColumnIndexOrThrow("_data"));return path;
    }
    Object decode(String path,boolean main) throws Exception {
        Object options=optionsClass.getConstructor().newInstance();
        optionsClass.getField("bBasicInfo").setBoolean(options,true);
        // SCN and MAIN are read from the platform, not guessed integer IDs.
        optionsClass.getField("imageType").setInt(options,optionsClass.getField(main?"MAIN":"SCN").getInt(null));
        return decode.invoke(null,path,options);
    }
    static boolean release(Object image){
        if(image!=null)try{image.getClass().getMethod("release").invoke(image);}catch(Exception failure){return false;}
        return true;
    }
    void close(){if(cursor!=null){try{cursor.close();}finally{cursor=null;position=-1;path=null;}}}
}
