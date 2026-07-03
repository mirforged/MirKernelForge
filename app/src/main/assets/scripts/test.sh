#!/system/bin/sh

echo "Script Start.."
echo "Version: 3"
echo userId: $(id)
mkdir -p /data/adb/mirkforged
cd "/data/adb/mirkforged"
echo curr dir: $(pwd)

./getboot.sh #会写为ori_boot.img kernel
cp ./boot_ori.img boot.img

#echo "=== infomations ==="
#./kptools-android --image kernel --dump #symbol
#./kptools-android --flag  #config
#echo "=== infomations ==="

echo "===     List    ==="
./kptools-android --list
echo "===     END     ==="

echo "Script End"