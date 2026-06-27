package xin.micro.kp.moduleloader.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.util.KernelPatch;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class PatchFragment extends Fragment {

    private RecyclerView rvModules;
    private Button btnAddModule;
    private Button btnStartRepair;
    private TextView tvInfoTitle;
    private TextView tvInfoDescription;
    private TextView tvInfoStatus;
    private ModuleAdapter adapter;
    private List<ModuleItem> moduleList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patch, container, false);
        initViews(view);
        setupInfoCard();
        setupRecyclerView();
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        rvModules = view.findViewById(R.id.rv_modules);
        btnAddModule = view.findViewById(R.id.btn_add_module);
        btnStartRepair = view.findViewById(R.id.btn_start_repair);
        tvInfoTitle = view.findViewById(R.id.tv_info_title);
        tvInfoDescription = view.findViewById(R.id.tv_info_description);
        tvInfoStatus = view.findViewById(R.id.tv_info_status);
    }

    private void setupInfoCard() {
        // 这里可以动态更新信息卡片的内容
        tvInfoTitle.setText("信息");
        if(!KernelPatch.getInstance().isNormal()){
            tvInfoStatus.setText("无法获取状态");
        } else if(KernelPatch.getInstance().isPatched()){
            tvInfoStatus.setText("状态: 已修补");
        }else{
            tvInfoStatus.setText("状态: 未修补");
        }
        tvInfoDescription.setText("当前模块加载器状态");
    }

    private void setupRecyclerView() {
        moduleList = new ArrayList<>();
        // 注意：这里不再添加信息卡片，因为信息卡片已经写在布局中了

        // 添加示例模板卡片（从第二个卡片开始）
        moduleList.add(new ModuleItem(
                "示例模块 1",
                "这是使用模板创建的示例卡片，点击详情查看"
        ));

        adapter = new ModuleAdapter(moduleList);
        rvModules.setLayoutManager(new LinearLayoutManager(getContext()));
        rvModules.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            ModuleItem item = moduleList.get(position);
            Toast.makeText(getContext(), "点击了: " + item.title, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        btnAddModule.setOnClickListener(v -> {
            // 触发选择文件
            Toast.makeText(getContext(), "选择文件功能 - 请在此实现文件选择逻辑", Toast.LENGTH_SHORT).show();
            // 这里你之后会实现文件选择逻辑
        });

        btnStartRepair.setOnClickListener(v ->{
            String msg = MagicUtil.patchKernel(null);
            msg += "\nPatch Boot\n"+MagicUtil.packBootImg();
            new AlertDialog.Builder(requireContext())
                .setTitle("确认操作")
                .setMessage("当前已经修补完成 是否安装()？\n\nPatch Logs: \n"+msg)
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户点击确定

                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 用户点击取消
                    dialog.dismiss();
                })
                .setCancelable(true) // 点击外部可取消
                .show();
        });
    }

    // 数据模型类（不需要isInfoCard字段了）
    public static class ModuleItem {
        public String title;
        public String description;

        public ModuleItem(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    // RecyclerView适配器（简化版，只处理模块卡片）
    public static class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

        private List<ModuleItem> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onDetailClick(int position);
        }

        public ModuleAdapter(List<ModuleItem> items) {
            this.items = items;
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.btn_card_item, parent, false);
            return new ModuleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
            ModuleItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDescription.setText(item.description);
            holder.btnDetail.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDetailClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ModuleViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvDescription;
            Button btnDetail;

            ModuleViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_card_title);
                tvDescription = itemView.findViewById(R.id.tv_card_description);
                btnDetail = itemView.findViewById(R.id.btn_card_detail);
            }
        }
    }
}