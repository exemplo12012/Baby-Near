package pi.iesb.br.babynear.nfc;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import pi.iesb.br.babynear.R;

public class NFCWriteFragment extends DialogFragment {

  public static final String TAG = NFCWriteFragment.class.getSimpleName();

  public static NFCWriteFragment newInstance() {

    return new NFCWriteFragment();
  }

  private TextView mTvMessage;
  private ProgressBar mProgress;
  private Listener mListener;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_write,container,false);
    initViews(view);
    return view;
  }

  private void initViews(View view) {

    mTvMessage = view.findViewById(R.id.tv_message);
    mProgress = view.findViewById(R.id.progress);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mListener = (WriteTagActivity)context;
    mListener.onDialogDisplayed();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener.onDialogDismissed();
  }

  public void onNfcDetected(Ndef ndef, String messageToWrite){

    mProgress.setVisibility(View.VISIBLE);
    writeToNfc(ndef,messageToWrite);
  }

  private void writeToNfc(Ndef ndef, String message){

    mTvMessage.setText(getString(R.string.message_write_progress));

    byte[] lang = Locale.getDefault().getLanguage().getBytes(StandardCharsets.UTF_8);
    byte[] text = message.getBytes(StandardCharsets.UTF_8); // Content in UTF-8
    int langSize = lang.length;
    int textLength = text.length;
    ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + langSize + textLength);
    payload.write((byte) (langSize & 0x1F));
    payload.write(lang, 0, langSize);
    payload.write(text, 0, textLength);
    if (ndef != null) {

      try {
        ndef.connect();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
          NdefRecord.RTD_TEXT, new byte[0],
          payload.toByteArray());
        ndef.writeNdefMessage(new NdefMessage(textRecord));
        ndef.close();
        //Write Successful
        mTvMessage.setText(getString(R.string.message_write_success));

      } catch (IOException | FormatException e) {
        e.printStackTrace();
        mTvMessage.setText(getString(R.string.message_write_error));

      } finally {
        mProgress.setVisibility(View.GONE);
      }

    }
  }
}
