package xin.micro.kp.moduleloader;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import xin.micro.kp.moduleloader.root.RootShellUtil;
import xin.micro.kp.moduleloader.ui.HomeFragment;
import xin.micro.kp.moduleloader.ui.ModulesFragment;

public class MainActivity extends AppCompatActivity {

    private final HomeFragment homeFragment= new HomeFragment();
    private final ModulesFragment modulesFragment = new ModulesFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 处理边到边显示，确保内容不被系统栏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        // Main View Container
        FragmentContainerView fragmentContainerView = findViewById(R.id.fragmentContainerView);

        // 设置默认选中的菜单项
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // 设置底部导航栏的点击监听
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.navigation_home) {
                selectedFragment = homeFragment;
            } else if (itemId == R.id.navigation_modules) {
                selectedFragment = modulesFragment;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        //获取Root权限
        RootShellUtil.initRoot(getApplicationContext());

        // 加载默认的 Fragment（首页）
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, homeFragment)
                    .commit();
        }
    }

}