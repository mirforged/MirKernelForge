package xin.micro.kp.moduleloader.ui;

import android.os.Bundle;
import android.util.Log;
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

import java.io.File;
import java.util.List;

import xin.micro.kp.moduleloader.R;
import xin.micro.kp.moduleloader.kp.KPMItem;
import xin.micro.kp.moduleloader.util.FileUtil;
import xin.micro.kp.moduleloader.kp.KernelPatch;
import xin.micro.kp.moduleloader.util.MagicUtil;

public class PatchFragment extends Fragment {

    private RecyclerView rvModules;
    private Button btnAddModule;
    private Button btnStartRepair;
    private TextView tvInfoTitle;
    private TextView tvInfoDescription;
    private TextView tvInfoStatus;
    private ModuleAdapter adapter;
    private FileUtil fileUtil;//用于选择模块

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
        setupView();
        setupListeners();
        Log.d("PatchFragment", "onCreateView: ");
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

    private void setupView() {

        // 这里可以动态更新信息卡片的内容
        tvInfoTitle.setText("信息");
        if (!KernelPatch.getInstance().isNormal()) {
            tvInfoStatus.setText("无法获取状态");
        } else if (KernelPatch.getInstance().isPatched()) {
            tvInfoStatus.setText("状态: 已修补");
        } else {
            tvInfoStatus.setText("状态: 未修补");
        }
        int preAddKpmCount = KernelPatch.getInstance().refreshKpmList(requireContext());

        //显示将被修补的kpm
        if (preAddKpmCount > 0) {
            //遍历dir 获取需要修补的kpm
            List<KPMItem> moduleList = KernelPatch.getInstance().getModuleList();
            adapter = new ModuleAdapter(moduleList);

            rvModules.setLayoutManager(new LinearLayoutManager(getContext()));
            rvModules.setAdapter(adapter);
            adapter.setOnItemClickListener(position -> {
                KPMItem item = moduleList.get(position);
                Toast.makeText(getContext(), "点击了: " + item.name, Toast.LENGTH_SHORT).show();
            });
            tvInfoDescription.setText("跟随修补的kpm数量: " + preAddKpmCount + "个");
        } else {
            tvInfoDescription.setText("你还没有预添加kpm");
        }

    }

    private void setupListeners() {
        //finished
        btnAddModule.setOnClickListener(v -> {
            // 触发选择文件
            Toast.makeText(getContext(), "选择文件功能 - 请在此实现文件选择逻辑", Toast.LENGTH_SHORT).show();
            fileUtil.pickFile();
            this.fileUtil.setListener(path -> {
                if (path != null) {
                    KPMItem kpmItem = new KPMItem(path);
                    kpmItem.complete();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("即将添加：" + kpmItem.name)
                            .setMessage("描述: " + kpmItem.description)
                            .setPositiveButton("确定", (dialog, which) -> {
                                // 用户点击确定
                                if (KernelPatch.getInstance().preAddKpm(requireContext(), new File(path))) {
                                    Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .setCancelable(true) // 点击外部可取消
                            .show();
                } else {
                    Toast.makeText(getContext(), "FilePath为Null", Toast.LENGTH_SHORT).show();
                }
            });
        });


        btnStartRepair.setOnClickListener(v -> {
            String msg = KernelPatch.getInstance().doPatchAndPackBootImg(); //doPatch
            if (msg.contains("patch done")){

                new AlertDialog.Builder(requireContext())
                        .setTitle("确认操作")
                        .setMessage("当前已经修补完成 是否安装()？\n\nPatch Logs: \n" + msg)
                        .setPositiveButton("确定", (dialog, which) -> {
                            // 用户点击确定

                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            // 用户点击取消
                            dialog.dismiss();
                        })
                        .setCancelable(true) // 点击外部可取消
                        .show();
            }else{
                new AlertDialog.Builder(requireContext())
                        .setTitle("失败")
                        .setMessage("当前已经修补似乎没有成功\n\nPatch Logs: \n" + msg)
                        .setPositiveButton("确定(无操作)", (dialog, which) -> {

                            dialog.dismiss();
                        })
                        .setCancelable(true) // 点击外部可取消
                        .show();
            }
        });
    }

    // RecyclerView适配器（简化版，只处理模块卡片）
    public static class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

        private List<KPMItem> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onDetailClick(int position);
        }

        public ModuleAdapter(List<KPMItem> items) {
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