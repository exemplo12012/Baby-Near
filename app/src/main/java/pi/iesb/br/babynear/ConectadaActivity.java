package pi.iesb.br.babynear;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.UUID;

/**
 * Created by henrique on 14/11/2017.
 */

public class ConectadaActivity extends AppCompatActivity implements Serializable {

    private static final long serialVersionUID = 5890514926067136397L;
    Button btn_voltar;
    TextView txt_texto;
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;

    private InputStream inputStream;
    private OutputStream outputStream;

    private LinearLayout baseLayout;
    private boolean connected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.activity_conectado,null,false);
        setContentView(baseLayout);
        btn_voltar = (Button) findViewById(R.id.btn_voltar);
        txt_texto = (TextView) findViewById(R.id.txt_conectado);
        btDevice = (BluetoothDevice) getIntent().getExtras().get("device");
        btSocket = (BluetoothSocket) getIntent().getExtras().get("socket");

        txt_texto.setText(btDevice.getName() + " foi conectado!!" );

        btn_voltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
        try {
            connect(btDevice);
        } catch ( Exception e ){

        }
        int corVerde = getResources().getColor(R.color.verde, null);
        baseLayout.setBackgroundColor(corVerde);
        new ConnectService().execute();

    }

    private void connect(BluetoothDevice bdDevice) throws Exception{

        UUID uuid = bdDevice.getUuids() == null ? UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") : bdDevice.getUuids()[0].getUuid() ;

        btSocket = bdDevice.createRfcommSocketToServiceRecord(uuid);
        btSocket.connect();

        inputStream = btSocket.getInputStream();
        outputStream = btSocket.getOutputStream();
        connected = true;

    };

    public byte[] receive() {
        try {

            byte[] buffer = new byte[256];
            btSocket.getInputStream();
            for(int i=0;i<buffer.length;i++)
                buffer[i]='\0';
            return buffer;
        } catch ( Exception e ) {
            connected = true;
            return null;
        }
    }

    private class ConnectService extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            byte[] buffer = new byte[256];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer); //read bytes from input buffer
                } catch (IOException e) {
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

            return;
        }
        private Runnable myRunnable = new Runnable (){
            @Override
            public void run() {
                Toast.makeText(ConectadaActivity.this, "Tudo Bem!", Toast.LENGTH_SHORT);
                txt_texto.setText("Conexão acabou!!!!");
            }
        };

    }
}
