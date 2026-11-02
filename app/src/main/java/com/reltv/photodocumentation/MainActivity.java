package com.reltv.photodocumentation;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES=101, TAKE_PHOTO=102;
    private EditText titleInput, dateInput, locationInput;
    private LinearLayout photoList;
    private final ArrayList<PhotoItem> photos=new ArrayList<>();
    private Uri cameraUri;
    private final ArrayList<EditText> captionInputs=new ArrayList<>();
    private LinearLayout root;

    static class PhotoItem { Uri uri; String caption; PhotoItem(Uri u,String c){uri=u;caption=c;} }

    @Override public void onCreate(Bundle b){ super.onCreate(b); buildUI(); loadDraft(); renderPhotos(); }

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(15);e.setHint(hint);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(bg(Color.rgb(245,247,250),12));return e;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);return b;}
    private void buildUI(){
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(28));root.setBackgroundColor(Color.rgb(248,250,252));scroll.addView(root);setContentView(scroll);
        TextView brand=text("ReL",18,Color.WHITE); TextView heading=text("PHOTO DOKUMENTASI",21,Color.WHITE); TextView sub=text("Dokumentasi Foto Profesional",12,0xFFD9E6F2);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(20),dp(18),dp(20));hero.setBackground(bg(Color.rgb(20,40,63),18));hero.addView(brand);hero.addView(heading);hero.addView(sub);root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        addGap(root,16);root.addView(text("INFORMASI DOKUMENTASI",13,Color.rgb(23,107,135)));
        titleInput=field("Judul dokumentasi / kegiatan");dateInput=field("Tanggal (contoh: 3 November 2026)");locationInput=field("Lokasi kegiatan");
        addField(titleInput);addField(dateInput);addField(locationInput);
        if(dateInput.getText().length()==0)dateInput.setText(new SimpleDateFormat("d MMMM yyyy",new Locale("id","ID")).format(new Date()));
        addGap(root,14);root.addView(text("FOTO DOKUMENTASI",13,Color.rgb(23,107,135)));
        TextView note=text("Tambahkan foto dari kamera atau galeri. Setiap foto mempunyai keterangan sendiri.",12,Color.DKGRAY);root.addView(note);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button gallery=button("＋ Galeri");Button camera=button("＋ Kamera");actions.addView(gallery,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(camera,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(actions);gallery.setOnClickListener(v->pickImages());camera.setOnClickListener(v->takePhoto());
        photoList=new LinearLayout(this);photoList.setOrientation(LinearLayout.VERTICAL);root.addView(photoList);
        addGap(root,12);Button save=button("Simpan Projek");Button pdf=button("BUAT PDF");save.setOnClickListener(v->{saveDraft();toast("Projek tersimpan di telefon.");});pdf.setOnClickListener(v->createPdf());root.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));root.addView(pdf,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView footer=text("PDF A4 • 2 foto setiap halaman • Tanpa logo",11,Color.GRAY);footer.setGravity(Gravity.CENTER);root.addView(footer);
    }
    private void addGap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void addField(EditText e){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.topMargin=dp(8);root.addView(e,p);}
    private void pickImages(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGES);}
    private void takePhoto(){try{ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"ReLTV_"+System.currentTimeMillis()+".jpg");values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");cameraUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);startActivityForResult(i,TAKE_PHOTO);}catch(Exception e){toast("Kamera tidak dapat dibuka: "+e.getMessage());}}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK)return;
        if(req==PICK_IMAGES&&data!=null){if(data.getClipData()!=null){for(int x=0;x<data.getClipData().getItemCount();x++)addPhoto(data.getClipData().getItemAt(x).getUri());}else if(data.getData()!=null)addPhoto(data.getData());}
        if(req==TAKE_PHOTO&&cameraUri!=null)addPhoto(cameraUri);renderPhotos();saveDraft();}
    private void addPhoto(Uri u){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}photos.add(new PhotoItem(u,""));}
    private void renderPhotos(){if(photoList==null)return;photoList.removeAllViews();captionInputs.clear();if(photos.size()==0){TextView empty=text("Belum ada foto. Tambahkan foto untuk memulakan dokumentasi.",12,Color.GRAY);empty.setPadding(0,dp(12),0,dp(12));photoList.addView(empty);return;}
        for(int i=0;i<photos.size();i++){final int index=i;PhotoItem item=photos.get(i);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(bg(Color.WHITE,14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);photoList.addView(card,cp);
            TextView label=text(String.format(Locale.US,"FOTO %02d",i+1),13,Color.rgb(23,107,135));card.addView(label);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);try{image.setImageURI(item.uri);}catch(Exception ignored){}card.addView(image,new LinearLayout.LayoutParams(-1,dp(170)));
            EditText caption=field("Keterangan foto "+(i+1));caption.setSingleLine(false);caption.setMinLines(2);caption.setText(item.caption);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(8);card.addView(caption,ep);captionInputs.add(caption);
            Button remove=button("Hapus foto ini");remove.setOnClickListener(v->{syncCaptions();photos.remove(index);renderPhotos();saveDraft();});card.addView(remove);
        }
    }
    private void syncCaptions(){for(int i=0;i<Math.min(photos.size(),captionInputs.size());i++)photos.get(i).caption=captionInputs.get(i).getText().toString();}
    private void saveDraft(){syncCaptions();try{org.json.JSONArray arr=new org.json.JSONArray();for(PhotoItem p:photos){org.json.JSONObject o=new org.json.JSONObject();o.put("uri",p.uri.toString());o.put("caption",p.caption);arr.put(o);}org.json.JSONObject rootJ=new org.json.JSONObject();rootJ.put("title",titleInput.getText().toString());rootJ.put("date",dateInput.getText().toString());rootJ.put("location",locationInput.getText().toString());rootJ.put("photos",arr);getSharedPreferences("draft",0).edit().putString("data",rootJ.toString()).apply();}catch(Exception e){toast("Gagal menyimpan projek.");}}
    private void loadDraft(){try{String raw=getSharedPreferences("draft",0).getString("data",null);if(raw==null)return;org.json.JSONObject o=new org.json.JSONObject(raw);titleInput.setText(o.optString("title"));dateInput.setText(o.optString("date"));locationInput.setText(o.optString("location"));org.json.JSONArray a=o.optJSONArray("photos");if(a!=null)for(int i=0;i<a.length();i++){org.json.JSONObject p=a.getJSONObject(i);photos.add(new PhotoItem(Uri.parse(p.getString("uri")),p.optString("caption")));}}catch(Exception ignored){}}
    private Bitmap loadBitmap(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri)){Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new Exception("Foto tidak dapat dibaca");return b;}}
    private void createPdf(){syncCaptions();if(photos.size()==0){toast("Tambahkan sekurang-kurangnya satu foto.");return;}saveDraft();PdfDocument doc=new PdfDocument();int pageNo=0;final int W=595,H=842;Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        try{for(int start=0;start<photos.size();start+=2){pageNo++;PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(W,H,pageNo).create();PdfDocument.Page page=doc.startPage(info);Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
            paint.setColor(Color.rgb(20,40,63));paint.setTextSize(20);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText("DOKUMENTASI FOTO",40,48,paint);
            paint.setColor(Color.DKGRAY);paint.setTextSize(10);paint.setTypeface(Typeface.DEFAULT);c.drawText(titleInput.getText().toString().isEmpty()?"Dokumentasi Kegiatan":titleInput.getText().toString(),40,70,paint);c.drawText("Tarikh: "+dateInput.getText().toString(),40,88,paint);c.drawText("Lokasi: "+locationInput.getText().toString(),40,104,paint);paint.setColor(Color.LTGRAY);c.drawLine(40,118,555,118,paint);
            for(int slot=0;slot<2;slot++){int idx=start+slot;if(idx>=photos.size())break;int top=140+slot*340;Bitmap bmp=loadBitmap(photos.get(idx).uri);int maxW=515,maxH=245;float scale=Math.min((float)maxW/bmp.getWidth(),(float)maxH/bmp.getHeight());int dw=(int)(bmp.getWidth()*scale),dh=(int)(bmp.getHeight()*scale);Rect dst=new Rect(40+(maxW-dw)/2,top+(maxH-dh)/2,40+(maxW-dw)/2+dw,top+(maxH-dh)/2+dh);c.drawBitmap(bmp,null,dst,paint);bmp.recycle();paint.setColor(Color.rgb(23,107,135));paint.setTextSize(11);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(String.format(Locale.US,"FOTO %02d",idx+1),40,top+263,paint);paint.setColor(Color.DKGRAY);paint.setTextSize(11);paint.setTypeface(Typeface.DEFAULT);drawWrapped(c,photos.get(idx).caption,40,top+282,510,paint);}
            paint.setColor(Color.GRAY);paint.setTextSize(9);c.drawText("ReL TV Photo Documentation",40,820,paint);c.drawText("Halaman "+pageNo,500,820,paint);doc.finishPage(page);}
            String filename="ReLTV_Dokumentasi_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date())+".pdf";ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,filename);values.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/ReL TV Documentation");Uri out=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(out==null)throw new Exception("Tidak dapat membuat fail PDF");try(OutputStream os=getContentResolver().openOutputStream(out)){doc.writeTo(os);}toast("PDF berjaya disimpan dalam Download/ReL TV Documentation.");
        }catch(Exception e){toast("PDF gagal dibuat: "+e.getMessage());}finally{doc.close();}}
    private void drawWrapped(Canvas c,String value,float x,float y,float width,Paint p){if(value==null||value.trim().isEmpty())value="Tiada keterangan.";String[] words=value.split("\\s+");String line="";float yy=y;for(String w:words){String test=line.isEmpty()?w:line+" "+w;if(p.measureText(test)>width){c.drawText(line,x,yy,p);line=w;yy+=16;}else line=test;}if(!line.isEmpty())c.drawText(line,x,yy,p);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
