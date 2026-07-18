package org.seniorconnect.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // YouTube is the only active button in this phase (see AGENTS.md).
        // Call, Speak, and Map remain behavior-free.
        findViewById(R.id.action_youtube).setOnClickListener(
                v -> startActivity(new Intent(this, YouTubeActivity.class)));
    }
}
