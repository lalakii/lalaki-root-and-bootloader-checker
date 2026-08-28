package c;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cn.lalaki.rc.R;

/***
 * Created on 2026-08-10
 *
 * @author lalaki
 * @since lalaki root checker
 */
public class Rc extends android.app.Activity {
    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("resource")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Environment.isExternalStorageManager()) {
            DialogInterface.OnClickListener dialogClickListener = (_, i) -> {
                if (i == AlertDialog.BUTTON_POSITIVE) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    } catch (Exception _) {

                    } finally {
                        finish();
                    }
                }
            };
            new AlertDialog.Builder(this).setTitle(R.string.tip).setMessage(R.string.msg).setPositiveButton(R.string.ok, dialogClickListener).setNegativeButton(R.string.cancel, dialogClickListener).setCancelable(false).setOnDismissListener(_ -> finish()).show();
            return;
        }
        TextView tv = new TextView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        Button btn = new Button(this);
        btn.setOnClickListener(_ -> {
            ScrollView imgLayout = new ScrollView(this);
            ImageView egView = new ImageView(this);
            egView.setImageResource(R.drawable.eg);
            imgLayout.addView(egView);
            new AlertDialog.Builder(this).setTitle(R.string.example).setView(imgLayout).setNegativeButton(R.string.done, null).show();
        });
        btn.setText(R.string.example);
        layout.addView(btn);
        layout.addView(tv);
        tv.setText(R.string.result);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        RelativeLayout mainLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mainLayout.addView(scrollView, layoutParams);
        mainLayout.setFitsSystemWindows(true);
        setContentView(mainLayout);
        ExecutorService service = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(() -> {
            try {
                if (latch.await(15000L, TimeUnit.MILLISECONDS)) {
                    invoke(tv, ".");
                }
            } catch (InterruptedException _) {
            } finally {
                service.shutdown();
                service.close();
            }
        });
        service.execute(() -> {
            final ProcessBuilder builder = new ProcessBuilder();
            final ArrayList<String[]> cmdList = new ArrayList<>();
            // 检测 build.prop 有关Bootloader锁的参数
            String[] propKeys = {"ro.bootloader", "ro.boot.flash.locked", "ro.boot.veritymode", "ro.boot.verifiedbootstate", "ro.boot.vbmeta.device_state", "sys.oem_unlock_allowed"};
            for (String propKey : propKeys) {
                cmdList.add(new String[]{"/system/bin/sh", "-c", String.format("echo \" %s = $(getprop %s)\"", propKey, propKey)});
            }
            // 检查下列二进制文件是否存在
            String[] binArr = {"su", "magisk"};
            for (String binName : binArr) {
                cmdList.add(new String[]{"/system/bin/which", binName});
                cmdList.add(new String[]{"/system/bin/type", binName});
                cmdList.add(new String[]{binName});
                cmdList.add(new String[]{binName, "--help"});
                cmdList.add(new String[]{"/system/bin/sh", "-c", String.format("%s --help", binName)});
            }
            cmdList.add(new String[]{"/system/bin/sh", "-c", "mount | grep -i -E \"ksu|module\""});
            cmdList.add(new String[]{"/system/bin/sh", "-c", "cat /proc/self/mounts | grep -i -E \"ksu|module\""});
            cmdList.add(new String[]{"/system/bin/sh", "-c", "cat /proc/self/mountinfo | grep -i \"ksu|module\""});
            cmdList.add(new String[]{"/system/bin/sh", "-c", "cat /proc/self/mountstats | grep -i \"ksu|module\""});
            // 检测下列路径是否存在 magisk 相关内容, 脚本或日志文件
            String[] dirArr = {Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "/system/addon.d", "/data/local/tmp"};
            for (String dir : dirArr) {
                final String checkCmd = String.format("%s | grep -i magisk", dir);
                cmdList.add(new String[]{"/system/bin/find", checkCmd});
                cmdList.add(new String[]{"/system/bin/ls", checkCmd});
                File dirPath = new File(dir);
                if (dirPath.isDirectory()) {
                    File[] dirs = dirPath.listFiles();
                    if (dirs != null) {
                        for (File child : dirs) {
                            String childPath = child.getAbsolutePath();
                            if (childPath.toLowerCase().contains("magisk")) {
                                invoke(tv, childPath);
                            }
                        }
                    }
                }
            }
            for (String[] cmd : cmdList) {
                Process proc = null;
                int exitCode = -1;
                try {
                    proc = builder.command(cmd).start();
                    proc.waitFor();
                    exitCode = proc.exitValue();
                } catch (IOException | InterruptedException _) {
                }
                if (proc != null && exitCode == 0) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        String line = "";
                        while (line != null) {
                            line = reader.readLine();
                            String finalLine = line;
                            if (finalLine != null && !finalLine.isEmpty()) {
                                invoke(tv, finalLine);
                            }
                        }
                        reader.close();
                    } catch (IOException _) {

                    }
                }
            }
            String[] apps = {"me.bmax.apatch", "com.topjohnwu.magisk", "com.tsng.hidemyapplist"};
            for (String it : apps) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(it); // 检测Magisk App
                if (launchIntent != null) {
                    invoke(tv, it + " - App Exists");
                }
            }
            List<ApplicationInfo> appList;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                appList = getPackageManager().getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS));
            } else {
                appList = getPackageManager().getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.MATCH_DISABLED_COMPONENTS);
            }
            for (ApplicationInfo it : appList) {
                String pkgName = String.format("%s", it.packageName);
                String lowerPkgName = pkgName.toLowerCase();
                if (lowerPkgName.contains("magisk") || lowerPkgName.contains("kernelsu")) {
                    invoke(tv, pkgName);
                }
            }
            latch.countDown();
        });
        scrollView.setOnTouchListener((_, _) -> {
            try {
                ActionBar bar = getActionBar();
                if (bar != null) {
                    bar.hide();
                }
            } catch (Exception _) {
            }
            return false;
        });
    }

    private void invoke(TextView view, String log) {
        view.post(() -> view.append(String.format("%s\n", log)));
    }
}