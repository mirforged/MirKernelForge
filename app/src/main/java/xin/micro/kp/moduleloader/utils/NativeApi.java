package xin.micro.kp.moduleloader.utils;

public class NativeApi {

    // Used to load the 'moduleloader' library on application startup.
    static {
        System.loadLibrary("moduleloader");
    }

    /**
     * A native method that is implemented by the 'moduleloader' native library,
     * which is packaged with this application.
     */
    public native String syscall();

}
