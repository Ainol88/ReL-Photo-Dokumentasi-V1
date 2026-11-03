
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
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private static final int PICK_IMAGES = 101;
    private static final int TAKE_PHOTO = 102;

    private EditText titleInput, filenameInput;
    private LinearLayout photoList;
    private LinearLayout root;

    private final ArrayList<PhotoItem> photos = new ArrayList<>();
    private Uri cameraUri;

    static class PhotoItem {
        Uri uri;
        int rotation;

        PhotoItem(Uri u, int r) {
            uri = u;
            rotation = r;
        }
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUI();
        loadDraft();
        renderPhotos();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(bg(Color.rgb(245,247,250), 12));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(248,250,252));

        scroll.addView(root);
        setContentView(scroll);

        TextView heading = text(
            "ReL PHOTO DOKUMENTASI",
            21,
            Color.WHITE
        );

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(20), dp(18), dp(20));
        hero.setBackground(bg(Color.rgb(20,40,63), 18));
        hero.addView(heading);

        root.addView(hero);

        addGap(root, 16);

        root.addView(text(
            "Judul dokumentasi",
            14,
            Color.rgb(23,107,135)
        ));

        titleInput = field("Masukkan judul dokumentasi");
        addField(titleInput);

        addGap(root, 12);

        root.addView(text(
            "Nama file PDF",
            14,
            Color.rgb(23,107,135)
        ));

        filenameInput = field("Masukkan nama file PDF");
        addField(filenameInput);

        addGap(root, 16);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button gallery = button("＋ Galeri");
        Button camera = button("＋ Kamera");

        actions.addView(
            gallery,
            new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        actions.addView(
            camera,
            new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        root.addView(actions);

        gallery.setOnClickListener(v -> pickImages());
        camera.setOnClickListener(v -> takePhoto());

        photoList = new LinearLayout(this);
        photoList.setOrientation(LinearLayout.VERTICAL);
        root.addView(photoList);

        addGap(root, 16);

        Button save = button("Simpan Projek");
        Button pdf = button("BUAT PDF");

        save.setOnClickListener(v -> {
            saveDraft();
            toast("Projek berhasil disimpan.");
        });

        pdf.setOnClickListener(v -> createPdf());

        root.addView(
            save,
            new LinearLayout.LayoutParams(-1, dp(48))
        );

        root.addView(
            pdf,
            new LinearLayout.LayoutParams(-1, dp(54))
        );
    }

    private void addGap(LinearLayout l, int h) {
        View v = new View(this);
        l.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private void addField(EditText e) {
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, dp(50));

        p.topMargin = dp(8);
        root.addView(e, p);
    }

    private void pickImages() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, PICK_IMAGES);
    }

    private void takePhoto() {
        try {
            ContentValues values = new ContentValues();
            values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ReLTV_" + System.currentTimeMillis() + ".jpg"
            );
            values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            );

            cameraUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            );

            if (cameraUri == null) {
                throw new Exception("Tidak dapat membuat lokasi foto");
            }

            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            startActivityForResult(i, TAKE_PHOTO);

        } catch (Exception e) {
            toast("Kamera tidak dapat dibuka: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(
        int req,
        int res,
        Intent data
    ) {
        super.onActivityResult(req, res, data);

        if (res != RESULT_OK) return;

        if (req == PICK_IMAGES && data != null) {
            if (data.getClipData() != null) {
                for (int x = 0;
                     x < data.getClipData().getItemCount();
                     x++) {
                    addPhoto(
                        data.getClipData().getItemAt(x).getUri()
                    );
                }
            } else if (data.getData() != null) {
                addPhoto(data.getData());
            }
        }

        if (req == TAKE_PHOTO && cameraUri != null) {
            addPhoto(cameraUri);
        }

        renderPhotos();
        saveDraft();
    }

    private void addPhoto(Uri u) {
        try {
            getContentResolver().takePersistableUriPermission(
                u,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {}

        photos.add(new PhotoItem(u, 0));
    }

    private Bitmap loadBitmap(Uri uri) throws Exception {
        try (InputStream in =
                 getContentResolver().openInputStream(uri)) {

            Bitmap b = BitmapFactory.decodeStream(in);

            if (b == null) {
                throw new Exception("Foto tidak dapat dibaca");
            }

            return b;
        }
    }

    private Bitmap rotateBitmap(Bitmap source, int degrees) {
        if (degrees == 0) return source;

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.getWidth(),
            source.getHeight(),
            matrix,
            true
        );
    }

    private void renderPhotos() {
        if (photoList == null) return;

        photoList.removeAllViews();

        for (int i = 0; i < photos.size(); i++) {
            final int index = i;
            PhotoItem item = photos.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            card.setBackground(bg(Color.WHITE, 14));

            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);

            cp.topMargin = dp(10);
            photoList.addView(card, cp);

            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);

            try {
                Bitmap original = loadBitmap(item.uri);
                Bitmap rotated = rotateBitmap(original, item.rotation);

                image.setImageBitmap(rotated);

                if (rotated != original) {
                    original.recycle();
                }
            } catch (Exception e) {
                image.setImageURI(item.uri);
            }

            card.addView(
                image,
                new LinearLayout.LayoutParams(-1, dp(210))
            );

            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.HORIZONTAL);

            Button rotate = button("↻ Putar 90°");
            Button remove = button("Hapus");

            controls.addView(
                rotate,
                new LinearLayout.LayoutParams(0, dp(48), 1)
            );

            controls.addView(
                remove,
                new LinearLayout.LayoutParams(0, dp(48), 1)
            );

            card.addView(controls);

            rotate.setOnClickListener(v -> {
                photos.get(index).rotation =
                    (photos.get(index).rotation + 90) % 360;

                renderPhotos();
                saveDraft();
            });

            remove.setOnClickListener(v -> {
                photos.remove(index);
                renderPhotos();
                saveDraft();
            });
        }
    }

    private void saveDraft() {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();

            for (PhotoItem p : photos) {
                org.json.JSONObject o =
                    new org.json.JSONObject();

                o.put("uri", p.uri.toString());
                o.put("rotation", p.rotation);
                arr.put(o);
            }

            org.json.JSONObject rootJ =
                new org.json.JSONObject();

            rootJ.put("title", titleInput.getText().toString());
            rootJ.put("filename", filenameInput.getText().toString());
            rootJ.put("photos", arr);

            getSharedPreferences("draft", 0)
                .edit()
                .putString("data", rootJ.toString())
                .apply();

        } catch (Exception e) {
            toast("Gagal menyimpan projek.");
        }
    }

    private void loadDraft() {
        try {
            String raw = getSharedPreferences("draft", 0)
                .getString("data", null);

            if (raw == null) return;

            org.json.JSONObject o =
                new org.json.JSONObject(raw);

            titleInput.setText(o.optString("title"));
            filenameInput.setText(o.optString("filename"));

            org.json.JSONArray a = o.optJSONArray("photos");

            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    org.json.JSONObject p = a.getJSONObject(i);

                    photos.add(new PhotoItem(
                        Uri.parse(p.getString("uri")),
                        p.optInt("rotation", 0)
                    ));
                }
            }

        } catch (Exception ignored) {}
    }

    private String cleanFilename(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "ReLTV_Dokumentasi";
        }

        name = name.trim().replaceAll(
            "[\\\\/:*?\"<>|]",
            "_"
        );

        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            name = name.substring(0, name.length() - 4);
        }

        return name + ".pdf";
    }

    private void createPdf() {
        if (photos.size() == 0) {
            toast("Tambahkan foto terlebih dahulu.");
            return;
        }

        if (titleInput.getText().toString().trim().isEmpty()) {
            titleInput.setError("Masukkan judul dokumentasi");
            return;
        }

        saveDraft();

        PdfDocument doc = new PdfDocument();

        final int W = 595;
        final int H = 842;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        try {
            int pageNo = 0;

            for (int start = 0;
                 start < photos.size();
                 start += 2) {

                pageNo++;

                PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(
                        W, H, pageNo
                    ).create();

                PdfDocument.Page page = doc.startPage(info);
                Canvas c = page.getCanvas();

                c.drawColor(Color.WHITE);

                String title = titleInput.getText().toString().trim();

                paint.setColor(Color.BLACK);
                paint.setTextSize(20);
                paint.setTypeface(
                    Typeface.create("sans-serif", Typeface.BOLD)
                );
                paint.setTextAlign(Paint.Align.CENTER);

                float titleWidth = paint.measureText(title);

                if (titleWidth > 515) {
                    paint.setTextSize(
                        Math.max(10, 20 * 515 / titleWidth)
                    );
                }

                c.drawText(title, W / 2f, 55, paint);

                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setFilterBitmap(true);
                paint.setDither(true);

                for (int slot = 0; slot < 2; slot++) {
                    int idx = start + slot;

                    if (idx >= photos.size()) break;

                    PhotoItem item = photos.get(idx);

                    Bitmap original = loadBitmap(item.uri);
                    Bitmap rotated = rotateBitmap(
                        original,
                        item.rotation
                    );

                    int boxX = 40;
                    int boxY = slot == 0 ? 95 : 455;
                    int boxW = 515;
                    int boxH = 330;

                    float scale = Math.min(
                        (float) boxW / rotated.getWidth(),
                        (float) boxH / rotated.getHeight()
                    );

                    int dw = Math.max(
                        1,
                        (int)(rotated.getWidth() * scale)
                    );

                    int dh = Math.max(
                        1,
                        (int)(rotated.getHeight() * scale)
                    );

                    int left = boxX + (boxW - dw) / 2;
                    int top = boxY + (boxH - dh) / 2;

                    Rect dst = new Rect(
                        left,
                        top,
                        left + dw,
                        top + dh
                    );

                    c.drawBitmap(rotated, null, dst, paint);

                    if (rotated != original) {
                        rotated.recycle();
                    }

                    original.recycle();
                }

                doc.finishPage(page);
            }

            String filename = cleanFilename(
                filenameInput.getText().toString()
            );

            ContentValues values = new ContentValues();

            values.put(
                MediaStore.Downloads.DISPLAY_NAME,
                filename
            );

            values.put(
                MediaStore.Downloads.MIME_TYPE,
                "application/pdf"
            );

            values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                "Download/ReL TV Documentation"
            );

            Uri out = getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            );

            if (out == null) {
                throw new Exception("Tidak dapat membuat fail PDF");
            }

            try (OutputStream os =
                     getContentResolver().openOutputStream(out)) {
                doc.writeTo(os);
            }

            toast(
                "PDF berhasil disimpan: " + filename
            );

        } catch (Exception e) {
            toast("PDF gagal dibuat: " + e.getMessage());

        } finally {
            doc.close();
        }
    }

    private void toast(String s) {
        Toast.makeText(
            this,
            s,
            Toast.LENGTH_LONG
        ).show();
    }
}
