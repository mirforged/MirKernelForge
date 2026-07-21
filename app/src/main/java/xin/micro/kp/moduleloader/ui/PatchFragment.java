package xin.micro.kp.moduleloader.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.kp.KPMItem;
import xin.micro.kp.moduleloader.utils.AssetsUtil;
import xin.micro.kp.moduleloader.utils.ConfigUtils;
import xin.micro.kp.moduleloader.utils.FileUtil;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.utils.MagicUtil;
import xin.micro.kp.moduleloader.utils.addition.InstantKernAPI;

public class PatchFragment extends MyFragment {

    private RecyclerView rvModules;
    private Button btnRefreshStatus;
    private Button btnAddModule;
    private Button btnStartRepair;
    private TextView tvInfoTitle;
    private TextView tvInfoDescription;
    private TextView tvInfoStatus;
    private ModuleAdapter adapter;
    private FileUtil fileUtil;
    private Switch switchEnableIKA;

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

        // ======================== 判断当前是否需要刷新工作区 =========================
        String currBootSha = MagicUtil.getCurrBootSha256();
        String recordedBootSha = ConfigUtils.sp.getString("recorded_boot_sha256", "NULL");
        if (recordedBootSha.equals("NULL")) {
            ConfigUtils.sp.edit().putString("recorded_boot_sha256", MagicUtil.getCurrBootSha256()).apply();
            KernelPatch.getInstance().refreshStatusFull(requireContext());
            Toast.makeText(requireContext(), "初次使用？ 已自动拉取boot分区", Toast.LENGTH_SHORT).show();
        }
        if (!currBootSha.equals(recordedBootSha)) {
            new AlertDialog.Builder(requireContext()).setTitle("工作空间boot.img与实际boot分区不匹配").setMessage("如果你不清楚你在做什么 请参考 \n如果你在软件外曾更新过boot分区*请重新拉取* \n如果你清楚自己在做什么 可以取消\n\n选择错误将会导致boot回退*").setPositiveButton("重新拉取", (dialog, which) -> {
                        KernelPatch.getInstance().refreshStatusFull(requireContext());
                    }).setNegativeButton("取消", (dialog, which) -> {
                        dialog.dismiss();//useless
                    }).setCancelable(true) // 点击外部可取消
                    .show();
        }

        // =================== 自动获取当前patch信息 =======================
        if (!KernelPatch.getInstance().doGetPatchInformation()) {
            //还未拉取boot
            new AlertDialog.Builder(requireContext()).setTitle("还未拉取boot分区").setMessage("是否立刻拉取boot分区？").setPositiveButton("立刻拉取（推荐）", (dialog, which) -> {
                KernelPatch.getInstance().refreshStatusFull(requireContext());
            }).setNegativeButton("取消", (dialog, which) -> {
                dialog.dismiss();//useless
            }).show();
        }

        initViews(view);
        refreshView();
        setupListeners();
        return view;
    }

