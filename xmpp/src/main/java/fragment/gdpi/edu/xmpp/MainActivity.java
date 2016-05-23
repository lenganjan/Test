package fragment.gdpi.edu.xmpp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText edUser,edPassword,edFrindlyID,edTextMsg;
    private Button btnConnection,btnSignUp,btnLogin,btnAddFrindly,btnSendMsg,btnSendImage,btnSendVideo;
    private XMPPTCPConnection connection;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){
                Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    /**
     * 初始化控件。
     */
    private void initView() {
        edUser = (EditText)findViewById(R.id.edUser);
        edPassword = (EditText)findViewById(R.id.edPassword);
        btnConnection = (Button)findViewById(R.id.btnConnection);
        btnSignUp = (Button)findViewById(R.id.btnSignUp);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        edFrindlyID = (EditText)findViewById(R.id.edFindlyID);
        edTextMsg = (EditText)findViewById(R.id.edTextMsg);
        btnAddFrindly = (Button)findViewById(R.id.btnAddFrindly);
        btnSendMsg = (Button)findViewById(R.id.btnSendMsg);
        btnSendImage = (Button)findViewById(R.id.btnSendImage);
        btnSendVideo = (Button)findViewById(R.id.btnSendVideo);


        btnConnection.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnAddFrindly.setOnClickListener(this);
        btnSendMsg.setOnClickListener(this);
        btnSendImage.setOnClickListener(this);
        btnSendVideo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnConnection:
                connectionService();
                break;
            case R.id.btnSignUp:
                sendSignUpOrLogin(1);
                break;
            case R.id.btnLogin:
                sendSignUpOrLogin(2);
                break;
            case R.id.btnAddFrindly:
                addFriendly();
                break;
            case R.id.btnSendMsg:
                sendMessage();
                break;
            case R.id.btnSendImage:
                selectIntent(1);
                break;
            case R.id.btnSendVideo:
                selectIntent(2);
                break;
        }
    }

    /**
     * 从系统的图库和视频中选择文件返回，发送给好友。
     * @param sele 参数为1，选择图片；参数为2，选择视频；
     */
    private void selectIntent(int sele){
        if(sele==1){
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,1);
        }else if(sele==2){
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,2);
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        final String frindlyID = edFrindlyID.getText()+"@desktop-nl06mpj";
        final String textMsg = edTextMsg.getText().toString();
        if(frindlyID.equals("")&&textMsg.equals("")||frindlyID.isEmpty()&&textMsg.isEmpty()){
            Toast.makeText(this,"账号ID和文本内容不能为空",Toast.LENGTH_SHORT).show();
        }else {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            Chat chat = chatManager.createChat(frindlyID);
            try {
                chat.sendMessage(textMsg);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加好友
     */
    private void addFriendly() {
        final String frindlyID = edFrindlyID.getText()+"@desktop-nl06mpj";
        if(frindlyID.equals("")||frindlyID.isEmpty()){
            Toast.makeText(this,"账号ID不能为空",Toast.LENGTH_SHORT).show();
        }else {
            Roster roster = Roster.getInstanceFor(connection);
            try {
                roster.createEntry(frindlyID,frindlyID,null);
            } catch (SmackException.NotLoggedInException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    private void connectionService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connection = getXMPICPConnection();
                try {
                    connection.connect();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        ;
    }

    /**
     * 注册和登陆调用的方法
     * @param select 参数为 1 表示注册，参数为 2 表示登陆。
     */
    private void sendSignUpOrLogin(final int select) {
        final String user = edUser.getText().toString();
        final String password = edPassword.getText().toString();
        if(user.equals("")||password.equals("")){
            Toast.makeText(this,"用户名和密码不能为空",Toast.LENGTH_SHORT).show();
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(connection.isConnected()){
                        connection.disconnect();
                    }
                    try {
                        connection.connect();
                        if(select==1) {
                            AccountManager accountManager = AccountManager.getInstance(connection);
                            if (accountManager.supportsAccountCreation()) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("email", "myname@qq.com");
                                map.put("phone", "create_android");
                                accountManager.createAccount(user, password, map);
                            }
                        }else if(select==2){
                            connection.login(user,password);
                            Presence presence = new Presence(Presence.Type.available);
                            presence.setStatus("我在线中");
                            connection.sendStanza(presence);
                            receiveMessage();
                        }
                    } catch (SmackException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * 接受好友发送来的消息。
     */
    private void receiveMessage() {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                chat.addMessageListener(new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
                        if(message.getBody()!=null){
                            Message msg = mHandler.obtainMessage();
                            msg.arg1=1;
                            msg.obj=message.getBody().toString();
                            mHandler.sendMessage(msg);                                            }
                    }
                });
            }
        });
    }

    /**
     * 初始化XMPP状态
     * @return XMPPTCPConnection的对象。
     */
    private XMPPTCPConnection getXMPICPConnection(){
        String service = "172.21.112.206";  //服务器地址  10.0.0.2  172.21.112.206
        int port = 5222;    //服务器端口
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();  //创建Builder，利用builder配置信息
        builder.setHost(service);
        builder.setPort(port);
        builder.setServiceName("");
        builder.setCompressionEnabled(false);
        builder.setDebuggerEnabled(true);
        builder.setSendPresence(true);
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        return new XMPPTCPConnection(builder.build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if((requestCode==1||requestCode==2)&&resultCode==RESULT_OK&&data!=null){
            selectFile(requestCode, data);
        }
    }

    /**
     * 选择图片或者视频发送
     * @param requestCode onActivityResult的返回码
     * @param data onActivityResult的返回数据
     */
    private void selectFile(int requestCode, Intent data) {
        final String friendID = edFrindlyID.getText()+"@desktop-nl06mpj";
        Uri selectedImage = data.getData();
        String filePathColom[]=new String[1];
        if(requestCode==1) {
            filePathColom[0] = MediaStore.Images.Media.DATA;
        }else if(requestCode==2){
            filePathColom[0] = MediaStore.Video.Media.DATA;
        }
        Cursor cursor = getContentResolver().query(selectedImage,filePathColom,null,null,null);
        cursor.moveToFirst();
        String filePath = cursor.getString(cursor.getColumnIndex(filePathColom[0]));
        cursor.close();
        FileTransferManager fileTransferManager = FileTransferManager.getInstanceFor(connection);
        OutgoingFileTransfer outgoingFileTransfer = fileTransferManager.createOutgoingFileTransfer(friendID);
        File file = new File(filePath);
        try {
            outgoingFileTransfer.sendFile(file,"发送图片");
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }
}
