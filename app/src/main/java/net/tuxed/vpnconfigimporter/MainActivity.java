package net.tuxed.vpnconfigimporter;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startDownload = (Button) findViewById(R.id.importButton);
        startDownload.setOnClickListener(this);

        Toolbar t = (Toolbar) findViewById(R.id.toolbar);
        t.setTitle("eduvpn");
        Button scanCode = (Button) findViewById(R.id.scanButton);
        scanCode.setOnClickListener(this);

    }

    public void onClick(View v) {
        if (R.id.importButton == v.getId()) {
            try {
                DownloadFilesTask d = new DownloadFilesTask();
                EditText vpnUrl = (EditText) findViewById(R.id.vpnUrl);
                EditText userName = (EditText) findViewById(R.id.userName);
                EditText userPass = (EditText) findViewById(R.id.userPass);
                EditText configName = (EditText) findViewById(R.id.configName);

                String[] s = {vpnUrl.getText().toString(), userName.getText().toString(), userPass.getText().toString(), configName.getText().toString()};

                d.execute(s);//new URL("https://vpn.tuxed.net/vpn-user-portal/api/config"));
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage());
            }
        }

        if (R.id.scanButton == v.getId()) {
            try {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                startActivityForResult(intent, 0);
            } catch (Exception e) {

                Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                startActivity(marketIntent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                Log.i("MainActivity", contents);

                Uri u = Uri.parse(contents);

                EditText vpnUrl = (EditText) findViewById(R.id.vpnUrl);
                vpnUrl.setText(u.getScheme() + "://" + u.getAuthority() + u.getPath());
                EditText userName = (EditText) findViewById(R.id.userName);
                userName.setText(u.getQueryParameter("userName"));
                EditText userPass = (EditText) findViewById(R.id.userPass);
                userPass.setText(u.getQueryParameter("userPass"));
                EditText configName = (EditText) findViewById(R.id.configName);
                String strConfigName = u.getQueryParameter("configName");
                if (null == strConfigName) {
                    // generate random name
                    strConfigName = "android_" + this.rnd();
                }
                configName.setText(strConfigName);
            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class DownloadFilesTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            Downloader d = new Downloader();
            return d.downloadFile(s[0], s[1], s[2], s[3]);
        }

        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        public File getConfigStorageDir(String vpnName) {
            // Get the directory for the user's public downloads directory.
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), vpnName);
            if (!file.mkdirs()) {
                Log.e("MainActivity", "Directory not created");
            }
            return file;
        }

        protected void onPostExecute(String s) {
            File f = new File(this.getConfigStorageDir("VPN"), "Android.ovpn");
            try {

                FileWriter fw = new FileWriter(f);

                fw.write(s);
                fw.close();
                Log.i("MainActivity", "file " + f.getAbsolutePath() + " written");

                Uri foo = Uri.parse(f.getAbsolutePath());
                try {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(f), "application/x-openvpn-profile");
                    startActivity(intent);
                } catch (Exception e) {
                    // failed to open file, possibly user does not have OpenVPN app installed,
                    // could it also be user cancel?!
                    Uri marketUri = Uri.parse("market://details?id=net.openvpn.openvpn");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                    startActivity(marketIntent);
                }
            } catch (IOException e) {
                Log.e("MainActivity", "unable to write file " + e.getMessage());
            }
        }
    }

    public String rnd() {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (
                int i = 0;
                i < 5; i++)

        {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }

        String output = sb.toString();
        return output;
    }
}
