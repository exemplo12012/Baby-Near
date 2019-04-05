package pi.iesb.br.babynear;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

/**
 * Created by henrique on 14/11/2017.
 */

public class ConectadaActivity extends AppCompatActivity implements Serializable {

  private static final long serialVersionUID = 5890514926067136397L;
  Button btn_voltar;
  TextView txt_texto;
  TextView txt_texto_configuracao;
  BluetoothDevice btDevice;
  BluetoothSocket btSocket;
  private static final String CHANNEL_ID = "BABY NEAR NOTIFICATION CHANNEL";

  int sinal;

  private InputStream inputStream;
  private OutputStream outputStream;

  private LinearLayout baseLayout;
  private boolean connected;
  private BluetoothGatt bluetoothGatt;
  private BluetoothGattCallback mGattCallback;
  private Integer distanciaAtual = 0;
  private Integer distanciaMaxPermitida;
  private boolean reproduzirAudio;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    baseLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.activity_conectado, null, false);
    setContentView(baseLayout);
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    distanciaMaxPermitida = Integer.parseInt(settings.getString("distancia_max_permitida", "10"));
    reproduzirAudio = settings.getBoolean("gravacao_baby_near", true);

    btn_voltar = findViewById(R.id.btn_voltar);
    txt_texto = findViewById(R.id.txt_conectado);
    txt_texto_configuracao = findViewById(R.id.txt_configuracao);
    btDevice = (BluetoothDevice) getIntent().getExtras().get("device");
    btSocket = (BluetoothSocket) getIntent().getExtras().get("socket");

    txt_texto.setText(btDevice.getName() + " foi conectado!!");
    txt_texto_configuracao.setText("Distância aproximada máxima permitida: " + distanciaMaxPermitida + " metros");

    btn_voltar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        finish();
      }
    });

    createNotificationChannel();

    mGattCallback = new BluetoothGattCallback() {
      @Override
      public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            sinal = rssi;
            getDistanceFromSinal(sinal);
            txt_texto.setText(btDevice.getName() + " foi conectado!!\n " +
              "RSSI: " + rssi + "dBm\n" +
              "A criança está a aproximadamente " + distanciaAtual + " metros" );
          }
        });
      }
    };

  }

  private void getDistanceFromSinal(int sinal) {
    if ( sinal > -5 ) {
      distanciaAtual = 0;
    } else if ( sinal > -10 ) {
      distanciaAtual = 3;
    } else if ( sinal > -15 ) {
      distanciaAtual = 5;
    } else if ( sinal > -20 ) {
      distanciaAtual = 7;
    } else if ( sinal > -30 ) {
      distanciaAtual = 9;
    } else {
      distanciaAtual = 11;
    }
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = getString(R.string.channel_name);
      String description = getString(R.string.channel_description);
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setDescription(description);
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    try {
      connect(btDevice);
    } catch (Exception e) {
      e.printStackTrace();
    }
    int corVerde = getResources().getColor(R.color.verde, null);
    baseLayout.setBackgroundColor(corVerde);
    new ConnectService().execute();

  }

  private void connect(BluetoothDevice bdDevice) throws Exception {
    try {
      bluetoothGatt = bdDevice.connectGatt(this, false, mGattCallback);

      if (bluetoothGatt == null) {
        return;
      }

      UUID uuid = bdDevice.getUuids() == null ? UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") : bdDevice.getUuids()[0].getUuid();

      btSocket = bdDevice.createRfcommSocketToServiceRecord(uuid);
      btSocket.connect();

      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      if ( btSocket != null ) {
        try{
          tmpIn = btSocket.getInputStream();
          tmpOut = btSocket.getOutputStream();
        } catch (IOException e) { }
      }

      inputStream = tmpIn;
      outputStream = tmpOut;

      String configuracao = reproduzirAudio ? "N" : "R";
      outputStream.write(configuracao.getBytes());
      outputStream.flush();

      connected = true;

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private class ConnectService extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... voids) {
      byte[] buffer = new byte[256];
      int bytes;
      while (bluetoothGatt.readRemoteRssi()) {
        if ( distanciaAtual > distanciaMaxPermitida ) {
          try {
            outputStream.write("D".getBytes());
            outputStream.flush();
          } catch (IOException e) {
            break;
          }
          break;
        }
      }
      publishProgress();
      return false;
    }

    @Override
    protected void onProgressUpdate(Void... voids) {
      runOnUiThread(myRunnable);
      int corVermelha = getResources().getColor(R.color.vermelho, null);
      baseLayout.setBackgroundColor(corVermelha);
      Toast.makeText(ConectadaActivity.this, "Acabou a conexão", Toast.LENGTH_SHORT).show();
      MediaPlayer mp = MediaPlayer.create(ConectadaActivity.this, R.raw.alerta);
      mp.start();
      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
      } else {
        //deprecated in API 26
        v.vibrate(2000);
      }
      createNotificationOnFail();
      endSocket();
    }

    private Runnable myRunnable = new Runnable() {
      @Override
      public void run() {
        Toast toastTdBem = Toast.makeText(ConectadaActivity.this, "Tudo Bem!", Toast.LENGTH_SHORT);
        toastTdBem.show();
        txt_texto.setText("Conexão acabou!!!!");
      }
    };

  }

  public void endSocket() {
    try {
      btSocket.close();
    } catch (IOException e) { }
  }

  private void createNotificationOnFail() {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_notification_alert))
      .setContentTitle("ALERTA!!!!!!")
      .setContentText("Dispositivo mais distante do que o seguro!!")
      .setPriority(NotificationCompat.PRIORITY_MAX);
    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
    notificationManagerCompat.notify(new Random().nextInt(), builder.build());
  }
}
