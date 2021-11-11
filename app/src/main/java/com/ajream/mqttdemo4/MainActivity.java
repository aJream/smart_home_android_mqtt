package com.ajream.mqttdemo4;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    //定义成员
    private Button connectbtn;
    private Button exitbtn;

    private TextView temptv;
    private TextView humitv;
    private TextView lighttv;
    private TextView ledtv;
    private TextView beeptv;
    private TextView bar1tv;
    private TextView bar2tv;
    private TextView showmsgtv;

    private SeekBar tempbar;
    private SeekBar lightbar;

    //MQTT客户端配置所需信息
    private String host = "tcp://HOOC1TIEMK.iotcloud.tencentdevices.com:1883";  //mqtt服务器（腾讯云）地址、端口
    private String userName = "HOOC1TIEMKAndroidClient;12010126;P4LLD;1672218218";
    private String passWord = "90df9dc57ca3f5befb0c9271a0e3daa4aa2d0609e0441e8086b45775872be25a;hmacsha256";
    private String mqtt_id = "HOOC1TIEMKAndroidClient-android";
    private String mqtt_sub_topic = "HOOC1TIEMK/AndroidClient/data";
    private String mqtt_pub_topic = "HOOC1TIEMK/AndroidClient/data";

    private ScheduledExecutorService scheduler;
    private MqttClient mqttClient;
    private MqttConnectOptions options;
    private Handler handler;

    private String msgToPublish = "";  //要发布的消息
    private String alarmTempMsg = "";
    private String minLightMsg = "";
    JSONObject msgGet;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //关联控件
        connectbtn = findViewById(R.id.connectbtn);
        exitbtn = findViewById(R.id.exitbtn);
        temptv = findViewById(R.id.temp);
        humitv = findViewById(R.id.humi);
        lighttv = findViewById(R.id.light);
        ledtv = findViewById(R.id.ledsta2);
        beeptv = findViewById(R.id.beepsta2);
        bar1tv = findViewById(R.id.seekbarval1);
        bar2tv = findViewById(R.id.seekbarval2);
        showmsgtv = findViewById(R.id.msgTxt);

        tempbar = findViewById(R.id.seekBar1);
        lightbar = findViewById(R.id.seekBar2);

        alarmTempMsg = String.valueOf(tempbar.getProgress());
        minLightMsg = String.valueOf(lightbar.getProgress());

        /*点击按钮连接*/
        connectbtn.setOnClickListener(view -> {
            Mqtt_init();
            startReconnect();
        });

        exitbtn.setOnClickListener(view -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        });


        tempbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Toast.makeText(MainActivity.this, "设置报警温度为: " + progress, Toast.LENGTH_LONG).show();
                bar1tv.setText(check(progress));
                alarmTempMsg = check(progress);
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                msgToPublish = alarmTempMsg + "," + minLightMsg;
                msgToPublish = "{\"atemp\":\"" + alarmTempMsg + "\",\"mlight\":\"" + minLightMsg+"\"}";
                publishMsg(mqtt_pub_topic, msgToPublish);

            }
        });

        lightbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Toast.makeText(MainActivity.this, "设置最低光照为: " + i, Toast.LENGTH_LONG).show();
                bar2tv.setText(check(i));
                minLightMsg = check(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                msgToPublish = "{\"atemp\":\"" + alarmTempMsg + "\",\"mlight\":\"" + minLightMsg+"\"}";
                publishMsg(mqtt_pub_topic, msgToPublish);
            }
        });

        handler = new Handler() {
            @SuppressLint("SetTextI18n")

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传
                        break;
                    case 3: //MQTT 收到消息回传 UTF8Buffer msg=newUTF8Buffer(object.toString());
//                        Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                        showmsgtv.setText(msg.obj.toString());
                        JSONObject msgGet = null;
                        try {
                            msgGet = new JSONObject(msg.obj.toString());
                            temptv.setText(msgGet.get("temp").toString());
                            humitv.setText(msgGet.get("humi").toString());
                            lighttv.setText(msgGet.get("light").toString());
                            if(Integer.parseInt(msgGet.get("ledsta").toString())==0) ledtv.setText("关");
                            else ledtv.setText("开");
                            if(msgGet.get("beepsta").toString().charAt(0)=='0') beeptv.setText("关");
                            else beeptv.setText("开");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case 30:  //连接失败
                        Toast.makeText(MainActivity.this,"连接失败" ,Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   //连接成功
                        Toast.makeText(MainActivity.this,"连接成功" ,Toast.LENGTH_SHORT).show();
                        try {
                            mqttClient.subscribe(mqtt_sub_topic,1);
                        }
                        catch (MqttException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private String check(int progress) {
        int curValue = 100 * progress/Math.abs(100);
        return String.valueOf(curValue);
    }

    private void publishMsg(String topic, String message2) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void Mqtt_init() {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            mqttClient = new MqttClient(host, mqtt_id, new MemoryPersistence());

            //MQTT的连接设置
            options = new MqttConnectOptions();

            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);

            options.setUserName(userName); //设置连接的用户名

            options.setPassword(passWord.toCharArray()); //设置连接的密码

            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);

            //设置回调函数
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
//                    System.out.println("connectionLost----------");
//                    startReconnect();
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("getMsg: ");
                    Message msg = new Message();
                    msg.what = 3;   //收到消息标志位
                    msg.obj = message.toString();
                    handler.sendMessage(msg);    // hander 回传
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(mqttClient.isConnected())) {
                        mqttClient.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!mqttClient.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }


}