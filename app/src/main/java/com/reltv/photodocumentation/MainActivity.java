package com.reltv.photodocumentation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES=101, TAKE_PHOTO=102;
    private EditText titleInput;
    private LinearLayout photoList,root;
    private Spinner countSpinner,orientationSpinner;
    private final ArrayList<PhotoItem> photos=new ArrayList<>();
    private final ArrayList<EditText> captionInputs=new ArrayList<>();
    private Uri cameraUri;
    private int ink=Color.rgb(25,43,67), teal=Color.rgb(0,137,145), orange=Color.rgb(242,139,54);
    private int pdfTitleSize=18,pdfMargin=24,pdfGap=10,pdfInset=0,pdfTitleGap=20;
    static class PhotoItem { Uri uri; String caption; float scale=1f; PhotoItem(Uri u,String c){uri=u;caption=c;} }

    @Override public void onCreate(Bundle b){super.onCreate(b);buildUI();loadDraft();renderPhotos();}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));return d;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setHint(hint);e.setPadding(dp(13),dp(8),dp(13),dp(8));e.setBackground(shape(Color.rgb(244,247,250),13));return e;}
    private Button button(String label,int color){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setBackground(shape(color,14));b.setPadding(dp(10),0,dp(10),0);return b;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(shape(Color.WHITE,18));return l;}
    private void buildUI(){
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(26));root.setBackgroundColor(Color.rgb(239,244,248));scroll.addView(root);setContentView(scroll);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(20),dp(22),dp(20),dp(22));hero.setBackground(gradient(Color.rgb(19,43,69),Color.rgb(0,126,139),22));
        TextView brand=text("ReL  /  DOKUMENTASI",12,0xFFBCE8E8);brand.setLetterSpacing(.08f);hero.addView(brand);
        TextView heading=text("Photo Dokumentasi",25,Color.WHITE);heading.setTypeface(null,1);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(7);hero.addView(heading,hp);
        TextView sub=text("Catat kegiatan. Susun foto. Cetak laporan.",12,0xFFE1F4F4);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(5);hero.addView(sub,sp);
        TextView chip=text("  A4  •  PDF  •  KOLASE 2–6 FOTO  ",11,Color.WHITE);chip.setPadding(dp(8),dp(8),dp(8),dp(8));chip.setBackground(shape(0x33FFFFFF,30));LinearLayout.LayoutParams chp=new LinearLayout.LayoutParams(-2,-2);chp.topMargin=dp(16);hero.addView(chip,chp);root.addView(hero);
        addGap(root,14);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statCard("01","ATURAN KOLASE",teal),new LinearLayout.LayoutParams(0,-2,1));
        View sep=new View(this);stats.addView(sep,new LinearLayout.LayoutParams(dp(9),1));
        stats.addView(statCard("A4","DUA ORIENTASI",orange),new LinearLayout.LayoutParams(0,-2,1));root.addView(stats);
        addGap(root,16);section("01","Informasi dokumentasi","Isi identitas laporan kegiatan");
        LinearLayout info=card();titleInput=field("Masukkan judul dokumentasi");addField(info,titleInput);root.addView(info);
        addGap(root,16);section("02","Bingkai kolase","Tentukan jumlah foto dan arah kertas A4");
        LinearLayout settings=card();settings.addView(text("JUMLAH FOTO PER HALAMAN",11,ink));
        countSpinner=new Spinner(this);countSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"2 foto per halaman","3 foto per halaman","4 foto per halaman","5 foto per halaman","6 foto per halaman"}));addSpinner(settings,countSpinner);
        TextView orientLabel=text("ORIENTASI KERTAS",11,ink);LinearLayout.LayoutParams olp=new LinearLayout.LayoutParams(-1,-2);olp.topMargin=dp(12);settings.addView(orientLabel,olp);
        orientationSpinner=new Spinner(this);orientationSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Portrait (tegak)","Landscape (mendatar)"}));addSpinner(settings,orientationSpinner);root.addView(settings);
        addGap(root,16);section("03","Foto dokumentasi","Tambah foto dari galeri atau kamera");
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button gallery=button("＋  Pilih galeri",teal),camera=button("＋  Kamera",orange);
        actions.addView(gallery,new LinearLayout.LayoutParams(0,dp(50),1));View gap=new View(this);actions.addView(gap,new LinearLayout.LayoutParams(dp(10),1));actions.addView(camera,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(actions);
        gallery.setOnClickListener(v->pickImages());camera.setOnClickListener(v->takePhoto());
        photoList=new LinearLayout(this);photoList.setOrientation(LinearLayout.VERTICAL);root.addView(photoList);
        addGap(root,16);section("04","Pengaturan dan cetak","Atur tampilan PDF sebelum disimpan");
        LinearLayout editCard=card();Button layout=button("EDIT TATA LETAK PDF",ink);editCard.addView(layout,new LinearLayout.LayoutParams(-1,dp(48)));root.addView(editCard);layout.setOnClickListener(v->showLayoutEditor());
        addGap(root,10);
        LinearLayout saveCard=card();Button save=button("SIMPAN DRAF",ink),preview=button("LIHAT PREVIEW",orange),pdf=button("BUAT PDF A4",teal);
        saveCard.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,dp(50));vp.topMargin=dp(9);saveCard.addView(preview,vp);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(54));pp.topMargin=dp(9);saveCard.addView(pdf,pp);root.addView(saveCard);
        save.setOnClickListener(v->{saveDraft();toast("Draf dokumentasi tersimpan di telefon.");});preview.setOnClickListener(v->showPreview());pdf.setOnClickListener(v->askPdfFileName());
        TextView footer=text("ReL PHOTO DOKUMENTASI  •  V2",10,Color.GRAY);footer.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(18);root.addView(footer,fp);
    }
    private LinearLayout statCard(String big,String small,int accent){LinearLayout c=card();TextView n=text(big,22,accent);n.setTypeface(null,1);c.addView(n);TextView s=text(small,9,ink);s.setTypeface(null,1);c.addView(s);return c;}
    private void section(String no,String heading,String desc){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);TextView n=text(no+"  /  "+heading.toUpperCase(Locale.ROOT),12,teal);n.setTypeface(null,1);row.addView(n);TextView d=text(desc,11,Color.GRAY);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(3);row.addView(d,p);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(9);root.addView(row,rp);}
    private void addGap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void addField(LinearLayout l,EditText e){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(49));p.topMargin=dp(7);l.addView(e,p);}
    private void addSpinner(LinearLayout l,Spinner s){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(5);l.addView(s,p);}
    private int selectedCount(){return countSpinner==null?2:countSpinner.getSelectedItemPosition()+2;}
    private boolean isLandscape(){return orientationSpinner!=null&&orientationSpinner.getSelectedItemPosition()==1;}
    private void pickImages(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGES);}
    private void takePhoto(){try{ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"ReLTV_"+System.currentTimeMillis()+".jpg");values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");cameraUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);startActivityForResult(i,TAKE_PHOTO);}catch(Exception e){toast("Kamera tidak dapat dibuka: "+e.getMessage());}}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK)return;
        if(req==PICK_IMAGES&&data!=null){if(data.getClipData()!=null){for(int x=0;x<data.getClipData().getItemCount();x++)addPhoto(data.getClipData().getItemAt(x).getUri());}else if(data.getData()!=null)addPhoto(data.getData());}
        if(req==TAKE_PHOTO&&cameraUri!=null)addPhoto(cameraUri);renderPhotos();saveDraft();}
    private void addPhoto(Uri u){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}photos.add(new PhotoItem(u,""));}
    private void renderPhotos(){if(photoList==null)return;photoList.removeAllViews();captionInputs.clear();if(photos.size()==0){TextView empty=text("Belum ada foto. Foto yang dipilih akan tampil di sini.",12,Color.GRAY);empty.setPadding(dp(4),dp(14),dp(4),dp(14));photoList.addView(empty);return;}
        for(int i=0;i<photos.size();i++){final int index=i;PhotoItem item=photos.get(i);LinearLayout c=card();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);photoList.addView(c,cp);
            TextView label=text(String.format(Locale.US,"FOTO %02d",i+1),12,teal);label.setTypeface(null,1);c.addView(label);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);try{image.setImageURI(item.uri);}catch(Exception ignored){}LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(185));ip.topMargin=dp(6);c.addView(image,ip);
            EditText caption=field("Keterangan foto "+(i+1));caption.setSingleLine(false);caption.setMinLines(2);caption.setText(item.caption);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(8);c.addView(caption,ep);captionInputs.add(caption);
            Button remove=button("HAPUS FOTO",0xFF9B3D43);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(42));rp.topMargin=dp(7);c.addView(remove,rp);remove.setOnClickListener(v->{syncCaptions();photos.remove(index);renderPhotos();saveDraft();});
        }
    }
    private void syncCaptions(){for(int i=0;i<Math.min(photos.size(),captionInputs.size());i++)photos.get(i).caption=captionInputs.get(i).getText().toString();}
    private void saveDraft(){syncCaptions();try{org.json.JSONArray arr=new org.json.JSONArray();for(PhotoItem p:photos){org.json.JSONObject o=new org.json.JSONObject();o.put("uri",p.uri.toString());o.put("caption",p.caption);o.put("scale",p.scale);arr.put(o);}org.json.JSONObject j=new org.json.JSONObject();j.put("title",titleInput.getText().toString());j.put("photos",arr);j.put("count",selectedCount());j.put("orientation",isLandscape()?"landscape":"portrait");getSharedPreferences("draft_v2",0).edit().putString("data",j.toString()).apply();savePdfSettings();}catch(Exception e){toast("Gagal menyimpan draf.");}}
    private void loadDraft(){try{String raw=getSharedPreferences("draft_v2",0).getString("data",null);if(raw==null)return;org.json.JSONObject o=new org.json.JSONObject(raw);titleInput.setText(o.optString("title"));countSpinner.setSelection(Math.max(0,Math.min(4,o.optInt("count",2)-2)));orientationSpinner.setSelection("landscape".equals(o.optString("orientation"))?1:0);loadPdfSettings();org.json.JSONArray a=o.optJSONArray("photos");if(a!=null)for(int i=0;i<a.length();i++){org.json.JSONObject p=a.getJSONObject(i);PhotoItem item=new PhotoItem(Uri.parse(p.getString("uri")),p.optString("caption"));item.scale=(float)Math.max(0.6,Math.min(1.4,p.optDouble("scale",1.0)));photos.add(item);}}catch(Exception ignored){}}

    private void showPreview(){
        syncCaptions();
        if(photos.size()==0){toast("Tambahkan foto terlebih dahulu untuk melihat preview.");return;}
        saveDraft();
        boolean landscape=isLandscape();int perPage=selectedCount();
        int pageCount=(int)Math.ceil((double)photos.size()/perPage);
        ScrollView sc=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),dp(12),dp(12),dp(12));content.setBackgroundColor(Color.rgb(238,242,246));sc.addView(content);
        for(int page=0;page<pageCount;page++){
            LinearLayout sheet=new LinearLayout(this);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(dp(12),dp(12),dp(12),dp(12));sheet.setBackground(shape(Color.WHITE,8));
            TextView title=text(titleInput.getText().toString().trim().isEmpty()?"Dokumentasi Kegiatan":titleInput.getText().toString().trim(),pdfTitleSize,ink);title.setTypeface(null,1);title.setGravity(Gravity.CENTER);title.setOnClickListener(v->showTitleEditor(title));sheet.addView(title);
            int start=page*perPage,end=Math.min(photos.size(),start+perPage);
            int cols=landscape?(perPage<=2?perPage:(perPage<=4?2:3)):(perPage<=2?1:2);
            LinearLayout row=null;
            for(int i=start;i<end;i++){
                if((i-start)%cols==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(8);sheet.addView(row,rp);}
                ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.FIT_CENTER);img.setBackgroundColor(Color.rgb(246,247,249));
                try{img.setImageURI(photos.get(i).uri);}catch(Exception ignored){}
                img.setScaleX(photos.get(i).scale);img.setScaleY(photos.get(i).scale);img.setOnClickListener(v->showPhotoEditor(index));
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(landscape?95:125),1);ip.setMargins(dp(3),dp(2),dp(3),dp(2));row.addView(img,ip);
            }
            LinearLayout.LayoutParams pageParams=new LinearLayout.LayoutParams(-1,-2);pageParams.bottomMargin=dp(12);content.addView(sheet,pageParams);
        }
        new AlertDialog.Builder(this).setTitle("Preview dokumen • "+pageCount+" halaman").setView(sc).setNegativeButton("Tutup",null).setNeutralButton("Edit tata letak",(d,w)->showLayoutEditor()).setPositiveButton("Simpan PDF",(d,w)->askPdfFileName()).show();
    }
    private Bitmap loadBitmap(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri)){Bitmap b=BitmapFactory.decodeStream(in);if(b==null)throw new Exception("Foto tidak dapat dibaca");return b;}}
    private void showLayoutEditor(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(8),dp(18),dp(4));
        addSetting(panel,"Ukuran judul (12–24)",12,24,pdfTitleSize,0);
        addSetting(panel,"Margin halaman (12–60)",12,60,pdfMargin,1);
        addSetting(panel,"Jarak antar foto (4–24)",4,24,pdfGap,2);
        addSetting(panel,"Ruang tepi foto (0–30)",0,30,pdfInset,3);
        addSetting(panel,"Jarak judul ke foto (8–50)",8,50,pdfTitleGap,4);
        new AlertDialog.Builder(this).setTitle("Edit tata letak PDF").setView(panel).setNegativeButton("Batal",null).setPositiveButton("Simpan pengaturan",(d,w)->{savePdfSettings();toast("Pengaturan tata letak disimpan.");}).show();
    }
    private void showTitleEditor(TextView previewTitle){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(10),dp(18),dp(6));
        TextView sample=text(titleInput.getText().toString().trim().isEmpty()?"Dokumentasi Kegiatan":titleInput.getText().toString().trim(),pdfTitleSize,ink);sample.setTypeface(null,1);sample.setGravity(Gravity.CENTER);panel.addView(sample);
        TextView value=text("Ukuran tulisan: "+pdfTitleSize,13,ink);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(12);panel.addView(value,vp);
        SeekBar bar=new SeekBar(this);bar.setMax(20);bar.setProgress(Math.max(0,Math.min(20,pdfTitleSize-12)));panel.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean fromUser){pdfTitleSize=12+progress;value.setText("Ukuran tulisan: "+pdfTitleSize);sample.setTextSize(pdfTitleSize);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        new AlertDialog.Builder(this).setTitle("Atur tulisan judul").setMessage("Geser untuk memperbesar atau memperkecil tulisan.").setView(panel).setNegativeButton("Batal",(d,w)->{loadPdfSettings();}).setPositiveButton("Terapkan",(d,w)->{previewTitle.setTextSize(pdfTitleSize);savePdfSettings();}).show();
    }
    private void showPhotoEditor(int index){
        if(index<0||index>=photos.size())return;
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(10),dp(18),dp(6));
        ImageView sample=new ImageView(this);sample.setScaleType(ImageView.ScaleType.FIT_CENTER);try{sample.setImageURI(photos.get(index).uri);}catch(Exception ignored){}
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(150));panel.addView(sample,ip);
        TextView value=text("Ukuran foto: "+Math.round(photos.get(index).scale*100)+"%",13,ink);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(10);panel.addView(value,vp);
        SeekBar bar=new SeekBar(this);bar.setMax(80);bar.setProgress(Math.max(0,Math.min(80,Math.round((photos.get(index).scale-0.6f)*100))));panel.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean fromUser){float z=0.6f+progress/100f;sample.setScaleX(z);sample.setScaleY(z);value.setText("Ukuran foto: "+Math.round(z*100)+"%");}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        new AlertDialog.Builder(this).setTitle("Atur ukuran foto "+(index+1)).setMessage("Geser ke kiri untuk mengecilkan dan ke kanan untuk memperbesar.").setView(panel).setNeutralButton("Reset",(d,w)->{photos.get(index).scale=1f;saveDraft();showPreview();}).setNegativeButton("Batal",null).setPositiveButton("Terapkan",(d,w)->{photos.get(index).scale=0.6f+bar.getProgress()/100f;saveDraft();showPreview();}).show();
    }
    private void addSetting(LinearLayout panel,String label,int min,int max,int value,int type){
        TextView tv=text(label+" : "+value,13,ink);panel.addView(tv);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(value-min);panel.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){int v=min+progress;tv.setText(label+" : "+v);switch(type){case 0:pdfTitleSize=v;break;case 1:pdfMargin=v;break;case 2:pdfGap=v;break;case 3:pdfInset=v;break;case 4:pdfTitleGap=v;break;}}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private void savePdfSettings(){getSharedPreferences("draft_v2",0).edit().putInt("pdfTitleSize",pdfTitleSize).putInt("pdfMargin",pdfMargin).putInt("pdfGap",pdfGap).putInt("pdfInset",pdfInset).putInt("pdfTitleGap",pdfTitleGap).apply();}
    private void loadPdfSettings(){android.content.SharedPreferences p=getSharedPreferences("draft_v2",0);pdfTitleSize=p.getInt("pdfTitleSize",18);pdfMargin=p.getInt("pdfMargin",24);pdfGap=p.getInt("pdfGap",10);pdfInset=p.getInt("pdfInset",0);pdfTitleGap=p.getInt("pdfTitleGap",20);}
    private void askPdfFileName(){
        if(photos.size()==0){toast("Tambahkan sekurang-kurangnya satu foto.");return;}
        EditText name=field("Contoh: Dokumentasi Kegiatan");
        name.setText("Dokumentasi_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date()));
        new AlertDialog.Builder(this).setTitle("Nama file PDF").setMessage("Masukkan nama file sebelum disimpan.").setView(name).setNegativeButton("Batal",null).setPositiveButton("Simpan PDF",(d,w)->{
            String filename=name.getText().toString().trim();if(filename.isEmpty()){toast("Nama file belum diisi.");return;}createPdf(filename);
        }).show();
    }
    private void createPdf(String requestedName){syncCaptions();if(photos.size()==0){toast("Tambahkan sekurang-kurangnya satu foto.");return;}saveDraft();PdfDocument doc=new PdfDocument();int pageNo=0;boolean landscape=isLandscape();final int W=landscape?842:595,H=landscape?595:842;int perPage=selectedCount();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        try{for(int start=0;start<photos.size();start+=perPage){pageNo++;PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pageNo).create());Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
            p.setColor(Color.rgb(25,43,67));p.setTextSize(pdfTitleSize);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));String heading=titleInput.getText().toString().trim();if(heading.isEmpty())heading="Dokumentasi Kegiatan";while(p.measureText(heading)>W-(pdfMargin*2)&&p.getTextSize()>9)p.setTextSize(p.getTextSize()-1);float titleWidth=p.measureText(heading);float titleBaseline=Math.max(pdfMargin+pdfTitleSize,(pdfMargin+pdfTitleSize));c.drawText(heading,Math.max(pdfMargin,(W-titleWidth)/2),titleBaseline,p);
            int cols,rows;if(landscape){cols=perPage<=2?perPage:(perPage<=4?2:3);rows=(int)Math.ceil((double)perPage/cols);}else{cols=perPage<=2?1:2;rows=(int)Math.ceil((double)perPage/cols);}
            float left=pdfMargin,top=pdfMargin+pdfTitleSize+pdfTitleGap,right=W-pdfMargin,bottom=H-pdfMargin,gap=pdfGap;float cellW=(right-left-gap*(cols-1))/cols;float cellH=(bottom-top-gap*(rows-1))/rows;
            for(int slot=0;slot<perPage;slot++){int idx=start+slot;if(idx>=photos.size())break;int col=slot%cols,row=slot/cols;float x=left+col*(cellW+gap),y=top+row*(cellH+gap);
                Bitmap bmp=loadBitmap(photos.get(idx).uri);float inset=Math.min(pdfInset,Math.min(cellW,cellH)/4f);drawFit(c,bmp,x+inset,y+inset,Math.max(1,cellW-2*inset),Math.max(1,cellH-2*inset),photos.get(idx).scale,p);bmp.recycle();
            }
            doc.finishPage(page);}
            String safeName=requestedName.replaceAll("[\\\\/:*?\\\"<>|]","_").trim();if(!safeName.toLowerCase(Locale.ROOT).endsWith(".pdf"))safeName+=".pdf";String filename=safeName;ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,filename);values.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/ReL TV Documentation");Uri out=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(out==null)throw new Exception("Tidak dapat membuat fail PDF");try(OutputStream os=getContentResolver().openOutputStream(out)){doc.writeTo(os);}toast("PDF berjaya disimpan dalam Download/ReL TV Documentation.");
        }catch(Exception e){toast("PDF gagal dibuat: "+e.getMessage());}finally{doc.close();}}
    private void drawFit(Canvas c,Bitmap b,float x,float y,float w,float h,float zoom,Paint p){float base=Math.min(w/b.getWidth(),h/b.getHeight());float factor=base*Math.max(0.6f,Math.min(1.4f,zoom));float dw=b.getWidth()*factor,dh=b.getHeight()*factor;float dx=x+(w-dw)/2,dy=y+(h-dh)/2;p.setColor(Color.WHITE);c.drawRect(x,y,x+w,y+h,p);int save=c.save();c.clipRect(x,y,x+w,y+h);p.setColor(Color.DKGRAY);c.drawBitmap(b,null,new RectF(dx,dy,dx+dw,dy+dh),p);c.restoreToCount(save);}
    private void drawWrapped(Canvas c,String value,float x,float y,float width,float height,Paint p){if(value==null||value.trim().isEmpty())value="Tiada keterangan.";String[] words=value.split("\\s+");String line="";float yy=y,step=12;for(String word:words){String test=line.isEmpty()?word:line+" "+word;if(p.measureText(test)>width){if(yy>y+height-step)break;if(!line.isEmpty())c.drawText(line,x,yy,p);line=word;yy+=step;}else line=test;}if(!line.isEmpty()&&yy<=y+height)c.drawText(line,x,yy,p);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
