package practice_app_map_places.susmoy.com.practiceapp_google_map_places;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    Button b_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_map = (Button) findViewById(R.id.button_map);

        if(IsServiceOk()){
            b_map.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent_map_activity = new Intent(MainActivity.this, MapActivity.class);
                    startActivity(intent_map_activity);
                }
            });
        }
    }

    public void Map(View v){
        startActivity(new Intent(MainActivity.this, MapActivity.class));
    }

    public boolean IsServiceOk(){
        int confirmation_connection = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(confirmation_connection == ConnectionResult.SUCCESS){
            Log.e("IsServiceOk","Service connection OKAY");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(confirmation_connection)){
            Log.e("IsServiceOk","Service connection DENIED");
        }
        else{
            Log.e("IsServiceOk","Found Undefined Problem !! ");
        }
        return false;
    }
}
