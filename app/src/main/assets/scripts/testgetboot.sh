#!/system/bin/sh

ARCH=$(getprop ro.product.cpu.abi)

IS_INSTALL_NEXT_SLOT=$1

# Load utility functions
. ./util_functions.sh

if [ "$IS_INSTALL_NEXT_SLOT" = "true" ]; then
  get_next_slot
else
  get_current_slot
fi

find_boot_image

cp $BOOTIMAGE ./boot_ori.img

#repack unpack 关联./kernel
./kptools-android unpack ./boot_ori.img
