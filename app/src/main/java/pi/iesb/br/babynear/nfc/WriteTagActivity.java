package pi.iesb.br.babynear.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import pi.iesb.br.babynear.R;

public class WriteTagActivity extends AppCompatActivity implements Listener{

  public static final String TAG = WriteTagActivity.class.getSimpleName();

  private EditText mEtMessage;
  private Button mBtWrite;

  private NFCWriteFragment mNfcWriteFragment;
  private String message;

  private boolean isDialogDisplayed = false;
  private boolean isWrite = false;

  private NfcAdapter mNfcAdapter;

  SharedPreferences settings;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_write);

    initViews();
    initNFC();
  }

  private void initViews() {

    mEtMessage = findViewById(R.id.et_message);
    mBtWrite = findViewById(R.id.btn_write);

    settings = PreferenceManager.getDefaultSharedPreferences(this);
    message = settings.getString("texto_tag", "");
    mEtMessage.setText(message);

    mBtWrite.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick (View view) {
        WriteTagActivity.this.showWriteFragment();
      }
    });
  }

  private void initNFC(){

    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
  }

  private void showWriteFragment() {

    isWrite = true;

    mNfcWriteFragment = (NFCWriteFragment) getSupportFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);

    if (mNfcWriteFragment == null) {

      mNfcWriteFragment = NFCWriteFragment.newInstance();
    }
    mNfcWriteFragment.show(getSupportFragmentManager(),NFCWriteFragment.TAG);

  }

  @Override
  public void onDialogDisplayed() {

    isDialogDisplayed = true;
  }

  @Override
  public void onDialogDismissed() {

    isDialogDisplayed = false;
    isWrite = false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
    IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
    IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

    PendingIntent pendingIntent = PendingIntent.getActivity(
      this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    if(mNfcAdapter!= null)
      mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

  }

  @Override
  protected void onPause() {
    super.onPause();
    if(mNfcAdapter!= null)
      mNfcAdapter.disableForegroundDispatch(this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

    Log.d(TAG, "onNewIntent: "+intent.getAction());

    if(tag != null) {
      Ndef ndef = Ndef.get(tag);

      if (isDialogDisplayed) {

        if (isWrite) {

          String messageToWrite = mEtMessage.getText().toString();
          SharedPreferences.Editor editor = settings.edit();
          editor.putString("texto_tag", messageToWrite);
          editor.apply();
          mNfcWriteFragment = (NFCWriteFragment) getSupportFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);
          mNfcWriteFragment.onNfcDetected(ndef,messageToWrite);

        }
      }
    }
  }
}
