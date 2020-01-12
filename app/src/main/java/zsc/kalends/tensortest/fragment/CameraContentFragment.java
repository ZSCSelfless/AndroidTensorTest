package zsc.kalends.tensortest.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import yalantis.com.sidemenu.interfaces.ScreenShotable;
import zsc.kalends.tensortest.MainActivity;
import zsc.kalends.tensortest.R;

public class CameraContentFragment extends Fragment implements ScreenShotable {
    public static final String TAG = "CameraContentFragment";
    private LinearLayout front_camera;
    private LinearLayout side_camera;
    private static final String Intent_key = "MESSAGE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_menu, container, false);
        front_camera = rootView.findViewById(R.id.front_camera);
        side_camera = rootView.findViewById(R.id.side_camera);
        initView();
        return rootView;
    }
    @Override
    public void takeScreenShot() {

    }

    @Override
    public Bitmap getBitmap() {
        return null;
    }

    private void initView() {
        front_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra(Intent_key, "front");
                startActivity(intent);
            }
        });

        side_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra(Intent_key, "side");
                startActivity(intent);
            }
        });
    }
}
