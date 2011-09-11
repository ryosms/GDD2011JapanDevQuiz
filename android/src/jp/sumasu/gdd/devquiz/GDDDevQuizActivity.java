package jp.sumasu.gdd.devquiz;


import com.google.android.apps.gddquiz.IQuizService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class GDDDevQuizActivity extends Activity {
	private IQuizService service = null;
	private ServiceConnection svcConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			service = IQuizService.Stub.asInterface(binder);
		}
	};
	private TextView result;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        result = (TextView)findViewById(R.id.gddResult);
        Button go = (Button)findViewById(R.id.getCode);
        go.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	try {
					result.setText(service.getCode());
				} catch (RemoteException e) {
					e.printStackTrace();
					Toast.makeText(GDDDevQuizActivity.this, "fail", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        bindService(new Intent("com.google.android.apps.gddquiz.IQuizService"), svcConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
    	unbindService(svcConn);
    	super.onDestroy();
    }
}
