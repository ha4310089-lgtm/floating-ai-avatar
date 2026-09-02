package com.example.floatingaiavatar;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class FloatingAvatarService extends Service {
    private WindowManager wm;
    private View avatar;
    private WindowManager.LayoutParams params;
    private SpeechRecognizer sr;
    private TextToSpeech tts;
    private static final String KEY = "";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("a", "A", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            startForeground(1, new Notification.Builder(this, "a").setContentTitle("AI Avatar Active").setSmallIcon(android.R.drawable.btn_star_big_on).build());
        }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        avatar = new AvatarView(this);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(160, 160, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START; params.x = 50; params.y = 250;
        wm.addView(avatar, params);

        tts = new TextToSpeech(this, s -> { if (s == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("hi", "IN")); });
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {} public void onBeginningOfSpeech() {} public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {} public void onEndOfSpeech() {} public void onError(int e) {}
            public void onResults(Bundle r) {
                ArrayList<String> m = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty()) ask(m.get(0));
            }
            public void onPartialResults(Bundle p) {} public void onEvent(int t, Bundle p) {}
        });
    }

    private void ask(String q) {
        new Thread(() -> {
            try {
                URL u = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + KEY);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);

                JSONObject part = new JSONObject();
                part.put("text", "You are a mobile voice assistant. Reply briefly in Hindi. User: " + q);
                JSONArray parts = new JSONArray();
                parts.put(part);
                JSONObject content = new JSONObject();
                content.put("parts", parts);
                JSONArray contents = new JSONArray();
                contents.put(content);
                JSONObject root = new JSONObject();
                root.put("contents", contents);

                byte[] out = root.toString().getBytes("UTF-8");
                c.getOutputStream().write(out);

                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();

                JSONObject res = new JSONObject(sb.toString());
                String reply = res.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                new Handler(Looper.getMainLooper()).post(() -> tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null));
            } catch (Exception ignored) {}
        }).start();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    class AvatarView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int ix, iy; float tx, ty;
        public AvatarView(Context c) { super(c); }
        protected void onDraw(Canvas c) {
            float cx = getWidth()/2f, cy = getHeight()/2f;
            p.setColor(Color.parseColor("#00F2FE")); c.drawCircle(cx, cy, cx-4, p);
            p.setColor(Color.BLACK); c.drawCircle(cx-18, cy-8, 6, p); c.drawCircle(cx+18, cy-8, 6, p);
            c.drawCircle(cx, cy+14, 8, p);
        }
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { ix = params.x; iy = params.y; tx = e.getRawX(); ty = e.getRawY(); return true; }
            if (e.getAction() == MotionEvent.ACTION_MOVE) { params.x = ix + (int)(e.getRawX() - tx); params.y = iy + (int)(e.getRawY() - ty); wm.updateViewLayout(this, params); return true; }
            if (e.getAction() == MotionEvent.ACTION_UP && Math.abs(e.getRawX()-tx)<15 && Math.abs(e.getRawY()-ty)<15) {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
                sr.startListening(i);
                return true;
            }
            return false;
        }
    }
}
