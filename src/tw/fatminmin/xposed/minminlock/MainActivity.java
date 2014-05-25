package tw.fatminmin.xposed.minminlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
    
    static final String PASSWORD_KEY = "password";
    SharedPreferences pref;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pref = getSharedPreferences(PASSWORD_KEY, MODE_PRIVATE);
        
        if(pref.getString("password", "").length() == 0) {
            setPassword();
        }
        else {
            askPassword();
        }
        
        if(savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PlaceholderFragment())
                .commit();
        }
    }
    private void askPassword() {
        final EditText input = new EditText(MainActivity.this);
        
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
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
                if(val.equals(pref.getString(PASSWORD_KEY, ""))) {
                    dialog.dismiss();
                }
                else {
                    input.setText("");
                }
            }
        });
    }
    private void setPassword() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        final View rootView = inflater.inflate(R.layout.set_password, null);
        
        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("Set Password")
                    .setView(rootView)
                    .setPositiveButton("Ok", null)
                    .setNegativeButton("Cancel", null)
                    .create();
        dialog.show();
        ((ViewGroup)rootView.getParent()).setPadding(10, 10, 10, 10);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                EditText p1 = (EditText) rootView.findViewById(R.id.edt_password);
                EditText p2 = (EditText) rootView.findViewById(R.id.edt_re_password);
                String v1 = p1.getText().toString();
                String v2 = p2.getText().toString();
                
                if(!v1.equals(v2)) {
                    p1.setText("");
                    p2.setText("");
                    Toast.makeText(MainActivity.this, R.string.msg_password_inconsistent, Toast.LENGTH_SHORT)
                        .show();
                }
                else if(v1.length() == 0) {
                    Toast.makeText(MainActivity.this, R.string.msg_password_null, Toast.LENGTH_SHORT)
                        .show();
                }
                else {
                    dialog.dismiss();
                    pref.edit()
                        .putString(PASSWORD_KEY, v1)
                        .commit();
                    Toast.makeText(MainActivity.this, R.string.msg_password_changed, Toast.LENGTH_SHORT)
                        .show();
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(pref.getString(PASSWORD_KEY, "").length() == 0)) {
                    dialog.dismiss();
                }
                else {
                    Toast.makeText(MainActivity.this, R.string.msg_password_inconsistent, Toast.LENGTH_SHORT)
                        .show();
                }
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_set_password) {
            setPassword();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends PreferenceFragment {
        
        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.preferences);
            
            setupAppList();
            
        }
        
        
        private void setupAppList() {

            PreferenceGroup pc = (PreferenceGroup) findPreference("main");


            Context activity = getActivity();

            PackageManager pm = activity.getPackageManager();
            List<ApplicationInfo> list = pm.getInstalledApplications(0);


            List<Preference> prefList = new ArrayList<Preference>();
            for(ApplicationInfo info : list) {

                if((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    CheckBoxPreference cp = new CheckBoxPreference(activity);
                    cp.setTitle(pm.getApplicationLabel(info));
                    cp.setKey(info.packageName);
                    cp.setIcon(pm.getApplicationIcon(info));

                    prefList.add(cp);
                }
            }

            Collections.sort(prefList, new Comparator<Preference>() {
                @Override
                public int compare(Preference lhs, Preference rhs) {
                    return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
                }

            });

            for(Preference pref : prefList) {
                pc.addPreference(pref);
            }

        }
    }

}
