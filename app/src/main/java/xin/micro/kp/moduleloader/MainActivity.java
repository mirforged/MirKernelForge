package xin.micro.kp.moduleloader;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import xin.micro.kp.moduleloader.root.RootShellUtil;
import xin.micro.kp.moduleloader.ui.HomeFragment;
import xin.micro.kp.moduleloader.ui.ModulesFragment;
import xin.micro.kp.moduleloader.ui.MyFragment;
import xin.micro.kp.moduleloader.ui.PatchFragment;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.utils.ConfigUtils;
import xin.micro.kp.moduleloader.utils.MagicUtil;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private ModulesFragment modulesFragment;
    private PatchFragment patchFragment;
    private MyFragment currentFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ConfigUtils.sp = getBaseContext().getSharedPreferences("my_config", MODE_PRIVATE);

        // 获取 Root 权限
        RootShellUtil.initRoot(getApplicationContext());
        if (!RootShellUtil.isRootAvailable()){
            new AlertDialog.Builder(this)
                    .setTitle("Root权限获取失败")
                    .setMessage("请检查Root权限是否开启 否则无法使用 :( ")
                    .setNegativeButton("退出", (dialog, which) -> {
                        finish();
                    })
                    .show();
        }
        MagicUtil.releaseFile(getApplicationContext());


        //================UILoader===================
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 处理边到边显示
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 初始化 Fragment（只创建一次）
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            modulesFragment = new ModulesFragment();
            patchFragment = new PatchFragment();

            // 首次加载，添加所有 Fragment 并隐藏
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainerView, homeFragment, "home")
                    .add(R.id.fragmentContainerView, modulesFragment, "modules")
                    .add(R.id.fragmentContainerView, patchFragment, "patch")
                    .hide(modulesFragment)
                    .hide(patchFragment)
                    .commit();

            currentFragment = homeFragment;
        } else {
            // 配置变更后恢复
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
            modulesFragment = (ModulesFragment) getSupportFragmentManager().findFragmentByTag("modules");
            patchFragment = (PatchFragment) getSupportFragmentManager().findFragmentByTag("patch");
            currentFragment = (MyFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        }

        // 设置默认选中的菜单项
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // 设置底部导航栏的点击监听
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            MyFragment targetFragment = null;

            if (itemId == R.id.navigation_home) {
                targetFragment = homeFragment;
            } else if (itemId == R.id.navigation_modules) {
                targetFragment = modulesFragment;
            }else if (itemId == R.id.navigation_patch) {
                if (!KernelPatch.getInstance().isNormal()){
                    Toast.makeText(getApplicationContext(), "请先拉取内核状态", Toast.LENGTH_SHORT).show();
                    return false;
                }
                targetFragment = patchFragment;
            }

            if (targetFragment != null && targetFragment != currentFragment) {
                // 使用 show/hide 切换，而不是 replace
                getSupportFragmentManager().beginTransaction()
                        .hide(currentFragment)
                        .show(targetFragment)
                        .commit();
                currentFragment = targetFragment;
                currentFragment.onShow(currentFragment.getView());// 在这里调用onShow方法
                return true;
            }
            return false;
        });

    }
}
