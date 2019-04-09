package pi.iesb.br.babynear;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Serializable {


  private static final long serialVersionUID = -5726768658971273029L;

  ListView listViewDetected;
  Button buttonSearch, buttonOn, buttonSettings;
  ArrayAdapter<String> detectedAdapter;
  static HandleSeacrh handleSeacrh;
  BluetoothDevice bdDevice;
  ArrayList<BluetoothDevice> arrayListPairedBluetoothDevices;
  private ButtonClicked clicked;
  BluetoothAdapter bluetoothAdapter = null;
  ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
  ListItemClicked listItemClicked;

  static final Integer REQUEST_ENABLE_BT = 2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    listViewDetected = findViewById(R.id.listViewDetected);
    buttonSearch = findViewById(R.id.buttonSearch);
    buttonOn = findViewById(R.id.buttonOn);
    buttonSettings = findViewById(R.id.settings);
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    clicked = new ButtonClicked();
    handleSeacrh = new HandleSeacrh();
    arrayListPairedBluetoothDevices = new ArrayList<>();
    /*
     * the above declaration is just for getting the paired bluetooth devices;
     * this helps in the removing the bond between paired devices.
     */
    arrayListBluetoothDevices = new ArrayList<>();
    detectedAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_single_choice);
    listViewDetected.setAdapter(detectedAdapter);
    listItemClicked = new ListItemClicked();
    detectedAdapter.notifyDataSetChanged();

  }

  @Override
  protected void onStart() {
    super.onStart();
    // Quick permission check
    int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
    permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
    permissionCheck += this.checkSelfPermission("Manifest.permission.BLUETOOTH_PRIVILEGED");
    permissionCheck += this.checkSelfPermission("Manifest.permission.BLUETOOTH_ADMIN");
    if (permissionCheck != 0) {

      this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_PRIVILEGED}, 1001); //Any number
    }

    if (!bluetoothAdapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    buttonOn.setOnClickListener(clicked);
    buttonSearch.setOnClickListener(clicked);
    buttonSettings.setOnClickListener(clicked);
    listViewDetected.setOnItemClickListener(listItemClicked);
  }

  // Lista de dispositivos achados
  class ListItemClicked implements AdapterView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      bdDevice = arrayListBluetoothDevices.get(position);
      Log.i("Log", "The dvice : " + bdDevice.toString());
      /*
       * here below we can do pairing without calling the callthread(), we can directly call the
       * connect(). but for the safer side we must usethe threading object.
       */
      //callThread();
      connect(bdDevice);
      try {

        //Chama a atividade nova
        chamaAtividadeConectada(bdDevice);

      } catch (Exception e) {
        e.printStackTrace();
      }//connect(bdDevice);
      Log.i("Log", "The bond is created: ");
    }
  }

  private BluetoothSocket connect(BluetoothDevice bdDevice) {
    BluetoothSocket socket = null;
    try {
      socket = bdDevice.createInsecureRfcommSocketToServiceRecord(UUID.randomUUID());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return socket;
  }

  public boolean removeBond(BluetoothDevice btDevice)
    throws Exception {
    Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
    Method removeBondMethod = btClass.getMethod("removeBond");
    Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
    return returnValue.booleanValue();
  }


  public boolean createBond(BluetoothDevice btDevice)
    throws Exception {
    Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
    Method createBondMethod = class1.getMethod("createBond");
    Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
    return returnValue.booleanValue();
  }


  class ButtonClicked implements View.OnClickListener {
    @Override
    public void onClick(View view) {
      switch (view.getId()) {
        case R.id.buttonOn:
          onBluetooth();
          break;
        case R.id.buttonSearch:
          arrayListBluetoothDevices.clear();
          detectedAdapter.notifyDataSetChanged();
          startSearching();
          break;
        case R.id.settings:
          settingsActivity();
          break;
        default:
          break;
      }
    }
  }

  private void settingsActivity() {
    bluetoothAdapter.cancelDiscovery();
    Intent configuracao = new Intent(this, SettingsActivity.class);
    configuracao.putExtra(SettingsActivity.EXTRA_NO_HEADERS, true);
    configuracao.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
    startActivity(configuracao);
  }

  private BroadcastReceiver myReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Message msg = Message.obtain();
      String action = intent.getAction();
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        try {
          //device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
          //device.getClass().getMethod("cancelPairingUserInput", boolean.class).invoke(device);
        } catch (Exception e) {
          Log.i("Log", "Inside the exception: ");
          e.printStackTrace();
        }

        String nome = device.getName() == null || device.getName().equals("null") ? "Dispositivo sem nome" : device.getName();
        if (arrayListBluetoothDevices.size() < 1)  {
          detectedAdapter.add(nome + "\n" + device.getAddress());
          arrayListBluetoothDevices.add(device);
          detectedAdapter.notifyDataSetChanged();
        } else {
          boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
          for (int i = 0; i < arrayListBluetoothDevices.size(); i++) {
            if (device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress())) {
              flag = false;
            }
          }
          if (flag) {
            detectedAdapter.add(nome + "\n" + device.getAddress());
            arrayListBluetoothDevices.add(device);
            detectedAdapter.notifyDataSetChanged();
          }
        }
      }
    }
  };

  private void startSearching() {
    Log.i("Log", "in the start searching method");
    IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    MainActivity.this.registerReceiver(myReceiver, intentFilter);
    bluetoothAdapter.startDiscovery();
  }

  private void onBluetooth() {
    if (!bluetoothAdapter.isEnabled()) {
      bluetoothAdapter.enable();
      Log.i("Log", "Bluetooth is Enabled");
    }
  }

  private void offBluetooth() {
    if (bluetoothAdapter.isEnabled()) {
      bluetoothAdapter.disable();
    }
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(myReceiver);
    super.onDestroy();
  }

  class HandleSeacrh extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 111:

          break;

        default:
          break;
      }
    }
  }

  private void chamaAtividadeConectada(BluetoothDevice device) {
    Intent conectado = new Intent(MainActivity.this, ConectadaActivity.class);
    conectado.putExtra("device", device);
    startActivity(conectado);
    bluetoothAdapter.cancelDiscovery();
    unregisterReceiver(myReceiver);
  }
}
