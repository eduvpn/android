package net.tuxed.vpnconfigimporter;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.security.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Intent intent = getIntent();
        Uri u = intent.getData();
        Log.i("RegisterActivity", u.toString());

        String fragment = u.getFragment();

        String[] f = fragment.split("&");

        String accessToken = null;

        for(String x : f) {
            String[] kv = x.split("=");
            if(kv[0].equals("access_token")) {
                // found access token
                accessToken = kv[1];
            }
        }

        if(null == accessToken) {
            // accessToken not found, FIXME handle this
        }

        TextView t = (TextView) findViewById(R.id.callbackUri);
        t.setText(accessToken);


//        EditText intentUri = (EditText) findViewById(R.id.intentUri);
//        intentUri.setText(fragment.toString());

        //String code = u.getQueryParameter("code");
        //String state = u.getQueryParameter("state");

        // FIXME: first exchange code for access token!
        // FIXME: come up with a good way to determine a configuration name...

        //String accessToken = "DEFINITELY_NOT_VALID";
        String configName = "Android_" + System.currentTimeMillis() / 1000L;

        // get the URL from the persistent storage, we do not want it hard coded here
        String newU = "https://vpn.tuxed.net/portal/api/config";

        String[] s = {newU, accessToken, configName};

        DownloadFilesTask d = new DownloadFilesTask(configName);
//
        d.execute(s);//new URL("https://vpn.tuxed.net/vpn-user-portal/api/config"));
    }

    private class DownloadFilesTask extends AsyncTask<String, Void, String> {
        private String configName;

        public DownloadFilesTask(String configName) {
            this.configName = configName;
        }

        protected String doInBackground(String... s) {
            Downloader d = new Downloader();
            return d.downloadFile(s[0], s[1], s[2]);
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
                Log.e("RegisterActivity", "Directory not created");
            }
            return file;
        }

        protected void onPostExecute(String s) {
            File f = new File(this.getConfigStorageDir("VPN"), this.configName + ".ovpn");
            try {

                // FIXME we need to extract the private key and certificate and store it in the
                // local key store, the CA and tls-auth can remain in the config, not so bad
                // if they get leaked
                //http://nelenkov.blogspot.de/2011/11/using-ics-keychain-api.html
                //http://android-developers.blogspot.de/2012/03/unifying-key-store-access-in-ics.html

                //Security.getProvider
//                KeyStore ks = KeyStore.getInstance("PKCS12");
//
//                byte[] privateKey = {0};
//
//                InputStream inStream = null;
//                X509Certificate cert = null;
//
//                try {
//                    //inStream = new FileInputStream("fileName-of-cert");
//                    String certData = "XYZ";
//                    inStream = new ByteArrayInputStream(certData.getBytes());
//                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
//                    cert = (X509Certificate)cf.generateCertificate(inStream);
//
//                } finally {
//                    if (inStream != null) {
//                        inStream.close();
//                    }
//                }
//
//                X509Certificate certList[] = {cert};
//

//                ks.setKeyEntry("foo", privateKey, certList);
//
//                ByteArrayOutputStream bos = null;
//
//                ks.store(bos, null);

                Log.e("RegisterActivity", s);

                FileWriter fw = new FileWriter(f);

                fw.write(s);
                fw.close();
                Log.i("RegisterActivity", "file " + f.getAbsolutePath() + " written");

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
//            } catch (KeyStoreException e) {
//                Log.e("RegisterActivity", e.getMessage());
            } catch (IOException e) {
                Log.e("MainActivity", "unable to write file " + e.getMessage());
            }catch(Exception e){
                Log.e("RegisterActivity", e.getMessage());
            }
        }
    }
}