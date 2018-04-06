package practice_app_map_places.susmoy.com.practiceapp_google_map_places;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by SusMoy on 3/23/2018.
 */

public class InfoWindow implements GoogleMap.InfoWindowAdapter {

    private final View window;
    private Context context;

    public InfoWindow(Context context) {
        this.window = LayoutInflater.from(context).inflate(R.layout.custom_info, null);
        this.context = context;
    }

    public void Init_info_window(Marker marker, View view){
        String title = marker.getTitle();
        //String details = marker.getSnippet();

        Log.e("Init_info_window", title);
        //Log.e("Init_info_window", details);

        TextView textview_title = (TextView) view.findViewById(R.id.textview_title);
        TextView textview_details = (TextView) view.findViewById(R.id.textview_details);

        textview_title.setText(title);
        //textview_details.setText(details);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        Init_info_window(marker, window);
        return window;
    }

    @Override
    public View getInfoContents(Marker marker) {
        Init_info_window(marker, window);
        return window;
    }
}
