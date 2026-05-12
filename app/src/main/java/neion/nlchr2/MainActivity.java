package neion.nlchr2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private AppAdapter adapter;
    private SharedPreferences prefs;
    private final String hiddenStr = "hidden_list";
    private final String favoriteStr = "favorites_list";
    private Set<String> hiddenSet = new HashSet<>();
    private Set<String> favorites = new HashSet<>();
    private ImageView eyeLine;
    private BiometricPrompt biometricPrompt;

    private SharedPreferences getPrefs() {
        if (prefs == null) prefs = getSharedPreferences("nl_prefs", MODE_PRIVATE);
        return prefs;
    }

    private final BiometricPrompt.AuthenticationCallback authCallback = new BiometricPrompt.AuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            eyeLine.setVisibility(View.GONE);
            loadInstalledApps();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {loadInstalledApps();}
    };

    private Set<String> readSet(String key) {
        Set<String> set = getPrefs().getStringSet(key, null);
        if (set == null || set.isEmpty()) return new HashSet<>();
        return new HashSet<>(set);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView rv = findViewById(R.id.rvApps);
        eyeLine = findViewById(R.id.eyeLine);

        adapter = new AppAdapter(this,
                pkg -> {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (intent != null) startActivity(intent);
                },
                this::popupMenu
        );
        rv.setAdapter(adapter);

        FrameLayout eyeContainer = findViewById(R.id.eyeContainer);
        eyeContainer.setOnClickListener(v -> {
            if (eyeLine.getVisibility() == View.VISIBLE) {
                if (canUseBiometric()) {
                    biometricPrompt.authenticate(new CancellationSignal(), getMainExecutor(), authCallback);
                } else eyeLine.setVisibility(View.GONE);
            } else eyeLine.setVisibility(View.VISIBLE);
            loadInstalledApps();
        });

        hiddenSet = readSet(hiddenStr);
        favorites = readSet(favoriteStr);

        eyeLine.setImageDrawable(getDrawable(R.drawable.eye_line));
        setupBiometric();
        registerPackageReceiver();

        rv.post(this::loadInstalledApps);
    }

    private void registerPackageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
    }

    private void loadInstalledApps() {
        var intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved;
        if (Build.VERSION.SDK_INT >= 33) {
            resolved = getPackageManager().queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolved = getPackageManager().queryIntentActivities(intent, 0);
        }

        Set<String> hidden = hiddenSet;
        Set<String> favs = favorites;
        boolean showAll = eyeLine.getVisibility() == View.GONE;

        var apps = new ArrayList<AppItem>();
        for (var ri : resolved) {
            if (showAll || !hidden.contains(ri.activityInfo.packageName)) apps.add(new AppItem(ri.loadLabel(getPackageManager()), ri.activityInfo.packageName, ri.loadIcon(getPackageManager())));
        }
        apps.sort((a, b) -> Boolean.compare(
                !favs.contains(a.getPkg()),
                !favs.contains(b.getPkg())
        ));
        adapter.update(apps, hidden, favs);
    }

    private void popupMenu(View view, String pkg) {
        Context context = view.getContext();
        PopupMenu popup = new PopupMenu(context, view);
        Menu menu = popup.getMenu();

        menu.add(favorites.contains(pkg) ? "Unfavorite" : "Favorite").setOnMenuItemClickListener(item -> {
            toggleSet(favorites, pkg, favoriteStr);
            return true;
        });

        menu.add(hiddenSet.contains(pkg) ? "Show" : "Hide").setOnMenuItemClickListener(item -> {
            toggleSet(hiddenSet, pkg, hiddenStr);
            return true;
        });

        menu.add("App Info").setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            context.startActivity(intent);
            return true;
        });
        popup.show();
    }

    private void toggleSet(Set<String> set, String pkg, String key) {
        if (!set.add(pkg)) set.remove(pkg);
        getPrefs().edit().putStringSet(key, set).apply();
        loadInstalledApps();
    }

    private boolean canUseBiometric() {
        return getSystemService(BiometricManager.class).canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void setupBiometric() {
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        biometricPrompt = new BiometricPrompt.Builder(this).setTitle("Verify that it's you").setAllowedAuthenticators(authenticators).build();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(packageReceiver);
    }
}