//    @Override
//    public void onShow() {
//        refreshView();
//    }

    private void initViews(View view) {
        rvModules = view.findViewById(R.id.rv_modules);
        btnRefreshStatus = view.findViewById(R.id.btn_refresh_status);
        btnAddModule = view.findViewById(R.id.btn_add_module);
        btnStartRepair = view.findViewById(R.id.btn_start_repair);
        tvInfoTitle = view.findViewById(R.id.tv_info_title);
        tvInfoDescription = view.findViewById(R.id.tv_info_description);
        tvInfoStatus = view.findViewById(R.id.tv_info_status);
        switchEnableIKA = view.findViewById(R.id.switch_enable_instant_kern_api);
    }

    @SuppressLint("SetTextI18n")
    private void refreshView() {
        tvInfoTitle.setText("信息[点击展开]");
        if (!KernelPatch.getInstance().isNormal()) {
            tvInfoStatus.setText("无法获取状态");
        } else if (KernelPatch.getInstance().isPatched()) {
            tvInfoStatus.setText("状态: 已修补");
        } else {
            tvInfoStatus.setText("状态: 未修补");
        }
        tvInfoTitle.setOnClickListener(v -> {
            if (tvInfoDescription.getVisibility() == View.VISIBLE) {
                tvInfoTitle.setText("信息[点击展开]");
                tvInfoDescription.setVisibility(View.GONE);
            } else {
                tvInfoTitle.setText("信息[点击收起]");
                tvInfoDescription.setVisibility(View.VISIBLE);
            }
        });

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
                        new AlertDialog.Builder(requireContext())
                                .setTitle("移除模块：" + item.name)
                                .setMessage("这个模块将被从(即将跟从修补)的模块列表中移除")
                                .setPositiveButton("确定", (dialog, which) -> {
                                    boolean success = KernelPatch.getInstance().removePreAddKpm(requireContext(), item.name+".kpm");
                                    Toast.makeText(getContext(), success ? "移除成功" : "移除失败", Toast.LENGTH_SHORT).show();
                                    refreshView();
                                })
                                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                                .setCancelable(true)
                                .show();
                    }
                });
            } else {
                adapter.updateItems(moduleList);
            }
        } else {
            if (adapter != null) {
                adapter.clearItems();
            }
        }

        KernelPatch.getInstance().doGetPatchInformation();
        MagicUtil.PatchInfo info = KernelPatch.getInstance().getPatchInformation();
        if (info!=null){
            tvInfoDescription.setText(
                    "将跟随修补的kpm数量: " + preAddKpmCount +
                            "\nbanner: " + info.banner() +
                            "\nisPatched: " + info.isPatched() +
                            "\nrecorded_boot_sha256: " + ConfigUtils.sp.getString("recorded_boot_sha256", "NULL"));
        }else{
            tvInfoDescription.setText("错误: 无法获取信息");
        }
    }

    private void setupListeners() {
        btnAddModule.setOnClickListener(v -> {
            fileUtil.pickFile();
            fileUtil.setListener(path -> {
                if (path != null) {
                    KPMItem kpmItem = new KPMItem(path);
                    kpmItem.complete();
                    new AlertDialog.Builder(requireContext()).setTitle("即将添加：" + kpmItem.name).setMessage("描述: " + kpmItem.description).setPositiveButton("确定", (dialog, which) -> {
                        if (KernelPatch.getInstance().preAddKpm(requireContext(), new File(path))) {
                            Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
                            refreshView();
                        } else {
                            Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss()).setCancelable(true).show();
                } else {
                    Toast.makeText(getContext(), "FilePath为Null", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnStartRepair.setOnClickListener(v -> {
            String msg = KernelPatch.getInstance().doPatchAndPackBootImg();
            if (msg != null) {
                new AlertDialog.Builder(requireContext()).setTitle("确认操作").setMessage("当前已经修补完成 是否安装(刷入)？\n\nPatch Logs: \n" + msg).setPositiveButton("确定", (dialog, which) -> {
                    KernelPatch.getInstance().flashBootSlot();
                    ConfigUtils.sp.edit().putString("recorded_boot_sha256", MagicUtil.getCurrBootSha256()).apply();
                }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss()).setCancelable(true).show();
            } else {
                new AlertDialog.Builder(requireContext()).setTitle("失败").setMessage("当前修补似乎没有成功\n\nPatch Logs: \n" + msg).setPositiveButton("确定", (dialog, which) -> dialog.dismiss()).setCancelable(true).show();
            }
        });

        btnRefreshStatus.setOnClickListener(v -> {
            //对比决定是否拉取的boot

            String currBootSha = MagicUtil.getCurrBootSha256();
            String recordedBootSha = ConfigUtils.sp.getString("recorded_boot_sha256", "NULL");
            if (!currBootSha.equals(recordedBootSha)) {
                new AlertDialog.Builder(requireContext()).setTitle("")
                        .setMessage("已确认工作空间boot.img与实际boot分区不匹配")
                        .setPositiveButton("立刻拉取[推荐]", (dialog, which) -> {
                            KernelPatch.getInstance().refreshStatusFull(requireContext());
                        }).setNegativeButton("取消", (dialog, which) -> {
                            dialog.dismiss();//useless
                        }).setCancelable(true) // 点击外部可取消
                        .show();
            }else {
                Toast.makeText(requireContext(),"匹配,无需拉取",Toast.LENGTH_SHORT).show();
            }
            refreshView();
        });
        switchEnableIKA.setOnClickListener(v->{
            try {
                if (switchEnableIKA.isChecked()) {
                    AssetsUtil.releaseAsset(requireContext(), "instant_kern_api.kpm");
                    File filesDir = requireContext().getFilesDir();
                    File kpm = new File(filesDir, "instant_kern_api.kpm");
                    KernelPatch.getInstance().preAddKpm(requireContext(), kpm);
                }else{
                    KernelPatch.getInstance().removePreAddKpm(requireContext(), "mirkforged_user_api.kpm");
                }
                refreshView();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.btn_card_item, parent, false);
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
                tvTitle = itemView.findViewById(R.id.card_title);
                tvDescription = itemView.findViewById(R.id.card_description);
                btnDetail = itemView.findViewById(R.id.btn_card_detail);
            }
        }
    }
}