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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.kp.KPMItem;
import xin.micro.kp.moduleloader.util.ConfigUtils;
import xin.micro.kp.moduleloader.util.FileUtil;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class PatchFragment extends MyFragment {

    private RecyclerView rvModules;
    private Button btnAddModule;
    private Button btnStartRepair;
    private TextView tvInfoTitle;
    private TextView tvInfoDescription;
    private TextView tvInfoStatus;
    private ModuleAdapter adapter;
    private FileUtil fileUtil;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileUtil = new FileUtil();
        fileUtil.init(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patch, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    @Override
    public void onShow() {
        refreshView();
    }

    private void initViews(View view) {
        rvModules = view.findViewById(R.id.rv_modules);
        btnAddModule = view.findViewById(R.id.btn_add_module);
        btnStartRepair = view.findViewById(R.id.btn_start_repair);
        tvInfoTitle = view.findViewById(R.id.tv_info_title);
        tvInfoDescription = view.findViewById(R.id.tv_info_description);
        tvInfoStatus = view.findViewById(R.id.tv_info_status);
    }

    private void refreshView() {
        tvInfoTitle.setText("信息");
        if (!KernelPatch.getInstance().isNormal()) {
            tvInfoStatus.setText("无法获取状态");
        } else if (KernelPatch.getInstance().isPatched()) {
            tvInfoStatus.setText("状态: 已修补");
        } else {
            tvInfoStatus.setText("状态: 未修补");
        }

        int preAddKpmCount = KernelPatch.getInstance().refreshKpmList(requireContext());
        List<KPMItem> moduleList = KernelPatch.getInstance().getModuleList();

        if (preAddKpmCount > 0 && moduleList != null && !moduleList.isEmpty()) {
            if (adapter == null) {
                adapter = new ModuleAdapter(moduleList);
                rvModules.setLayoutManager(new LinearLayoutManager(getContext()));
                rvModules.setAdapter(adapter);
                adapter.setOnItemClickListener(position -> {
                    KPMItem item = adapter.getItem(position);
                    if (item != null) {
                        Toast.makeText(getContext(), "点击了: " + item.name, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                adapter.updateItems(moduleList);
            }
            tvInfoDescription.setText("跟随修补的kpm数量: " + preAddKpmCount + "个");
        } else {
            tvInfoDescription.setText("你还没有预添加kpm");
            if (adapter != null) {
                adapter.clearItems();
            }
        }
    }

    private void setupListeners() {
        btnAddModule.setOnClickListener(v -> {
            fileUtil.pickFile();
            fileUtil.setListener(path -> {
                if (path != null) {
                    KPMItem kpmItem = new KPMItem(path);
                    kpmItem.complete();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("即将添加：" + kpmItem.name)
                            .setMessage("描述: " + kpmItem.description)
                            .setPositiveButton("确定", (dialog, which) -> {
                                if (KernelPatch.getInstance().preAddKpm(requireContext(), new File(path))) {
                                    Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
                                    refreshView();
                                } else {
                                    Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .setCancelable(true)
                            .show();
                } else {
                    Toast.makeText(getContext(), "FilePath为Null", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnStartRepair.setOnClickListener(v -> {
            String msg = KernelPatch.getInstance().doPatchAndPackBootImg();
            if (msg != null) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("确认操作")
                        .setMessage("当前已经修补完成 是否安装(刷入)？\n\nPatch Logs: \n" + msg)
                        .setPositiveButton("确定", (dialog, which) -> {
                            KernelPatch.getInstance().flashBootSlot();
                            ConfigUtils.sp.edit().putString("recorded_boot_sha256", MagicUtil.getCurrBootSha256()).apply();
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("失败")
                        .setMessage("当前修补似乎没有成功\n\nPatch Logs: \n" + msg)
                        .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
            }
        });
    }

    public static class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

        private List<KPMItem> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onDetailClick(int position);
        }

        public ModuleAdapter(List<KPMItem> items) {
            this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        }

        public void updateItems(List<KPMItem> newItems) {
            if (newItems == null) {
                this.items = new ArrayList<>();
            } else {
                this.items = new ArrayList<>(newItems);
            }
            notifyDataSetChanged();
        }

        public void clearItems() {
            this.items.clear();
            notifyDataSetChanged();
        }

        public KPMItem getItem(int position) {
            if (position >= 0 && position < items.size()) {
                return items.get(position);
            }
            return null;
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
            KPMItem item = items.get(position);
            holder.tvTitle.setText(item.name);
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