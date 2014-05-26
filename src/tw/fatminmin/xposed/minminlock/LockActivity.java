package tw.fatminmin.xposed.minminlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class LockActivity extends Activity {
    
    private SharedPreferences pref, passPref;
    private String requestPkg;
    private Handler handler = new Handler();
    private AlertDialog dialog;
    
    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        pref = getSharedPreferences(getPackageName() + "_preferences", MODE_WORLD_READABLE);
        passPref = getSharedPreferences("password", MODE_PRIVATE);
        
        requestPkg = getIntent().getStringExtra(Common.KEY_APP_ACCESS);
        
        
        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(requestPkg);
        
        askPassword();
        
    }
    
    @Override
    protected void onDestroy() {
        super.onPause();
        pref.edit()
            .putBoolean(requestPkg + "_tmp", false)
            .commit();
        if(dialog != null) {
            dialog.cancel();
        }
        
    }
    
    private void askPassword() {
        final EditText input = new EditText(LockActivity.this);
        
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        dialog = new AlertDialog.Builder(LockActivity.this)
                                    .setCancelable(false)
                                    .setTitle("Password")
                                    .setView(input)
                                    .setPositiveButton("Ok", null)
                                    .create();
        dialog.show();
        
        ((ViewGroup)input.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = input.getText().toString();
                if(val.equals(passPref.getString(Common.KEY_PASSWORD, ""))) {
                    dialog.dismiss();
                    pref.edit()
                        .putBoolean(requestPkg + "_tmp", true)
                        .commit();
                    
                    ActivityManager am = (ActivityManager) LockActivity.this.getSystemService(Activity.ACTIVITY_SERVICE);
                    am.killBackgroundProcesses(requestPkg);
                    Intent it = LockActivity.this.getPackageManager().getLaunchIntentForPackage(requestPkg);
                    it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    LockActivity.this.startActivity(it);
                    
                    handler.postDelayed(new Runnable() {
                        
                        @Override
                        public void run() {
                            pref.edit()
                                .putBoolean(requestPkg + "_tmp", false)
                                .commit();
                        }
                    }, 10000);
                }
                else {
                    input.setText("");
                }
            }
        });
    }
}
