package com.test.baserefreshview;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.xycode.xylibrary.base.BaseActivity;
import com.xycode.xylibrary.utils.L;

public class New1Activity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new1);
        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postEvent("anEventName", "ABC", obj -> {
                            L.e((String) obj);
                            return "AA";
                        });
                    }
                }
        );
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    @Override
    protected AlertDialog setLoadingDialog() {
        return null;
    }
}
